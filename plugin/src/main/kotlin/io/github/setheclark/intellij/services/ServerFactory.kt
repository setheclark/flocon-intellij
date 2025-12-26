package io.github.setheclark.intellij.services

import com.flocon.data.remote.server.Server
import com.flocon.data.remote.server.ServerJvm
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json

/**
 * Factory for creating Flocon Server instances.
 * This abstraction allows for testing without a real server.
 */
interface ServerFactory {
    /**
     * Create a new Server instance.
     * @param json The JSON serializer configuration
     * @return A new Server instance
     */
    fun createServer(json: Json): Server
}

/**
 * Default implementation that creates a real ServerJvm instance.
 */
@Inject
class FloconServerFactory : ServerFactory {
    override fun createServer(json: Json): Server {
        return ServerJvm(json)
    }
}
