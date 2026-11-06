# Bundle xerial sqlite-jdbc with SQLDelight for network history

Network history is stored in SQLite through SQLDelight, using a `sqlite-jdbc` driver that the plugin bundles itself. The database is built from `SQLiteDataSource().asJdbcDriver()`, never through the global `DriverManager`. SQLDelight's Kotlin standard library and coroutines artifacts are excluded; the IDE's copies are used.

## Why not the IDE's SQLite

The original plan was to reuse a SQLite driver the IDE already ships, to avoid a second native library. A prototype showed that no such driver is available to plugins (branch `prototype/sqldelight-ide-sqlite`, `PROTOTYPE-FINDINGS.md`):

- The platform module `intellij.platform.sqlite` is JetBrains' own JNI binding (`org.jetbrains.sqlite`). It is not JDBC and it is annotated `@ApiStatus.Internal`.
- The xerial `org.sqlite.JDBC` driver exists in IDEA only inside `intellij.ide.startup.importSettings`, which has internal visibility, so depending on it prevents the plugin from loading. In Android Studio it lives in the Android plugin, so using it would make the plugin Android-Studio-only.
- A custom SQLDelight `SqlDriver` over `org.jetbrains.sqlite` was considered and rejected: it depends on an internal API that can break in any IDE release.

## Consequences

- A second SQLite native library lives next to the IDE's. The prototype ran cleanly in IDEA 2026.2.1 and Android Studio 2026.2.1, including Android Studio's own xerial copy in a separate classloader.
- The plugin bundle grows by about 12 MB (`sqlite-jdbc` carries every platform's native). It can be trimmed by repackaging if size matters.
- `DriverManager` is empty in the IDE, so the data-source driver is required, not optional.
- Blob storage is not rejected: a 100 MB body compressed to 20 MB wrote in about 0.1 s and read in about 0.03 s, with compression (2-4 s) as the dominant cost. Move large bodies to files only if later measurement shows a problem.
- Windows and Linux natives are in the jar but were not exercised in the prototype.
