// Root build file for Flocon IntelliJ Plugin
// Main build logic is in plugin/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.changelog) apply false
    alias(libs.plugins.qodana) apply false
    alias(libs.plugins.kover) apply false
}

version = providers.gradleProperty("pluginVersion").get()

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}