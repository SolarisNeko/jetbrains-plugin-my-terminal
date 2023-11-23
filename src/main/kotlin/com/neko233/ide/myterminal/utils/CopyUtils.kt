package com.neko233.ide.myterminal.utils

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-24
 * */
object CopyUtils {
    @JvmStatic

    fun copyToClipboard(content: String?) {
        val stringSelection = StringSelection(content)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

}