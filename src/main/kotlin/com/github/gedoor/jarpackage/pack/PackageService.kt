package com.github.gedoor.jarpackage.pack

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class PackageService(val project: Project, val coroutineScope: CoroutineScope) {
    // 此时平台会自动为您注入正确的、生命周期安全的 coroutineScope
}