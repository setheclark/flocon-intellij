package io.github.setheclark.intellij.util

import java.net.URI
import java.net.URLDecoder

/**
 * Parses query parameters from a URL string.
 *
 * @param url The URL string to parse
 * @return A map of query parameter names to their values. Returns empty map if no query string
 *         or if the URL is malformed.
 *
 * Edge cases handled:
 * - No query string → empty map
 * - Malformed URL → empty map
 * - Parameters without values (e.g., ?debug) → map key to empty string
 * - URL-encoded characters → properly decoded
 */
fun parseQueryParameters(url: String): Map<String, String> {
    return try {
        val uri = URI(url)
        val query = uri.query ?: return emptyMap()

        query.split('&')
            .mapNotNull { param ->
                val parts = param.split('=', limit = 2)
                if (parts.isEmpty()) {
                    null
                } else {
                    val key = URLDecoder.decode(parts[0], "UTF-8")
                    val value = if (parts.size > 1) {
                        URLDecoder.decode(parts[1], "UTF-8")
                    } else {
                        ""
                    }
                    key to value
                }
            }
            .toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}
