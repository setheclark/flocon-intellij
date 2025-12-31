plugins {
    alias(libs.plugins.kotlin.jvm)
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
            srcDir("${rootProject.projectDir}/flocon-upstream/FloconDesktop/domain/src/commonMain/kotlin")
            exclude("**/DI.kt")
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    compileOnly(libs.paging.common)
}
