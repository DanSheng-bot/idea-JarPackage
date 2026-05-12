package com.github.gedoor.jarpackage.ui

import com.github.gedoor.jarpackage.pack.impl.AllPacker
import com.github.gedoor.jarpackage.pack.impl.EachPacker
import com.github.gedoor.jarpackage.util.Messages
import com.github.gedoor.jarpackage.util.Util
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages.showErrorDialog
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.swing.JComponent

class SettingsDialog(private val dataContext: DataContext) : DialogWrapper(true) {

    private val project: Project = dataContext.getData(CommonDataKeys.PROJECT)!!
    private val module = dataContext.getData(LangDataKeys.MODULE)!!
    private val virtualFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!

    // 状态变量
    private var jarName = ""
    private var exportPath = ""
    private var exportEachChildren = false
    private var fastMode = true

    private val properties = Properties()
    private val tempFile =
        File(project.basePath + File.separator + ".idea" + File.separator + "package-path.properties")

    private lateinit var mainPanel: DialogPanel

    init {
        title = "Package Jars"
        isResizable = false
        loadSettings() // 初始化数据  [cite: 2, 70-120]
        init() // 必须调用，否则界面不显示
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = panel {
            // 1. 定义一个变量，通过在一个不显示的 row 中创建它来完成初始化
            lateinit var exportEachCb: Cell<JBCheckBox>

            // 这里我们不在顶层直接写 checkBox，因为那不是合法的 DSL 语法
            // 我们利用一个占位符或在后面的 row 中定义，但为了解决顺序问题，可以这样做：

            row {
                // 先在这里创建，但我们一会儿把它“移动”到 Options 组
                exportEachCb = checkBox("Export each children")
                    .bindSelected({ exportEachChildren }, { exportEachChildren = it })
            }.visible(false) // 这一行设为不可见，仅仅为了初始化变量

            // 第二行：Jar Name
            row("Jar name:") {
                textField()
                    .bindText({ jarName }, { jarName = it })
                    .columns(COLUMNS_MEDIUM)
                    // 现在 exportEachCb 已经初始化过了，可以使用 .selected
                    .enabledIf(exportEachCb.selected.not())
                    .comment("The name of the generated .jar file")
            }

            // 第三行：Output Path
            row("Output path:") {
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Select Output Path")

                @Suppress("UnstableApiUsage")
                textFieldWithBrowseButton(descriptor, project) { it.path }
                    .bindText({ exportPath }, { exportPath = it })
                    .align(AlignX.FILL)
            }

            // 第四行：选项组
            group("Options") {
                row {
                    // 将上面创建好的组件通过 cell 放置在这里显示出来
                    cell(exportEachCb.component)
                        .visible(true) // 确保在这里是可见的状态

                    checkBox("Fast mode")
                        .bindSelected({ fastMode }, { fastMode = it })
                }
            }
        }
        return mainPanel
    }

    // 对应原 java 中的 onOK() 逻辑  [cite: 2, 151-177]
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
            showErrorDialog(project, "The selected output path does not exist", "Path Error")
            return
        }

        // 执行打包逻辑  [cite: 2, 164-173]
        val packager = if (exportEachChildren) {
            EachPacker(dataContext, exportPath.trim())
        } else {
            AllPacker(dataContext, exportPath.trim(), finalJarName)
        }

        if (fastMode) {
            CompilerManager.getInstance(project).make(module, packager)
        } else {
            CompilerManager.getInstance(project).compile(module, packager)
        }

        saveSettings()
        super.doOKAction() // 关闭窗口
    }

    // --- 数据持久化逻辑 (保持原逻辑不变)  [cite: 2, 104-124] ---

    private fun loadSettings() {
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
                // 2. 如果没存过，计算默认值  [cite: 2, 108-115]
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

            // 3. 读取路径逻辑  [cite: 2, 118-124]
            exportPath = properties.getProperty(key) ?: (File(project.basePath!!).parent + File.separator + "JAR")

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