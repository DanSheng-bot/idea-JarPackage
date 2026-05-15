package com.github.gedoor.jarpackage.pack.impl

import com.github.gedoor.jarpackage.pack.Packager
import com.github.gedoor.jarpackage.util.CommonUtils
import com.github.gedoor.jarpackage.util.JarInfo
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import java.nio.file.Path

@Suppress("DuplicatedCode")
class AllPacker(
    dataContext: DataContext,
    private val exportPath: String,
    private val exportJarName: String
) : Packager(dataContext) {

    @Throws(Exception::class)
    override fun pack() {
        val allVfs = HashSet<VirtualFile>()
        val jarInfo = JarInfo()
        for (virtualFile in virtualFiles) {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile)
            if (psiDirectory != null) {
                val psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory)!!
                outputRoots.forEach loopOutput@{ outputRoot ->
                    var pvf: VirtualFile = outputRoot
                    val packageNames = psiPackage.qualifiedName
                        .split("\\.".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    for (n in packageNames) {
                        pvf = pvf.findChild(n) ?: return@loopOutput
                    }
                    CommonUtils.collectExportFilesNest(project, allVfs, pvf)
                    val outIndex = outputRoot.path.length + 1
                    val vfsList = allVfs.sortedBy { it.path }
                    for (vf in vfsList) {
                        val jarEntryName = vf.path.substring(outIndex)
                        jarInfo[jarEntryName] = vf
                    }
                }
                checkJarIsComplete(jarInfo, psiPackage)
            }
        }
        CommonUtils.createNewJar(project, Path.of(exportPath, exportJarName), jarInfo)
    }

}