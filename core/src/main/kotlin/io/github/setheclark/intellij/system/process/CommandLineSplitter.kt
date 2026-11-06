package io.github.setheclark.intellij.system.process

/**
 * Splits a command string into arguments on whitespace that is outside double quotes.
 * Quote characters are kept in the resulting arguments, so `shell "a b"` becomes
 * `["shell", "\"a b\""]`. An unterminated quote extends to the end of the string.
 */
fun splitCommandLine(command: String): List<String> {
    val args = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (c in command) {
        when {
            c == '"' -> {
                inQuotes = !inQuotes
                current.append(c)
            }

            c.isWhitespace() && !inQuotes -> {
                if (current.isNotEmpty()) {
                    args += current.toString()
                    current.clear()
                }
            }

            else -> current.append(c)
        }
    }
    if (current.isNotEmpty()) args += current.toString()
    return args
}
