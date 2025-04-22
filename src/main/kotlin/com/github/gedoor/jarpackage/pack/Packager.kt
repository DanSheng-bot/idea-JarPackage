package com.github.gedoor.jarpackage.pack

import com.github.gedoor.jarpackage.util.Messages
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.project.Project

abstract class Packager(dataContext: DataContext) : CompileStatusNotification {

    protected val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!

    @Throws(Exception::class)
    abstract fun pack()

    override fun finished(b: Boolean, error: Int, i1: Int, compileContext: CompileContext) {
        if (error == 0) {
            try {
                ApplicationManager.getApplication().runWriteAction {
                    pack()
                }
            } catch (e: Exception) {
                Messages.error(project, e.localizedMessage)
                e.printStackTrace()
            }
        } else {
            Messages.error(project, "compile error")
        }
    }

}