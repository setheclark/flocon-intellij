plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)

    jvm("desktop")

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("${rootProject.projectDir}/flocon-upstream/FloconDesktop/data/remote/src/commonMain/kotlin")
            kotlin.exclude("**/DI.kt")

            dependencies {
                api(project(":flocon-sources:domain"))
                api(project(":flocon-sources:data-core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kermit)
            }
        }

        val desktopMain by getting {
            kotlin.srcDir("${rootProject.projectDir}/flocon-upstream/FloconDesktop/data/remote/src/desktopMain/kotlin")
            kotlin.exclude("**/DI.kt")

            dependencies {
                // Ktor server
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.websockets)

                // Ktor client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
            }
        }
    }
}
