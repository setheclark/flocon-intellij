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

// Flocon source modules - include sources directly with exclusions
include(":flocon-sources:domain")
include(":flocon-sources:data-core")
include(":flocon-sources:data-remote")
