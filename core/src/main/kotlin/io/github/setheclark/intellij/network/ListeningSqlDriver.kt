package io.github.setheclark.intellij.network

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Adds query change notifications to [delegate].
 *
 * The SQLDelight driver built from a `DataSource` ignores listeners, so `Query.asFlow()` would never re-emit.
 * Everything else, including connection handling, is the delegate's.
 */
internal class ListeningSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {

    private val listeners = ConcurrentHashMap<String, CopyOnWriteArraySet<Query.Listener>>()

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach { key -> listeners.getOrPut(key) { CopyOnWriteArraySet() }.add(listener) }
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach { key -> listeners[key]?.remove(listener) }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        queryKeys
            .flatMap { key -> listeners[key].orEmpty() }
            .toSet()
            .forEach { it.queryResultsChanged() }
    }
}
