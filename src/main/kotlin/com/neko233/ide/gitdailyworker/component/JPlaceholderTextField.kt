package com.neko233.ide.gitdailyworker.component

import java.awt.Color
import java.awt.Graphics
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField

class JPlaceholderTextField(private val placeholder: String) : JTextField() {

    init {
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                repaint()
            }

            override fun focusLost(e: FocusEvent?) {
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (text.isEmpty() && !isFocusOwner) {
            val textColor = foreground
            val placeholderColor = Color.GRAY

            g.color = placeholderColor
            g.drawString(placeholder, insets.left, g.fontMetrics.maxAscent + insets.top)
            g.color = textColor
        }
    }
}
