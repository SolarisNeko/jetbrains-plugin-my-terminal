package com.neko233.ide.myterminal.window

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class CustomDialog : DialogWrapper(true) {

    private val textField = JTextField(20)

    init {
        title = "Custom Dialog Title"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(textField, BorderLayout.CENTER)
        return panel
    }

    override fun createActions(): Array<Action> {
        val okAction = object : DialogWrapperAction("OK") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                // OK button logic here
                close(OK_EXIT_CODE)
            }
        }

        val cancelAction = object : DialogWrapperAction("Cancel") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                close(CANCEL_EXIT_CODE)
            }
        }

        return arrayOf(okAction, cancelAction)
    }

}
