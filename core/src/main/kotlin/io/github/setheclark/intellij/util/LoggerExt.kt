package io.github.setheclark.intellij.util

import co.touchlab.kermit.Logger

fun Logger.withPluginTag(tag: String): Logger = Logger.withTag(">>> $tag")