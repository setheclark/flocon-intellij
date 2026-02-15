# Flocon Source Modules

This directory contains Gradle modules that include source code from the `upstream` submodule with specific exclusions.

## Why Not Use Composite Build?

The original setup used a Gradle composite build to include FloconDesktop modules:

```kotlin
includeBuild("flocon-upstream/FloconDesktop") {
    dependencySubstitution {
        substitute(module("io.github.openflocon.desktop:domain")).using(project(":domain"))
        // ...
    }
}
```

However, composite builds treat included projects as opaque units. You cannot:
- Filter or exclude source files from the included project
- Modify dependencies of the included project
- Control the JVM toolchain version

## The Solution

Instead of composite build, these modules include FloconDesktop sources directly with exclusions:

```kotlin
sourceSets {
    main {
        kotlin {
            srcDir("${rootProject.projectDir}/flocon/upstream/FloconDesktop/domain/src/commonMain/kotlin")
            exclude("**/DI.kt")
        }
    }
}
```

This approach:
- Provides granular control over which source files are included
- Allows excluding unnecessary transitive dependencies
- Allows direct control of JVM toolchain (required for IntelliJ Platform 2024.3+ compatibility)
- Preserves all other FloconDesktop functionality
- Does not require modifying the upstream submodule

## Module Structure

| Module | Sources From | Notes |
|--------|-------------|-------|
| `domain` | `FloconDesktop/domain/src/commonMain` | Uses kotlin-jvm plugin |
| `data-core` | `FloconDesktop/data/core/src/commonMain` | Uses kotlin-jvm plugin |
| `data-remote` | `FloconDesktop/data/remote/src/{commonMain,desktopMain}` | Uses kotlin-multiplatform plugin (required for expect/actual declarations) |

## Updating FloconDesktop

When updating the `upstream` submodule, check for:
1. New dependencies added to FloconDesktop modules that may need to be added here
2. New source directories that may need to be included
3. Changes to the expect/actual structure in data-remote
