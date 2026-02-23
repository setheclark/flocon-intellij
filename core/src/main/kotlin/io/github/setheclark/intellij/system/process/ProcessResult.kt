package io.github.setheclark.intellij.system.process

import io.github.openflocon.domain.common.Either

/**
 * Result of a process execution.
 */
typealias ProcessResult = Either<Throwable, String>