package com.github.gedoor.jarpackage.util

import com.github.gedoor.jarpackage.util.Messages.info
import com.github.gedoor.jarpackage.util.Messages.notify
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.regex.Pattern

@Suppress("unused")
object CommonUtils {
    /**
     * 不需要打包的文档
     */
    private val pattern = Pattern.compile(".*?\\.(doc|docx|xls|xlsx|ppt|pptx)$", Pattern.CASE_INSENSITIVE)
    private val versionOpcodes = try {
        val apiVersion = Opcodes::class.java.getField("API_VERSION")
        apiVersion[null] as Int
    } catch (e: Exception) {
        Opcodes.API_VERSION
    }

    /**
     * 收集需要打包的文件
     */
    @JvmStatic
    fun collectExportFilesNest(project: Project, collected: MutableSet<VirtualFile>, parentVf: VirtualFile) {
        if (!parentVf.isDirectory) {
            if (!pattern.matcher(parentVf.name).matches()) {
                collected.add(parentVf)
            }
        } else {
            @Suppress("UnsafeVfsRecursion")
            parentVf.children.forEach {
                if (it.isDirectory) {
                    collectExportFilesNest(project, collected, it)
                } else if (!pattern.matcher(it.name).matches()) {
                    collected.add(it)
                }
            }
        }
    }

    /**
     * 创建jar文件
     */
    @JvmStatic
    fun createNewJar(project: Project, jarFileFullPath: Path, jarInfo: JarInfo) {
        val manifest = Manifest()
        val mainAttributes = manifest.mainAttributes
        mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        mainAttributes[Attributes.Name("Created-By")] = Constants.creator
        try {
            Files.newOutputStream(jarFileFullPath).use { os ->
                BufferedOutputStream(os).use { bos ->
                    JarOutputStream(bos, manifest).use { jos ->
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                        info(project, "start package $jarFileFullPath at ${dateFormat.format(Date())}")
                        jarInfo.forEach { (entryName, virtualFile) ->
                            val jarEntry = JarEntry(entryName)
                            jarEntry.lastModifiedTime = FileTime.fromMillis(virtualFile.timeStamp)
                            jos.putNextEntry(jarEntry)
                            if (!virtualFile.isDirectory) {
                                jos.write(virtualFile.contentsToByteArray())
                            }
                            jos.closeEntry()
                            info(project, "packed $entryName")
                        }
                        info(project, "packageJar success $jarFileFullPath")
                        notify(
                            NotificationType.INFORMATION,
                            "packageJar Success",
                            jarFileFullPath.toString(),
                            listOf(ActionShowExplorer(jarFileFullPath))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}