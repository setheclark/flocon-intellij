# Flocon Network Inspector - IntelliJ Plugin

> Advanced network debugging for Android apps, integrated directly into IntelliJ IDEA

<!-- TODO: Add screenshot here -->

## What is Flocon Network Inspector?

Flocon Network Inspector is an IntelliJ IDEA plugin that captures and visualizes network traffic from Android apps using the [Flocon SDK](https://github.com/openflocon/Flocon). It brings powerful network debugging capabilities directly into your IDE, eliminating the need to switch between tools.

### Key Features

- **Real-time Network Capture** - Watch HTTP/HTTPS, GraphQL, and gRPC requests as they happen
- **Full Request/Response Details** - View headers, bodies, status codes, and timing
- **Multiple Device Support** - Monitor several connected devices simultaneously
- **Advanced Filtering** - Search by method, URL, status code, duration, and more
- **MCP Server** - Expose network data to AI agents for intelligent debugging assistance

## Why Use the IntelliJ Plugin?

While Flocon also offers a [standalone desktop application](https://github.com/openflocon/Flocon), the IntelliJ plugin provides:

- **Seamless IDE Integration** - No need to switch windows
- **Quick Access** - Tool window available via View → Tool Windows → Network Inspector
- **Workspace Persistence** - Settings and state saved per project
- **Developer-Friendly** - Built for developers who live in their IDE

> **⚠️ Important:** Do not run the Flocon desktop app and IntelliJ plugin simultaneously. Both use the same ports (8084 for WebSocket, 8085 for HTTP) and will conflict with each other. Choose one or the other for your debugging session.

## Installation

### Prerequisites

- IntelliJ IDEA 2024.1 or later (Community or Ultimate)
- Android Studio (optional, for device connectivity)
- Android SDK with ADB installed
- Android app with Flocon SDK integrated

### Install the Plugin

**Option 1: Download from GitHub Releases (Recommended)**

1. Go to the [latest release](https://github.com/setheclark/flocon-intellij/releases/latest)
2. Download the `plugin-*.zip` file from the Assets section
3. Open IntelliJ IDEA → Settings → Plugins
4. Click ⚙️ → Install Plugin from Disk
5. Select the downloaded `.zip` file
6. Restart the IDE

**Option 2: Build from Source**

1. Clone the repository and build:
   ```bash
   git clone https://github.com/setheclark/flocon-intellij.git
   cd flocon-intellij
   ./gradlew :plugin:buildPlugin
   ```

2. Install from disk:
   - Open IntelliJ IDEA → Settings → Plugins
   - Click ⚙️ → Install Plugin from Disk
   - Select `plugin/build/distributions/plugin-*.zip`
   - Restart the IDE

### Integrate Flocon SDK in Your Android App

To capture network traffic, you need to integrate the Flocon SDK into your Android app.

Follow the [official Flocon setup guide](https://openflocon.github.io/Flocon/setup) for integration instructions.

## Usage

### Basic Workflow

1. **Connect Your Device**
   - Connect Android device via USB or WiFi
   - Ensure ADB is enabled and device is authorized

2. **Open the Tool Window**
   - View → Tool Windows → Network Inspector
   - Or use keyboard shortcut (platform-specific)

3. **Run Your App**
   - Launch your app with Flocon SDK integrated
   - Network requests appear in real-time

4. **Inspect Traffic**
   - Click any request to see full details
   - Use filters to narrow down specific requests
   - Export data as needed

### Understanding the Interface

The Network Inspector tool window has several sections:

- **Device Selector** - Choose which connected device to monitor
- **Request List** - All captured network calls with method, URL, status, and duration
- **Detail Panel** - Full request/response information for selected call
- **Timeline View** - Visual representation of when calls occurred
- **Filters** - Refine visible requests by various criteria

### Configuration

Access settings via **Settings → Tools → Network Inspector**:

#### Network Storage Settings

- **Maximum Stored Calls** - How many calls to keep in memory (default: 1000)
- **Body Cache Size** - Memory limit for request/response bodies (default: 50 MB)
- **Max Body Size** - Truncate bodies larger than this (default: 1 MB)
- **Enable Compression** - GZIP compress stored bodies to save memory (default: enabled)

#### MCP Server Settings

- **Enable MCP Server** - Allow AI agents to query network data (default: enabled)
- **MCP Server Port** - Port for the MCP server to listen on (default: 8086)

Changes to MCP settings take effect immediately without restarting the IDE.

## MCP Server: AI-Powered Network Debugging

The MCP (Model Context Protocol) server exposes your network debugging data to AI agents, enabling intelligent assistance with API issues.

### What is MCP?

MCP is an open protocol that allows AI assistants (like Claude) to access tools and data sources. The Flocon MCP server provides three powerful tools for querying network traffic.

### Available MCP Tools

#### 1. `list_network_calls`

List recent network calls with optional filters.

**Parameters:**
- `deviceId` (optional) - Filter by device ID
- `packageName` (optional) - Filter by Android package
- `method` (optional) - Filter by HTTP method (GET, POST, etc.)
- `urlPattern` (optional) - Filter by URL regex pattern
- `statusCode` (optional) - Filter by HTTP status code
- `minDuration` (optional) - Filter by minimum duration in milliseconds
- `limit` (optional) - Maximum results (default: 100, max: 1000)

**Example:**
```json
{
  "deviceId": "emulator-5554",
  "packageName": "com.example.app",
  "method": "POST",
  "statusCode": 500,
  "limit": 50
}
```

#### 2. `get_network_call`

Get complete details for a specific network call. Use this after `list_network_calls` to drill down into a specific request.

**Parameters:**
- `callId` (required) - The unique ID returned in the "Call ID" field from `list_network_calls`

**Returns:**
- Full request details (headers, body, method, URL)
- Full response details (headers, body, status, duration)
- Timestamps and metadata

**Example:**
```json
{
  "callId": "call-abc123"
}
```

**Typical workflow:**
1. Call `list_network_calls` to see recent requests
2. Note the "Call ID: xxx" from a request of interest
3. Call `get_network_call` with that ID to see full details

#### 3. `filter_network_calls`

Advanced filtering with support for GraphQL operations and time ranges.

**Parameters:**
- `deviceId` (optional) - Filter by device
- `packageName` (optional) - Filter by package
- `requestType` (optional) - Filter by type: "Http", "GraphQl", or "Grpc"
- `graphQlOperationType` (optional) - For GraphQL: "query", "mutation", or "subscription"
- `graphQlOperationName` (optional) - Filter by GraphQL operation name
- `hasFailure` (optional) - Filter by success/failure status
- `startTimeAfter` (optional) - Filter calls after timestamp (epoch milliseconds)
- `startTimeBefore` (optional) - Filter calls before timestamp (epoch milliseconds)
- `limit` (optional) - Maximum results (default: 100, max: 1000)

**Example:**
```json
{
  "requestType": "GraphQl",
  "graphQlOperationType": "mutation",
  "hasFailure": false,
  "startTimeAfter": 1705334400000,
  "limit": 100
}
```

### Connecting AI Clients to MCP Server

The MCP server listens on `http://localhost:8086/mcp` by default (configurable in settings).

#### Claude Desktop

Add to your Claude Desktop configuration (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "flocon-network-inspector": {
      "url": "http://localhost:8086/mcp"
    }
  }
}
```

Restart Claude Desktop, and the Flocon tools will be available.

#### Custom MCP Clients

Connect any MCP-compatible client to the server URL. The server uses Server-Sent Events (SSE) transport and follows the MCP specification.

**Discovery:**
- The server advertises capabilities including three tools
- No authentication required (localhost-only by default)
- JSON-RPC 2.0 protocol over SSE

### MCP Use Cases

**Debugging Failed Requests:**
```
"Show me all failed network calls in the last hour"
→ AI queries filter_network_calls with hasFailure=true and time filter
```

**Analyzing GraphQL Operations:**
```
"Find all GraphQL mutations that took longer than 2 seconds"
→ AI combines requestType, graphQlOperationType, and duration filters
```

**Investigating Specific APIs:**
```
"Get full details for the call to /api/users/123 that returned 404"
→ AI uses list_network_calls with URL filter, then get_network_call for details
```

**Pattern Detection:**
```
"Are there any API calls failing consistently?"
→ AI queries multiple filters, analyzes patterns, suggests fixes
```

### MCP Server Configuration

**Enable/Disable:**
- Settings → Tools → Network Inspector
- Toggle "Enable MCP Server for AI Agent Access"
- Server restarts automatically

**Change Port:**
- Settings → Tools → Network Inspector
- Modify "MCP Server Port" (1024-65535)
- Server restarts automatically

**Security Note:**
- Server binds to `localhost` only (no remote access)
- No authentication (relies on localhost trust model)
- Read-only access (cannot modify or delete data)
- Network bodies may contain sensitive data (tokens, PII)

## Building from Source

### Prerequisites

- JDK 17 or later
- Gradle 8.5+ (wrapper included)

### Build Commands

```bash
# Compile the plugin
./gradlew :plugin:compileKotlin

# Build plugin distribution
./gradlew :plugin:buildPlugin

# Run in IDE sandbox
./gradlew :plugin:runIde

# Run tests
./gradlew :plugin:test
```

### Project Structure

```
flocon-intellij/
├── plugin/                 # IntelliJ plugin module
│   ├── src/main/kotlin/
│   │   ├── mcp/           # MCP server implementation
│   │   ├── network/       # Network data storage
│   │   ├── services/      # Application services
│   │   ├── settings/      # Configuration UI
│   │   └── ui/            # Tool window UI
│   └── build.gradle.kts
├── flocon-sources/        # Shared Flocon domain code
└── flocon-upstream/       # Upstream Flocon submodule
```

## Troubleshooting

### Plugin Not Detecting Devices

**Problem:** No devices appear in the device selector.

**Solutions:**
- Ensure ADB is installed and in your PATH
- Run `adb devices` to verify device connectivity
- Check USB debugging is enabled on the device
- Restart ADB: `adb kill-server && adb start-server`

### No Network Calls Appearing

**Problem:** App is running but no traffic shows up.

**Solutions:**
- Verify Flocon SDK is integrated in your app
- Ensure you're using a `debug` build (not `release`)
- Check your app calls `Flocon.initialize(this)` in `onCreate()`
- Look for Flocon logs in Logcat: `adb logcat | grep Flocon`

### MCP Server Not Starting

**Problem:** AI client cannot connect to MCP server.

**Solutions:**
- Check Settings → Tools → Network Inspector
- Verify "Enable MCP Server" is checked
- Ensure port 8086 is not in use: `lsof -i :8086`
- Change the port in settings if needed
- Check IDE logs for MCP server startup messages

### Port Already in Use

**Problem:** `Failed to start MCP server: Address already in use`

**Solutions:**
- Find what's using the port: `lsof -i :8086`
- Change MCP server port in settings
- Kill the conflicting process if appropriate

### High Memory Usage

**Problem:** Plugin consuming too much memory.

**Solutions:**
- Reduce "Maximum Stored Calls" in settings
- Lower "Body Cache Size" limit
- Enable body compression if disabled
- Clear old data by restarting the plugin

## License

[Add license information here]

## Links

- **Flocon SDK**: https://github.com/openflocon/Flocon
- **Issues**: [GitHub Issues](https://github.com/setheclark/flocon-intellij/issues)
- **Model Context Protocol**: https://modelcontextprotocol.io/

