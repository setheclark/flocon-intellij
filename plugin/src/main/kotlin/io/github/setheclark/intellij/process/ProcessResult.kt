package io.github.setheclark.intellij.process

import io.github.openflocon.domain.common.Either

/**
 * Result of a process execution.
 */
typealias ProcessResult = Either<Throwable, String>
//data class ProcessResult(
//    val exitCode: Int,
//    val output: String,
//    val errorOutput: String = ""
//) {
//    val isSuccess: Boolean get() = exitCode == 0
//}