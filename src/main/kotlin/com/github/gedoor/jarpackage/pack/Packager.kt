package com.github.gedoor.jarpackage.pack

import com.github.gedoor.jarpackage.util.Messages
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.vfs.VirtualFile

abstract class Packager(dataContext: DataContext) {

    protected val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!
    protected val virtualFiles: Array<VirtualFile> = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!
    protected val module = dataContext.getData(LangDataKeys.MODULE)!!
    protected val outPutDir: VirtualFile = CompilerModuleExtension.getInstance(module)!!.compilerOutputPath!!

    @Throws(Exception::class)
    abstract fun pack()

    suspend fun invoke() {
        runCatching {
            readAction {
                pack()
            }
        }.onFailure {
            Messages.error(project, it.localizedMessage)
        }
    }

}