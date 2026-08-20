package com.github.gedoor.jarpackage.util

import com.intellij.compiler.impl.ProblemsViewPanel
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.MessageView
import com.intellij.util.containers.ContainerUtil
import java.util.function.Consumer

@Suppress("MemberVisibilityCanBePrivate")
object Messages : Constants {
    private const val ID = "packing"

    @JvmStatic
    fun clear(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val messageView = MessageView.getInstance(project)
            messageView.runWhenInitialized {
                val contentManager = messageView.contentManager
                val contents = contentManager.contents
                for (content in contents) {
                    if ("packing" == content.tabName) {
                        val viewPanel = content.component as ProblemsViewPanel
                        viewPanel.close()
                        break
                    }
                }
            }
        }
    }

    @JvmStatic
    fun info(project: Project, text: String, file: VirtualFile? = null) {
        message(project, text, 3, file)
    }

    @JvmStatic
    fun error(project: Project, text: String, file: VirtualFile? = null) {
        message(project, text, 4, file)
        notify(NotificationType.ERROR, "PackageJar error", text)
    }

    @JvmStatic
    fun message(project: Project, text: String, type: Int, file: VirtualFile? = null) {
        ApplicationManager.getApplication().invokeLater {
            val messageView = MessageView.getInstance(project)
            messageView.runWhenInitialized {
                activateMessageWindow(project)
                var packMessages: ProblemsViewPanel? = null
                val contents = messageView.contentManager.contents
                for (content in contents) {
                    if ("packing" == content.tabName) {
                        packMessages = content.component as ProblemsViewPanel
                        break
                    }
                }
                if (packMessages == null) {
                    packMessages = ProblemsViewPanel(project)
                    val content = ContentFactory.getInstance().createContent(packMessages, ID, true)
                    messageView.contentManager.addContent(content)
                    @Suppress("UsePropertyAccessSyntax")
                    messageView.contentManager.setSelectedContent(content)
                }
                packMessages.addMessage(type, arrayOf(text), file, -1, -1, null)
            }
        }
    }

    @JvmStatic
    fun activateMessageWindow(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW)
            toolWindow?.show(null)
        }
    }

    /**
     * show info notification popup with actions
     *
     * @param title   title
     * @param message content
     * @param actions actions show in popup and event log window
     */
    @JvmStatic
    @JvmOverloads
    fun notify(type: NotificationType, title: String, message: String, actions: List<AnAction>? = null) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification("Export Jar", title, message, type)
            ContainerUtil.notNullize(actions).forEach(Consumer { action: AnAction ->
                notification.addAction(action)
            })
            Notifications.Bus.notify(notification)
        }
    }
}