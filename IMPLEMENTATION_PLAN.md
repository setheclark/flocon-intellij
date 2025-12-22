# Flocon IntelliJ Plugin Implementation Plan

## Overview

Build an IntelliJ Platform plugin that displays network traffic from Android apps with Flocon SDK integrated. The plugin acts as a WebSocket server (port 9023), receives network data from devices, and displays it in a UI similar to Android Studio's Network Inspector.

**Key Decisions:**
- Target: All IntelliJ-based IDEs
- Scope: Network traffic inspection only (initially)
- Connection: Plugin runs WebSocket server, device connects to it
- Build: Gradle with Kotlin DSL
- **Code Reuse Strategy: Git submodule + Gradle composite build to directly use FloconDesktop modules**

---

## Code Reuse Strategy

### What We Reuse from Flocon (NO reimplementation needed)

| Flocon Module | Lines | Reusable | What We Get |
|---------------|-------|----------|-------------|
| `FloconDesktop/domain` | ~10,000 | 100% | Use cases, repository interfaces, business logic |
| `FloconDesktop/data/core` | ~3,000 | 95% | Data source interfaces |
| `FloconDesktop/data/remote` | ~4,000 | 90% | **WebSocket server**, message mappers, protocol handling |

### Key Files We Reuse Directly

1. **WebSocket Server** (production-ready, 267 lines):
   - `FloconDesktop/data/remote/src/desktopMain/kotlin/com/flocon/data/remote/server/ServerJvm.kt`

2. **Protocol & Message Handling**:
   - `FloconDesktop/domain/src/commonMain/kotlin/io/github/openflocon/domain/Protocol.kt`
   - `FloconDesktop/data/remote/src/commonMain/kotlin/com/flocon/data/remote/messages/`

3. **Domain Use Cases**:
   - All network-related use cases in `domain/src/commonMain/kotlin/io/github/openflocon/domain/network/`

### What We Build (Plugin-specific only)

- **IntelliJ UI** (Swing components) - cannot reuse Compose
- **IntelliJ Services** - lifecycle management
- **Repository adapters** - implement Flocon interfaces for IntelliJ context

---

## Project Structure

```
flocon-intellij/
├── flocon-upstream/                    # Git submodule → github.com/openflocon/Flocon
├── plugin/
│   ├── src/main/
│   │   ├── kotlin/io/github/openflocon/intellij/
│   │   │   ├── services/
│   │   │   │   ├── FloconProjectService.kt     # Manages server lifecycle
│   │   │   │   └── FloconApplicationService.kt
│   │   │   ├── adapters/
│   │   │   │   └── IntelliJNetworkRepository.kt # Implements domain interface
│   │   │   └── ui/
│   │   │       ├── FloconToolWindowFactory.kt
│   │   │       ├── NetworkInspectorPanel.kt
│   │   │       ├── timeline/TimelinePanel.kt
│   │   │       ├── list/NetworkCallListPanel.kt
│   │   │       ├── detail/DetailPanel.kt
│   │   │       └── filter/FilterPanel.kt
│   │   └── resources/
│   │       ├── META-INF/plugin.xml
│   │       └── icons/flocon.svg
│   └── build.gradle.kts
├── settings.gradle.kts                 # Composite build includes FloconDesktop
└── gradle.properties
```

---

## Gradle Configuration

### settings.gradle.kts (Composite Build)
```kotlin
rootProject.name = "flocon-intellij"

// Include Flocon as composite build
includeBuild("flocon-upstream/FloconDesktop") {
    dependencySubstitution {
        substitute(module("io.github.openflocon:domain"))
            .using(project(":domain"))
        substitute(module("io.github.openflocon:data-core"))
            .using(project(":data:core"))
        substitute(module("io.github.openflocon:data-remote"))
            .using(project(":data:remote"))
    }
}

include(":plugin")
```

### plugin/build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
    }

    // Flocon modules (via composite build - no reimplementation!)
    implementation("io.github.openflocon:domain")
    implementation("io.github.openflocon:data-core")
    implementation("io.github.openflocon:data-remote")

    // These are already transitive from Flocon, but explicit for clarity
    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("io.ktor:ktor-server-websockets-jvm:3.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("io.insert-koin:koin-core:4.1.0")
}
```

---

## Architecture

### Data Flow (Reusing Flocon Components)
```
Device App (with Flocon SDK)
    ↓ WebSocket (port 9023)
