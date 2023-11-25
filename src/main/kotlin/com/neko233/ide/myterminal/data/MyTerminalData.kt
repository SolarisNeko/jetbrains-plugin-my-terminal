package com.neko233.ide.myterminal.data

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-21
 * */
class MyTerminalData {

    /**
     * 匹配的系统
     */
    var os: String = "windows"

    /**
     * 命令的代表名字
     */
    var name: String = ""

    /**
     * 命令模板
     * 例如: wget ${url}
     */
    var cmdTemplate: String = ""


    /**
     * 模板值
     */
    var defaultKvMap: Map<String, Any> = HashMap()

}