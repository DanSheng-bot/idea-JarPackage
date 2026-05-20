package com.github.gedoor.jarpackage.ui

import com.github.gedoor.jarpackage.util.Constants
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.nio.file.Files
import java.nio.file.Path

/**
 * open the file in system file explorer
 */
class ActionShowExplorer(val filePath: Path) : AnAction(Constants.actionNameExplorer) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!Files.isDirectory(filePath)) {
            RevealFileAction.openFile(filePath.toFile())
        } else {
            RevealFileAction.openDirectory(filePath.toFile())
        }
    }

}