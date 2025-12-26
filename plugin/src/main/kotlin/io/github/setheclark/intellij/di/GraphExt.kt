package io.github.setheclark.intellij.di

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.services.ApplicationService

val Project.appGraph: AppGraph
    get() = service<ApplicationService>().appGraph