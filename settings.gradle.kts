rootProject.name = "Intellij Flocon Plugin"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":plugin")
include(":core")

// Flocon source modules - include sources directly with exclusions
include(":flocon:domain")
include(":flocon:data-core")
include(":flocon:data-remote")
