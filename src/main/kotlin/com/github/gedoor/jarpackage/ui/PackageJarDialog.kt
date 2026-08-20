package com.github.gedoor.jarpackage.ui

import com.github.gedoor.jarpackage.pack.PackageService
import com.github.gedoor.jarpackage.pack.impl.AllPacker
import com.github.gedoor.jarpackage.pack.impl.EachPacker
import com.github.gedoor.jarpackage.util.Messages
import com.github.gedoor.jarpackage.util.Util
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages.showErrorDialog
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.swing.JComponent
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

class PackageJarDialog(private val dataContext: DataContext) : DialogWrapper(true) {

    private val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!
    private val module = dataContext.getData(LangDataKeys.MODULE)!!
    private val virtualFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!

    // 状态变量
    private val propertyGraph = PropertyGraph()
    private var jarName = ""
    private var exportPath = ""
    private val exportEachProp = propertyGraph.property(false)
    private var fastMode = true

    private val properties = Properties()
    private val tempFile =
        File(project.basePath + File.separator + ".idea" + File.separator + "package-path.properties")

    private lateinit var mainPanel: DialogPanel

    init {
        title = "Package Jars"
        isResizable = false
        loadData() // 初始化数据
        init() // 必须调用，否则界面不显示
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = panel {

            // 第1行：Jar Name
            row("Jar name:") {
                textField()
                    .bindText({ jarName }, { jarName = it })
                    .columns(COLUMNS_MEDIUM)
                    .enabledIf(exportEachProp.not())
                    .comment("The name of the generated .jar file")
            }

            // 第2行：Output Path
            row("Output path:") {
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Select Output Path")

                @Suppress("UnstableApiUsage")
                textFieldWithBrowseButton(descriptor, project) { it.path }
                    .bindText({ exportPath }, { exportPath = it })
                    .align(AlignX.FILL)
            }

            // 第3行：选项组
            group("Options") {
                row {
                    checkBox("Export each children")
                        .bindSelected(exportEachProp)

                    checkBox("Fast mode")
                        .bindSelected({ fastMode }, { fastMode = it })
                }
            }
        }
        return mainPanel
    }


    override fun doOKAction() {
        // 更新内部状态
        mainPanel.apply()

        val nameInput = jarName.trim()
        val finalJarName = if (nameInput.lowercase().endsWith(".jar")) nameInput else "$nameInput.jar"
        Messages.clear(project)

        if (!Util.matchFileNamingConventions(finalJarName)) {
            showErrorDialog(project, "Please set a valid name for the output jar", "Invalid Name")
            return
        }

        val outDir = File(exportPath.trim())
        if (!outDir.exists()) {
            runCatching {
                outDir.mkdirs()
            }.onFailure {
                showErrorDialog(
                    project,
                    "The selected output path does not exist and make dir error",
                    "Path Error"
                )
                return
            }
        }

        saveSettings()
        super.doOKAction() // 关闭窗口

        // 执行打包逻辑
        val packager = if (exportEachProp.get()) {
            EachPacker(dataContext, exportPath.trim())
        } else {
            AllPacker(dataContext, exportPath.trim(), finalJarName)
        }

        val packageService = project.service<PackageService>()
        packageService.coroutineScope.launch {
            FileDocumentManager.getInstance().saveAllDocuments()
            if (executeBuild(project, module)) {
                delay(1.seconds)
                packager.invoke()
            }
        }
    }

    suspend fun executeBuild(project: Project, ideaModule: Module): Boolean {
        val taskManager = ProjectTaskManager.getInstance(project)
        // 使用 ProjectTaskListener 订阅编译总线
        val isSuccess = suspendCancellableCoroutine { continuation ->
            val connection = project.messageBus.connect()
            connection.subscribe(ProjectTaskListener.TOPIC, object : ProjectTaskListener {
                override fun finished(result: ProjectTaskManager.Result) {
                    connection.disconnect()
                    continuation.resume(!result.hasErrors() && !result.isAborted)
                }
            })
            when {
                fastMode -> taskManager.build(ideaModule)
                else -> taskManager.rebuild(ideaModule)
            }
        }
        return isSuccess
    }

    // --- 数据持久化逻辑
    private fun loadData() {
        try {
            if (tempFile.exists()) {
                FileInputStream(tempFile).use { properties.load(it) }
            }

            val key = getPropertyKey()

            // 1. 尝试从属性文件读取
            val savedJarName = properties.getProperty("JAR_$key")

            if (savedJarName != null) {
                jarName = savedJarName
            } else {
                // 2. 如果没存过，计算默认值
                val names = virtualFiles.mapNotNull { file ->
                    PsiManager.getInstance(project).findDirectory(file)?.let {
                        JavaDirectoryService.getInstance().getPackage(it)?.qualifiedName
                    }
                }

                var defaultName = Util.getTheSameStart(names)
                if (defaultName.isEmpty()) {
                    defaultName = module.name
                }

                if (defaultName.endsWith(".")) {
                    defaultName = defaultName.substring(0, defaultName.lastIndexOf("."))
                }
                jarName = defaultName
            }

            // 3. 读取路径逻辑
            exportPath = properties.getProperty(key)
                ?: (File(project.basePath!!, "out").path + File.separator + "JAR")

        } catch (e: Exception) {
            Messages.error(project, e.toString())
        }
    }

    private fun saveSettings() {
        try {
            val key = getPropertyKey()
            properties.setProperty("JAR_$key", jarName)
            properties.setProperty(key, exportPath)
            FileOutputStream(tempFile).use { properties.store(it, "Updated by Kotlin UI DSL") }
        } catch (e: Exception) {
            Messages.info(project, e.toString())
        }
    }

    private fun getPropertyKey(): String {
        val pKey = StringBuilder("MDL_${module.name}")
        for (file in virtualFiles) {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(file)
            if (psiDirectory != null) {
                val psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory)
                if (psiPackage != null) {
                    pKey.append("_PKG_").append(psiPackage.qualifiedName)
                }
            }
        }
        return pKey.toString().replace('.', '_')
    }
}