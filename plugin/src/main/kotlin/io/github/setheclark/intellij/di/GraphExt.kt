package io.github.setheclark.intellij.di

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import io.github.setheclark.intellij.services.ApplicationService

val appGraph: AppGraph
    get() = ApplicationManager.getApplication().service<ApplicationService>().appGraph
