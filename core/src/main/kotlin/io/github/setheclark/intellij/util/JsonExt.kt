package io.github.setheclark.intellij.util

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json

internal inline fun <reified T> Json.safeDecode(data: String): T? = try {
    decodeFromString(data)
} catch (t: Throwable) {
    Logger.e("Error decoding json", t)
    null
}