ServerJvm.kt [REUSED from FloconDesktop/data/remote]
    ↓ Flow<FloconIncomingMessageDataModel>
MessageRemoteDataSourceImpl [REUSED from FloconDesktop/data/remote]
    ↓ Parsed domain models
NetworkRepository interface [REUSED from FloconDesktop/domain]
    ↓ implemented by
IntelliJNetworkRepository [NEW - thin adapter]
    ↓ StateFlow<List<NetworkCall>>
UI Components (Swing) [NEW - IntelliJ-specific]
```

### What Flocon Already Provides
- `Protocol.kt` - All message types and routing constants
- `ServerJvm.kt` - Complete Ktor WebSocket server implementation
- `FloconIncomingMapper.kt` - JSON → domain model parsing
- `NetworkRepository` interface - data access contract
- `GetNetworkCallsUseCase` - business logic for network data
- `StartAdbForwardUseCase` - ADB reverse TCP setup (runs every 1.5s)
- `ExecuteAdbCommandUseCase` - Generic ADB command execution
- Multi-device session management via `(deviceId, packageName, appInstance)` keys

---

## ADB & Multi-Device Support (Already in Flocon)

### ADB Reverse TCP Forwarding
Flocon's `StartAdbForwardUseCase` handles this automatically:
```kotlin
// Runs: adb reverse tcp:9023 tcp:9023
// Also: adb reverse tcp:9024 tcp:9024 (for file uploads)
// Executed every 1.5 seconds to maintain forwarding
```

**Files we reuse:**
- `domain/.../settings/usecase/StartAdbForwardUseCase.kt`
- `domain/.../adb/usecase/ExecuteAdbCommandUseCase.kt`
- `composeApp/.../common/AdbExecutor.desktop.kt` - Platform-specific execution

### Multi-Device Session Management
The server tracks sessions using a composite key:
```kotlin
data class FloconDeviceIdAndPackageNameDataModel(
    val deviceId: DeviceId,         // Hardware serial or emulator ID
    val packageName: String,        // e.g., "com.example.myapp"
    val appInstance: Long,          // App launch timestamp (session ID)
)
```

**Supports:**
- Multiple physical devices + emulators simultaneously
- Multiple apps with Flocon SDK on same device
- Multiple instances of same app (identified by launch timestamp)

### ADB Path Discovery
Flocon finds ADB automatically:
1. Check if `adb` is in system PATH
2. Search common SDK locations:
   - macOS: `~/Library/Android/sdk/platform-tools/adb`
   - Windows: `%LOCALAPPDATA%\Android\sdk\platform-tools\adb`
   - Linux: `~/Android/sdk/platform-tools/adb`

**IntelliJ Plugin Advantage:** Can also query Android Studio's SDK path from IDE settings

### Device Discovery Flow
1. Device connects via WebSocket to `localhost:9023`
2. Device sends registration with `deviceId`, `packageName`, `appInstance`
3. Server maps session to composite key
4. Desktop sends ADB broadcast to get device serial for ADB commands:
   ```
   adb shell am broadcast -a "io.github.openflocon.flocon.DETECT" ...
   ```
5. Device reports serial back via WebSocket

### UI Considerations for Multi-Device
The plugin UI should allow users to:
1. **See all connected devices/apps** in a dropdown or sidebar
2. **Filter traffic by device/app** or view all combined
3. **Show connection status** per device (connected/disconnected)
4. **Color-code or tag** requests by source device/app

This mirrors how Android Studio's Logcat handles multiple devices.

---

## UI Design (Network Inspector Style)

```
┌─────────────────────────────────────────────────────────────────┐
│ [Clear] [Export] [Copy cURL]                      [Search...] ▼ │ ← Toolbar
├─────────────────────────────────────────────────────────────────┤
│ ████░░░░████████░░░░░░░████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │ ← Timeline
│ 0s                    5s                    10s                 │
├─────────────────────────────────────────────────────────────────┤
│ Status │ Method │ URL                      │ Duration │ Size   │
├────────┼────────┼──────────────────────────┼──────────┼────────┤
│  200   │  GET   │ /api/users               │   120ms  │  2.4KB │ ← List
│  201   │  POST  │ /api/orders              │   340ms  │  1.1KB │
│  ...   │  ...   │ ...                      │   ...    │  ...   │
├─────────────────────────────────────────────────────────────────┤
│ [Headers] [Request] [Response] [Timing]                         │ ← Detail
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Content-Type: application/json                              │ │
│ │ Authorization: Bearer ***                                   │ │
│ │ ...                                                         │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Phases

