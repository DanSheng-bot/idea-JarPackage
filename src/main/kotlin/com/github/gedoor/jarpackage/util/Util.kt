package com.github.gedoor.jarpackage.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

object Util {
    @JvmStatic
    fun matchFileNamingConventions(fileName: String): Boolean {
        return fileName.matches("[^/\\\\<>*?|\"]+".toRegex())
    }

    /**
     * 遍历目录
     */
    fun iterateDirectory(project: Project, directories: HashSet<VirtualFile>, directory: VirtualFile?) {
        if (directory != null) {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(directory)
            directories.add(psiDirectory!!.virtualFile)
            val psiDirectories = psiDirectory.subdirectories
            for (pd in psiDirectories) {
                iterateDirectory(project, directories, pd.virtualFile)
            }
        }
    }

    /**
     * 找出给定的字符串列表中所有字符串的“最长公共前缀”
     */
    fun getTheSameStart(strings: List<String>?): String {
        return if (!strings.isNullOrEmpty()) {
            var max = 888888
            for (string in strings) {
                if (string.length < max) {
                    max = string.length
                }
            }
            val sb = StringBuilder()
            val set = HashSet<Char>()
            for (i in 0 until max) {
                for (string in strings) {
                    set.add(string[i])
                }
                if (set.size != 1) {
                    break
                }
                sb.append(set.iterator().next())
                set.clear()
            }
            sb.toString()
        } else {
            ""
        }
    }

}