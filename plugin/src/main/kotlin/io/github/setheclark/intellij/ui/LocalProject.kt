package io.github.setheclark.intellij.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.intellij.openapi.project.Project

val LocalProject = staticCompositionLocalOf<Project> { error("No LocalProject set") }

@Composable
fun WithProject(project: Project, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalProject provides project, content = content)
}
