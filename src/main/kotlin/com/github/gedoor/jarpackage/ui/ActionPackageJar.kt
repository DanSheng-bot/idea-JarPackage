package com.github.gedoor.jarpackage.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.compiler.JavaCompilerBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackage
import org.apache.http.util.TextUtils

@Suppress("ControlFlowWithEmptyBody")
class ActionPackageJar : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.presentation
        if (presentation.isEnabled) {
            val dataContext = event.dataContext
            val project = CommonDataKeys.PROJECT.getData(dataContext)
            if (project == null) {
                presentation.isEnabled = false
                presentation.isVisible = false
                return
            }

            val virtualFiles = checkVirtualFiles(project, CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext))
            if (virtualFiles.isEmpty()) {
                presentation.isEnabled = false
                presentation.isVisible = false
                return
            }

            var psiPackage: PsiPackage? = null
            if (virtualFiles.size == 1) {
                val psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFiles[0])
                if (psiDirectory != null) {
                    psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory)
                }
            } else {
                val var12 = CommonDataKeys.PSI_ELEMENT.getData(dataContext)
                if (var12 is PsiPackage) {
                    psiPackage = var12
                }
            }

            val text: String
            if (psiPackage != null) {
                text = psiPackage.qualifiedName
            } else if (virtualFiles.size == 1) {
                val virtualFile = virtualFiles[0]
                text = virtualFile.name
            } else {
                text = JavaCompilerBundle.message("action.compile.description.selected.files")
            }

            if (TextUtils.isEmpty(text)) {
                presentation.isEnabled = false
            } else {
                presentation.setText(this.getButtonName(text), true)
                presentation.isEnabled = true
            }
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        // 1. 获取 DataContext
        val dataContext = event.dataContext

        // 2. 实例化你重构后的 Kotlin 对话框
        // 建议在 SettingsDialog 的 init 中设置好 title 和 resizable
        val dialog = PackageJarDialog(dataContext)

        // 3. 显示对话框
        // showAndGet() 会处理居中、模态显示，并返回用户是否点击了 OK
        if (dialog.showAndGet()) {
            // 如果需要，可以在这里处理点击 OK 后的额外逻辑
            // 但大部分逻辑建议已经封装在 SettingsDialog.doOKAction() 中
        }
    }

    private fun getButtonName(test: String): String {
        val sb = StringBuilder(40)
        sb.append("Package '")
        val length = test.length
        if (length > 23) {
            if (StringUtil.startsWithChar(test, '\'')) {
                sb.append("'")
            }

            sb.append("...")
            sb.append(test, length - 20, length)
        } else {
            sb.append(test)
        }
        sb.append("'")
        return sb.toString()
    }

    private fun checkVirtualFiles(project: Project, virtualFiles: Array<VirtualFile>?): Array<VirtualFile> {
        if (!virtualFiles.isNullOrEmpty()) {
            val psiManager = PsiManager.getInstance(project)
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val arrayList = ArrayList<VirtualFile>()

            for (virtualFile in virtualFiles) {
                if (projectFileIndex.isInSourceContent(virtualFile)
                    && virtualFile.isInLocalFileSystem
                    && virtualFile.isDirectory
                ) {
                    val var11 = psiManager.findDirectory(virtualFile)
                    if (var11 != null && JavaDirectoryService.getInstance().getPackage(var11) != null) {
                        arrayList.add(virtualFile)
                    }
                }
            }

            return VfsUtilCore.toVirtualFileArray(arrayList)
        } else {
            return VirtualFile.EMPTY_ARRAY
        }
    }

}