### Phase 1: Project Setup & Integration
1. **Repository Setup**
   - Initialize Git repository
   - Add Flocon as Git submodule: `git submodule add https://github.com/openflocon/Flocon.git flocon-upstream`
   - Configure Gradle composite build in `settings.gradle.kts`
   - Verify FloconDesktop modules compile with plugin

2. **Plugin Skeleton**
   - Create `plugin.xml` with tool window registration
   - Create `FloconProjectService` to manage server lifecycle
   - Wire up Flocon's `ServerJvm` to start on project open

3. **Basic Integration Test**
   - Start server, connect test device/emulator with Flocon SDK
   - Verify messages flow through Flocon's existing pipeline

### Phase 2: UI Implementation
1. **Tool Window & Main Panel**
   - `FloconToolWindowFactory` - register tool window
   - `NetworkInspectorPanel` - main container with split panes
   - `NetworkCallListPanel` - JBTable displaying Flocon's domain models

2. **Detail Panel**
   - `HeadersPanel` - formatted headers from Flocon models
   - `BodyPanel` - JSON formatting
   - `TimingPanel` - duration from Flocon's timing data

3. **Toolbar & Actions**
   - Clear, Export, Copy as cURL (can reuse Flocon's `GenerateCurlCommandUseCase`)

### Phase 3: Enhanced Features
1. **Timeline Visualization**
   - Custom painted panel showing request timing
   - Selection and filtering

2. **Filtering & Search**
   - URL search
   - Method/status filters
   - Leverage Flocon's existing filter models if available

3. **Server Controls**
   - Start/stop, port config, connection status

---

## Files to Create (Plugin-Specific Only)

| File | Purpose | Lines Est. |
|------|---------|------------|
| `settings.gradle.kts` | Composite build config | ~20 |
| `plugin/build.gradle.kts` | Plugin dependencies | ~40 |
| `plugin.xml` | Plugin manifest | ~30 |
| `FloconProjectService.kt` | Server lifecycle | ~50 |
| `IntelliJNetworkRepository.kt` | Adapter for Flocon interface | ~100 |
| `FloconToolWindowFactory.kt` | Tool window creation | ~30 |
| `NetworkInspectorPanel.kt` | Main UI container | ~150 |
| `NetworkCallListPanel.kt` | Request table | ~150 |
| `DetailPanel.kt` | Tabbed detail view | ~200 |
| `TimelinePanel.kt` | Timeline visualization | ~200 |
| **Total NEW code** | | **~970 lines** |

Compared to reimplementing (~17,000 lines from Flocon), this is **94% code reuse**.

---

## Flocon Files We Directly Use (Reference)

| File | What It Provides |
|------|------------------|
| `FloconDesktop/data/remote/src/desktopMain/.../ServerJvm.kt` | WebSocket server |
| `FloconDesktop/domain/.../Protocol.kt` | Message routing constants |
| `FloconDesktop/data/remote/.../FloconIncomingMapper.kt` | Message parsing |
| `FloconDesktop/domain/.../network/` | Network domain models & use cases |
| `FloconDesktop/data/core/.../NetworkRepository.kt` | Repository interface |

---

## Considerations

- **Composite build compatibility**: May need minor adjustments to Flocon's Gradle if KMP targets conflict
- **Koin DI**: Flocon uses Koin; adapt to IntelliJ's service locator or use Koin directly
- **Port conflicts**: Flocon's server already handles this; expose configuration
- **Upstream updates**: `git submodule update --remote` to sync with Flocon releases
- **Testing**: Can reuse Flocon's test utilities for protocol testing

---

## Benefits of This Approach

1. **Easy sync with Flocon updates**: Just update submodule
2. **Battle-tested server code**: Flocon's WebSocket server is production-ready
3. **Minimal maintenance**: Only UI code is plugin-specific
4. **Consistent behavior**: Same protocol handling as Flocon Desktop app
5. **Future features**: When Flocon adds features, plugin can adopt with minimal work
