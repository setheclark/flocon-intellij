package io.github.setheclark.intellij.network

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import co.touchlab.kermit.Logger
import io.github.setheclark.intellij.network.db.NetworkHistoryDatabase
import io.github.setheclark.intellij.util.withPluginTag
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.nio.file.Files
import javax.sql.DataSource

/**
 * Version of the history database layout. Bump it with every change to `NetworkCall.sq`.
 * History is a cache, so a mismatch drops and recreates the database instead of migrating it.
 */
internal const val NETWORK_HISTORY_SCHEMA_VERSION = 4

internal class OpenedHistoryDatabase(val database: NetworkHistoryDatabase, val driver: SqlDriver, val dataSource: DataSource)

/** Reads a single-integer PRAGMA, such as `page_count`. */
internal fun SqlDriver.pragmaLong(pragma: String): Long = executeQuery(
    identifier = null,
    sql = "PRAGMA $pragma",
    mapper = { cursor ->
        cursor.next()
        QueryResult.Value(cursor.getLong(0) ?: 0L)
    },
    parameters = 0,
).value

/** Folds the write-ahead log into the database file and truncates it, so the file on disk reflects what is stored. */
internal fun DataSource.checkpoint() {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        }
    }
}

/**
 * Opens the history database through a bundled SQLite JDBC driver. The driver is built from a data source, never
 * through the global `DriverManager`, which is empty inside the IDE (see ADR 0001).
 */
internal class NetworkHistoryDatabaseOpener {

    private val log = Logger.withPluginTag("NetworkHistoryDatabaseOpener")

    /**
     * Opens the database at [file]. If that fails, deletes the database files and tries once more.
     * Returns null when the second attempt fails too.
     */
    fun open(file: File): OpenedHistoryDatabase? {
        try {
            return attempt(file)
        } catch (t: Throwable) {
            log.w(t) { "Could not open network history database ${file.path}, recreating it" }
        }
        try {
            deleteDatabaseFiles(file)
            return attempt(file)
        } catch (t: Throwable) {
            log.e(t) { "Could not recreate network history database ${file.path}, history will be memory-only" }
            return null
        }
    }

    private fun attempt(file: File): OpenedHistoryDatabase {
        file.absoluteFile.parentFile?.mkdirs()

        var dataSource = newDataSource(file)
        var driver: SqlDriver = ListeningSqlDriver(dataSource.asJdbcDriver())
        try {
            val version = driver.userVersion()
            if (version != 0L && version != NETWORK_HISTORY_SCHEMA_VERSION.toLong()) {
                log.i { "Network history schema version $version does not match $NETWORK_HISTORY_SCHEMA_VERSION, recreating" }
                driver.close()
                deleteDatabaseFiles(file)
                dataSource = newDataSource(file)
                driver = ListeningSqlDriver(dataSource.asJdbcDriver())
            }
            val database = NetworkHistoryDatabase(driver)
            if (driver.userVersion() != NETWORK_HISTORY_SCHEMA_VERSION.toLong()) {
                enableAutoVacuum(dataSource)
                database.transaction {
                    NetworkHistoryDatabase.Schema.create(driver)
                    driver.execute(null, "PRAGMA user_version = $NETWORK_HISTORY_SCHEMA_VERSION", 0)
                }
            }
            return OpenedHistoryDatabase(database, driver, dataSource)
        } catch (t: Throwable) {
            runCatching { driver.close() }
            throw t
        }
    }

    private fun newDataSource(file: File): SQLiteDataSource {
        val config = SQLiteConfig().apply {
            // Readers and the single writer run on different connections.
            setJournalMode(SQLiteConfig.JournalMode.WAL)
            setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
            setBusyTimeout(BUSY_TIMEOUT_MS)
            enforceForeignKeys(true)
        }
        return SQLiteDataSource(config).apply { url = "jdbc:sqlite:${file.absolutePath}" }
    }

    /**
     * Makes SQLite hand freed pages back to the file as deletes commit, so retention limits the file on disk and
     * not just the space in use. A database that has been touched only changes its auto-vacuum mode through a
     * VACUUM, which is instant while it is still empty.
     */
    private fun enableAutoVacuum(dataSource: DataSource) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA auto_vacuum = FULL")
                statement.execute("VACUUM")
            }
        }
    }

    private fun SqlDriver.userVersion(): Long = pragmaLong("user_version")

    private fun deleteDatabaseFiles(file: File) {
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            Files.deleteIfExists(File(file.path + suffix).toPath())
        }
    }

    private companion object {
        const val BUSY_TIMEOUT_MS = 10_000
    }
}
