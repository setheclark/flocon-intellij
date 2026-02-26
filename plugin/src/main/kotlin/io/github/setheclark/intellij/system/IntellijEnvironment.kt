package io.github.setheclark.intellij.system

import com.intellij.util.EnvironmentUtil
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class IntellijEnvironment : Environment {
    override fun getEnvironmentVariable(key: String): String? {
        return EnvironmentUtil.getValue(key)
    }
}
