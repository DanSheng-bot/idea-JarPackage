package com.github.gedoor.jarpackage.pack.impl

import com.github.gedoor.jarpackage.pack.Packager
import com.github.gedoor.jarpackage.util.CommonUtils
import com.github.gedoor.jarpackage.util.JarInfo
import com.github.gedoor.jarpackage.util.Util
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import java.nio.file.Path

@Suppress("DuplicatedCode")
class EachPacker(dataContext: DataContext, private val exportPath: String) : Packager(dataContext) {

    @Throws(Exception::class)
    override fun pack() {
        val directories = HashSet<VirtualFile>()
        for (virtualFile in virtualFiles) {
            Util.iterateDirectory(project, directories, virtualFile)
        }
        val iterator: Iterator<VirtualFile> = directories.iterator()
        while (true) {
            var psiDirectory: PsiDirectory?
            do {
                if (!iterator.hasNext()) {
                    return
                }
                val directory = iterator.next()
                psiDirectory = PsiManager.getInstance(project).findDirectory(directory)
            } while (psiDirectory == null)
            val psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory)!!
            val allVfs = HashSet<VirtualFile>()
            val jarInfo = JarInfo()
            outputRoots.forEach loopOutput@{ outputDir ->
                var pvf: VirtualFile = outputDir
                val packageNames = psiPackage.qualifiedName
                    .split("\\.".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (n in packageNames) {
                    pvf = pvf.findChild(n) ?: return@loopOutput
                }
                CommonUtils.collectExportFilesNest(project, allVfs, pvf)
                val outIndex = outputDir.path.length + 1
                val vfList = allVfs.sortedBy { it.path }
                for (vf in vfList) {
                    val jarEntryName = vf.path.substring(outIndex)
                    jarInfo[jarEntryName] = vf
                }
            }
            checkJarIsComplete(jarInfo, psiPackage)
            CommonUtils.createNewJar(
                project,
                Path.of(exportPath, psiPackage.qualifiedName + ".jar"),
                jarInfo
            )
        }
    }


}