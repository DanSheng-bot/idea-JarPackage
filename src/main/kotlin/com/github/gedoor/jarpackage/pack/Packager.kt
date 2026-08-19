package com.github.gedoor.jarpackage.pack

import com.github.gedoor.jarpackage.util.JarInfo
import com.github.gedoor.jarpackage.util.Messages
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiPackage
import kotlinx.io.IOException

abstract class Packager(dataContext: DataContext) {

    protected val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!
    protected val virtualFiles: Array<VirtualFile> = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!
    protected val module = dataContext.getData(LangDataKeys.MODULE)!!
    val outputRoots: ArrayList<VirtualFile> = arrayListOf()

    suspend fun invoke() {
        runCatching {
            val javaRoot: VirtualFile? = CompilerModuleExtension.getInstance(module)?.compilerOutputPath
            if (javaRoot != null) {
                //向上找两级到 classes/ 目录
                val classesDir = javaRoot.parent?.parent
                if (classesDir != null) {
                    // true 表示同步（Synchronous），true 表示递归（Recursive）
                    // markDirtyAndRefresh 是目前最强力的 VFS 刷新手段
                    VfsUtil.markDirtyAndRefresh(false, true, true, classesDir)
                }
                outputRoots.add(javaRoot)

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

    @Throws(Exception::class)
    protected abstract fun pack()

    protected fun checkJarIsComplete(jarInfo: JarInfo, psiPackage: PsiPackage) {
        val classList = psiPackage.getAllTopClassQualifiedNamesRecursive()
        classList.forEach { className ->
            val entryName = className.replace('.', '/')
            val findEntryName = jarInfo.keys.find { it.startsWith(entryName) }
            if (findEntryName == null) {
                throw IOException("$className not found in JarInfo")
            }
        }
    }

    /**
     * 获取当前包下所有顶级类全类名（包名.类名），自动排除内部类
     */
    protected fun PsiPackage.getTopClassQualifiedNames(): Set<String> {
        return classes
            .filter { it.isValid && it.containingClass == null }
            .mapNotNull { it.qualifiedName }
            .toSet()
    }

    /**
     * 递归获取当前包及所有子包下 全部顶级类全类名
     */
    protected fun PsiPackage.getAllTopClassQualifiedNamesRecursive(): Set<String> {
        val result = hashSetOf<String>()
        // 当前包类
        result.addAll(getTopClassQualifiedNames())
        // 递归子包
        subPackages.forEach {
            result.addAll(it.getAllTopClassQualifiedNamesRecursive())
        }
        return result
    }
}