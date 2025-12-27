package io.github.setheclark.intellij.db

import app.cash.sqldelight.ColumnAdapter
import dev.zacsweers.metro.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Inject
class MapColumnAdapter : ColumnAdapter<Map<String, String>, String> {
    override fun decode(databaseValue: String): Map<String, String> {
        val data = Json.parseToJsonElement(databaseValue)

        return (data as? JsonObject)?.mapValues { (key, value) -> value.toString() }.orEmpty()
    }

    override fun encode(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }
}