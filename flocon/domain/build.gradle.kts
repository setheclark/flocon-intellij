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
            srcDir("${rootProject.projectDir}/flocon/upstream/FloconDesktop/domain/src/commonMain/kotlin")
            exclude("**/DI.kt")
            // Depend on FileKit, which the plugin doesn't use
            exclude(
                "**/analytics/usecase/ExportAnalyticsToCsvUseCase.kt",
                "**/network/usecase/CreateNetworkLogHtmlUseCase.kt",
                "**/network/usecase/ExportNetworkCallsToCsvUseCase.kt",
                "**/network/usecase/ImportNetworkCallsFromCsvUseCase.kt",
                "**/table/usecase/ExportTableToCsvUseCase.kt",
            )
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.dateTime)
    compileOnly(libs.paging.common)
}
