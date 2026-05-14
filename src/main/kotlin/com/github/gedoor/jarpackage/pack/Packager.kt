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
    val javaRoot: VirtualFile? = CompilerModuleExtension.getInstance(module)?.compilerOutputPath
    val outputRoots: ArrayList<VirtualFile> = arrayListOf()

    @Throws(Exception::class)
    abstract fun pack()

    suspend fun invoke() {
        runCatching {
            if (javaRoot != null) {
                outputRoots.add(javaRoot)

                // 2. 向上找两级到 classes/ 目录
                val classesDir = javaRoot.parent?.parent

                // 3. 横向寻找对应的 kotlin 编译目录 (如: .../build/classes/kotlin/main)
                // javaRoot.name 通常是 "main" 或 "test"
                val kotlinRoot = classesDir?.findChild("kotlin")?.findChild(javaRoot.name)

                if (kotlinRoot != null && kotlinRoot.exists()) {
                    outputRoots.add(kotlinRoot)
                }
            }
            readAction {
                pack()
            }
        }.onFailure {
            Messages.error(project, it.localizedMessage)
        }
    }

}