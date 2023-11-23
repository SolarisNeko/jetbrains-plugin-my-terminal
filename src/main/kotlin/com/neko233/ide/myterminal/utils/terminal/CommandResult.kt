package com.neko233.ide.myterminal.utils.terminal

data class CommandResult(
    val isOk: Boolean,
    val successList: List<String>,
    val errorList: List<String>,
) {
    fun isError(): Boolean {
        return !isOk
    }
}
