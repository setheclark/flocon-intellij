package io.github.setheclark.intellij.util

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity

fun buildCurlCommand(call: NetworkCallEntity, requestBody: String?): String = buildString {
    append("curl -X ")
    append(call.request.method)
    call.request.headers.forEach { (name, value) ->
        append(" \\\n  -H '")
        append(name.replace("'", "'\\''"))
        append(": ")
        append(value.replace("'", "'\\''"))
        append("'")
    }
    requestBody?.let { body ->
        append(" \\\n  -d '")
        append(body.replace("'", "'\\''"))
        append("'")
    }
    append(" \\\n  '")
    append(call.request.url.replace("'", "'\\''"))
    append("'")
}
