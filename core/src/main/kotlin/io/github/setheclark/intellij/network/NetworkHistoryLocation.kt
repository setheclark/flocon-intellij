package io.github.setheclark.intellij.network

import java.io.File

/**
 * Where the persisted network history database lives. Supplied by the host, which owns the system directory layout.
 */
interface NetworkHistoryLocation {
    val databaseFile: File
}
