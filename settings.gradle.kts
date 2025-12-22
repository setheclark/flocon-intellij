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

// Include Flocon Desktop as a composite build
// This allows us to depend on its modules directly without publishing them
includeBuild("flocon-upstream/FloconDesktop") {
    dependencySubstitution {
        // Map our dependency coordinates to the Flocon projects
        substitute(module("io.github.openflocon.desktop:domain"))
            .using(project(":domain"))
        substitute(module("io.github.openflocon.desktop:data-core"))
            .using(project(":data:core"))
        substitute(module("io.github.openflocon.desktop:data-remote"))
            .using(project(":data:remote"))
    }
}

include(":plugin")
