plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
}

ktlint {
    version.set("1.8.0")
    outputToConsole.set(true)
    filter {
        // SQLDelight generates the database classes into the build directory
        exclude { it.file.path.contains("/build/generated/") }
    }
}

sqldelight {
    databases {
        create("NetworkHistoryDatabase") {
            packageName.set("io.github.setheclark.intellij.network.db")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.time.ExperimentalTime",
        )
    }
}

dependencies {
    api(project(":flocon:domain"))
    api(project(":flocon:data-core"))
    api(project(":flocon:data-remote"))
    implementation(libs.kermit)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqldelight.jdbc)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.assertions)
}
