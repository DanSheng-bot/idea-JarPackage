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
        if (strings.isNullOrEmpty()) return ""

        // 1. 找出列表中字典序最小和最大的两个字符串
        // 共同前缀必定完全包含在字典序两极的字符串中
        val sorted = strings.sorted()
        val first = sorted.first()
        val last = sorted.last()

        val sb = StringBuilder()
        // 2. 只需要比对这两个字符串即可，无需每次都遍历整个列表，也无需使用 HashSet
        for (i in 0 until minOf(first.length, last.length)) {
            if (first[i] == last[i]) {
                sb.append(first[i])
            } else {
                break
            }
        }
        return sb.toString()
    }

}