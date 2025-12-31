plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.metro)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    google()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // IntelliJ Platform
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
    }

    // Flocon Desktop modules (source inclusion with DI.kt exclusions)
    // These provide the WebSocket server, protocol handling, and domain logic
    implementation(project(":flocon-sources:domain")) { excludeBundledDependencies() }
    implementation(project(":flocon-sources:data-core")) { excludeBundledDependencies() }
    implementation(project(":flocon-sources:data-remote")) { excludeBundledDependencies() }

    implementation(libs.kermit) { excludeBundledDependencies() }

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.assertions)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    // Ensure we use the desktop JVM artifacts from the KMP modules
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    // Configure JUnit 5 for tests
    test {
        useJUnitPlatform()
    }

    runIde {
        // Clear inherited JVM args from Gradle / IDE
        jvmArgs = listOf(
            "-Dkotlinx.coroutines.debug=off",
            "-Didea.is.internal=false",
            "-Xshare:off",
        )
    }
}

// Exclude transitive dependencies that conflict with IDE bundled libraries
private fun ModuleDependency.excludeBundledDependencies() {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}