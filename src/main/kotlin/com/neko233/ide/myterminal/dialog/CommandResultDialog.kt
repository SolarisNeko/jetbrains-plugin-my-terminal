package com.neko233.ide.myterminal.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.neko233.ide.myterminal.utils.terminal.CommandResult
import javax.swing.JComponent
import javax.swing.JTextArea

class CommandResultDialog(
    private val project: Project,
    private val result: CommandResult,
) : DialogWrapper(true) {

    init {
        title = "Command Result"
        setOKButtonText("OK")
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return if (result.isError()) {
            createErrorPanel(result.errorList)
        } else {
            createSuccessPanel(result.successList)
        }
    }

    private fun createSuccessPanel(successList: List<String>): JComponent {
        val successText = successList.joinToString("\n")
        return JTextArea(successText)
    }

    private fun createErrorPanel(errorList: List<String>): JComponent {
        val errorText = errorList.joinToString("\n")
        return JTextArea(errorText)
    }
}