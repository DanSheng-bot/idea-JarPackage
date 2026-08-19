package com.github.gedoor.jarpackage.ui

import com.github.gedoor.jarpackage.util.Constants
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path

/**
 * open the file by default app
 */
class ActionOpenFile(val filePath: Path) : AnAction(Constants.actionNameOpen) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!Files.isDirectory(filePath)) {
            Desktop.getDesktop().open(filePath.toFile())
        } else {
            RevealFileAction.openDirectory(filePath.toFile())
        }
    }

}