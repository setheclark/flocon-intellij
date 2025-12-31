plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("${rootProject.projectDir}/flocon-upstream/FloconDesktop/data/core/src/commonMain/kotlin")
            exclude("**/DI.kt")
        }
    }
}

dependencies {
    api(project(":flocon-sources:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)
    compileOnly(libs.paging.common)
}
