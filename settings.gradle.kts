rootProject.name = "flocon-intellij"

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }
}

include(":plugin")

// Flocon source modules - include sources directly with DI.kt exclusions
include(":flocon-sources:domain")
include(":flocon-sources:data-core")
include(":flocon-sources:data-remote")
