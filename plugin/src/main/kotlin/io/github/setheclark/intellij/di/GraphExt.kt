package io.github.setheclark.intellij.di

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.services.FloconApplicationService
import io.github.setheclark.intellij.services.FloconProjectService

val Project.appGraph: AppGraph
    get() = service<FloconApplicationService>().appGraph

val Project.projectGraph: ProjectGraph
    get() = service<FloconProjectService>().projectGraph