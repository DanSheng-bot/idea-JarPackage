//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
package com.github.gedoor.jarpackage.pack

import com.github.gedoor.jarpackage.util.Messages
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service
abstract class Packager(dataContext: DataContext) : CompileStatusNotification {

    protected val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!

    @Throws(Exception::class)
    abstract fun pack()

    override fun finished(b: Boolean, error: Int, i1: Int, compileContext: CompileContext) {
        if (error == 0) {
            try {
                pack()
            } catch (e: Exception) {
                Messages.error(project, e.localizedMessage)
                e.printStackTrace()
            }
        } else {
            Messages.error(project, "compile error")
        }
    }

}