package com.neko233.ide.myterminal.window

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.neko233.ide.myterminal.constant.MyTerminalConstant
import com.neko233.ide.myterminal.data.MyTerminalData
import com.neko233.ide.myterminal.dialog.CommandResultDialog
import com.neko233.ide.myterminal.utils.CopyUtils
import com.neko233.ide.myterminal.utils.TerminalUtils
import com.neko233.ide.myterminal.utils.terminal.CommandResult
import com.neko233.skilltree.commons.core.utils.CollectionUtils233
import com.neko233.skilltree.commons.core.utils.KvTemplate233
import com.neko233.skilltree.commons.core.utils.MapUtils233
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jdesktop.swingx.JXComboBox
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document

private const val HEIGHT_FONT = 100

private const val WIDTH_FRONT = 24

/**
 * 我的终端
 */
class MyTerminalWindow : ToolWindowFactory {

    // 基础
    lateinit var project: Project
    lateinit var toolWindow: ToolWindow

    // 选中的数据
    var curCmdName: String = ""
    var curOsName: String = ""

    // 终端输出面板
    var terminalOutputTextArea: JTextArea? = null
        private set

    // data
    private var osToNameToDataMap = HashMap<String, MutableMap<String, MyTerminalData>>()

    // 数据文件
    lateinit var dataFile: File

    // 可以选择的 cmd
    lateinit var selectItemScrollPanel: JBScrollPane
    lateinit var selectCmdNameJList: JBList<String>
    val curCmdDisplayList: MutableList<String> = mutableListOf()

    // 搜索框字段
    lateinit var osComboBox: JXComboBox
    lateinit var searchTextField: JTextField
    lateinit var selectItemOperateBtnPanel: JPanel

    // 数据文件
    var inputKvMap: MutableMap<JLabel, JTextField> = mutableMapOf()
        private set

    // 输入 cmd 面板
    lateinit var cmdKvArgsPanel: JPanel
        private set

    // hold button
    lateinit var generateCmdButton: JButton
        private set
    lateinit var saveCmdButton: JButton
        private set
    lateinit var cmdTemplateInputTextArea: JTextArea
        private set

    companion object {
        val LOG = Logger.getInstance(MyTerminalWindow::class.java)
    }

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        this.project = project
        this.toolWindow = toolWindow

        val basePath = project.basePath
        this.dataFile = File(basePath, MyTerminalConstant.defaultFileName)


        val contentManager = toolWindow.contentManager

        // 创建一个内容面板
        val contentFactory = ContentFactory.getInstance()
        val newPanel = createPanel(project)
        val content = contentFactory.createContent(newPanel, "My Terminal", false)
        contentManager.addContent(content)
    }

    private fun createPanel(project: Project): JPanel {

        // 字体输入大小
        val fontInputDimension = Dimension(HEIGHT_FONT, WIDTH_FRONT)


        val rootPanel = JPanel()
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.X_AXIS)

        // 第 1 部分
        val part1Panel = JPanel()
        part1Panel.preferredSize = Dimension(200, 80) // 设置输入框的大小
        part1Panel.layout = BoxLayout(part1Panel, BoxLayout.Y_AXIS)

        val configFileLabel = JLabel("Config File Path:")
        val configFileTextField = JTextField()
        configFileTextField.columns = 6
        // 设置输入框的大小
        configFileTextField.preferredSize = fontInputDimension
        configFileTextField.size = fontInputDimension

        val osLabel = JLabel("Select OS:")
        this.osComboBox = JXComboBox(arrayOf("Windows", "Linux", "Mac"))
        // 搜索框
        val searchLabel = JLabel("Search:")
        this.searchTextField = JTextField()
        searchTextField.preferredSize = fontInputDimension // 设置输入框的大小
        searchTextField.size = fontInputDimension // 设置输入框的大小

        searchTextField.columns = 6

        val generateConfigButton = JButton("Generate Config")


        val readAgainButton = JButton("Read Config")

        part1Panel.add(configFileLabel)
        part1Panel.add(configFileTextField)
        part1Panel.add(osLabel)
        part1Panel.add(osComboBox)
        part1Panel.add(searchLabel)
        part1Panel.add(searchTextField)
        part1Panel.add(generateConfigButton)
        part1Panel.add(readAgainButton)

        // 第 2 部分
        val part2Panel: JPanel = buildPanel2()

        // 第 3 部分
        val part3Panel = JPanel(BorderLayout())
        part3Panel.size = Dimension(100, 200)
        part3Panel.layout = BoxLayout(part3Panel, BoxLayout.Y_AXIS)

        // cmd Template
        val cmdTemplateTitle = JLabel("Command Template")
        this.cmdTemplateInputTextArea = JTextArea("echo \${key} = \${value}")
        cmdTemplateInputTextArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                // Code to execute when text is inserted
                handleTextAreaChange()
            }

            override fun removeUpdate(e: DocumentEvent) {
                // Code to execute when text is removed
                handleTextAreaChange()
            }

            override fun changedUpdate(e: DocumentEvent) {
                // Code to execute when text is changed (inserted or removed)
                handleTextAreaChange()
            }

            private fun handleTextAreaChange() {
                refreshCmdKvArgsPanel()
            }
        })

        // button
        this.saveCmdButton = JButton("Save")

        this.generateCmdButton = JButton("Generate")

        val executeButton = JButton("Execute")

        this.cmdKvArgsPanel = JPanel(GridLayout(0, 2))
        this.refreshCmdKvArgsPanel()

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(this.saveCmdButton)
        buttonPanel.add(this.generateCmdButton)
        buttonPanel.add(executeButton)

        part3Panel.add(cmdTemplateTitle, BorderLayout.CENTER)
        part3Panel.add(cmdTemplateInputTextArea, BorderLayout.CENTER)
        part3Panel.add(cmdKvArgsPanel, BorderLayout.SOUTH)
        part3Panel.add(buttonPanel, BorderLayout.EAST)

        // 第 4 部分
        val part4Panel = JPanel(BorderLayout())
        part4Panel.layout = BoxLayout(part4Panel, BoxLayout.Y_AXIS)

        // 终端输出
        this.terminalOutputTextArea = JBTextArea()
        this.terminalOutputTextArea!!.lineWrap = true
        this.terminalOutputTextArea!!.wrapStyleWord = true

        val scrollPane = JBScrollPane(terminalOutputTextArea)

        val panel4Title = JLabel("Terminal Output")
        val copyButton = JButton("Copy")
        val clearButton = JButton("Clear Console")
        clearButton.addActionListener { terminalOutputTextArea?.text = "" }

        part4Panel.add(panel4Title, BorderLayout.EAST)
        part4Panel.add(scrollPane, BorderLayout.CENTER)
        part4Panel.add(copyButton, BorderLayout.EAST)
        part4Panel.add(clearButton, BorderLayout.EAST)

        // 添加到主面板
        rootPanel.add(part1Panel, BorderLayout.EAST)
        rootPanel.add(part2Panel, BorderLayout.CENTER)
        rootPanel.add(part3Panel, BorderLayout.WEST)
        rootPanel.add(part4Panel, BorderLayout.SOUTH)

        copyButton.addActionListener {
            val content = terminalOutputTextArea?.text

            CopyUtils.copyToClipboard(content)
        }

        // 选择命令
        this.selectCmdNameJList.selectionModel?.addListSelectionListener {
            // 记录选中
            val cmdName = selectCmdNameJList.selectedValue
            if (cmdName == null) {
                return@addListSelectionListener
            }
            this.curCmdName = cmdName

            val myTerminalData = getCurrentChooseCmdData()
            if (myTerminalData == null) {
                return@addListSelectionListener
            }
            val cmdTemplate = myTerminalData.cmdTemplate

            cmdTemplateInputTextArea.text = cmdTemplate
            this.refreshCmdKvArgsPanel()
        }
        // 生成按钮
        generateCmdButton.addActionListener {
            val kvMap = this.extractKvMapFromChildLabelField(cmdKvArgsPanel)

            val build = KvTemplate233(cmdTemplateInputTextArea.text)
                .put(kvMap)
                .build()

            terminalOutputTextArea?.text = build
        }
        // 修改 executeButton 的监听器
        executeButton.addActionListener {
            val cmd = terminalOutputTextArea?.text ?: ""


            val result: CommandResult = TerminalUtils.executeCmdSyncReturnResult(cmd)

            dialogCmdResult(result)
        }

        // 生成默认配置按钮
        generateConfigButton.addActionListener {
            val terminalJsonFile = this.dataFile
            if (!terminalJsonFile.exists()) {
                try {
                    terminalJsonFile.createNewFile()
                    // default content
                    val defaultData = MyTerminalData().apply {
                        this.os = "windows"
                        this.name = "demo"
                        this.cmdTemplate = "echo $\\{key} = $\\{value}"
                    }
                    val defaultContent = JSON.toJSONString(mutableListOf(defaultData))

                    FileUtils.write(
                        terminalJsonFile,
                        defaultContent,
                        StandardCharsets.UTF_8,
                    )

                    // Set the path to the config file label
                    configFileTextField.text = terminalJsonFile.path
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                // File already exists, set its path to the config file label
                configFileTextField.text = terminalJsonFile.path
            }
        }

        // 重新读去配置
        readAgainButton.addActionListener {
            val basePath = project.basePath
            // 读取配置文件
            val terminalJsonFile = File(basePath, MyTerminalConstant.defaultFileName)
            if (!terminalJsonFile.exists()) {
                return@addActionListener
            }
            // 数据是 json[]
            val jsonArray = FileUtils.readFileToString(terminalJsonFile, StandardCharsets.UTF_8)

            val jsonList = JSON.parseArray(jsonArray, MyTerminalData::class.java)
            val myTerminalDataList = jsonList.stream()
                .filter {
                    if (StringUtils.isBlank(it.os)) {
                        return@filter false
                    }
                    if (StringUtils.isBlank(it.name)) {
                        return@filter false
                    }
                    return@filter true
                }
                .toList()
            if (CollectionUtils233.isEmpty(myTerminalDataList)) {
                return@addActionListener
            }

            // 转换
            val hashMap = HashMap<String, MutableMap<String, MyTerminalData>>()
            myTerminalDataList.forEach {
                // 小写
                hashMap.computeIfAbsent(it.os.lowercase()) { _ ->
                    HashMap()
                }[it.name] = it
            }

            // 内存数据
            this.osToNameToDataMap = hashMap

            val osName = (osComboBox.selectedItem as String).lowercase()
            this.curOsName = osName

            // 更新所有 cmd aliasName 选项
            this.updateScrollPanelView()
        }


        // 保存 click
        this.saveCmdButton.addActionListener {
            val curOsName = this.curOsName
            val curCmdName = this.curCmdName

            val content = this.cmdTemplateInputTextArea.text

            val defaultKvMap = mutableMapOf<String, String>()
            this.inputKvMap.forEach {
                val key = it.key.text.trim()
                val value = it.value.text.trim()
                defaultKvMap[key] = value
            }

            // save
            val newData = MyTerminalData().apply {
                this.os = curOsName
                this.name = curCmdName
                this.cmdTemplate = content
                this.defaultKvMap = defaultKvMap
            }

            osToNameToDataMap.computeIfAbsent(curOsName) { _ -> HashMap() }
                .put(curCmdName, newData)


            flushToJsonConfigFile()
        }

        // 搜索框过滤
        this.searchTextField.document.addDocumentListener(object : DocumentListener {

            val dataList = curCmdDisplayList;
            override fun insertUpdate(e: DocumentEvent) {
                filterList(e.document)
            }

            override fun removeUpdate(e: DocumentEvent) {
                filterList(e.document)
            }

            override fun changedUpdate(e: DocumentEvent) {
                // Plain text components do not fire these events
            }

            // 过滤
            private fun filterList(document: Document) {
                val text = document.getText(0, document.length).toLowerCase()

                // Filter the original list based on the text
                val filteredList = dataList.filter { it.lowercase().contains(text) }

                // Update the JList with the filtered list
                selectCmdNameJList.setListData(filteredList.toTypedArray())
            }
        })

        afterBind()

        // root
        return rootPanel
    }

    private fun afterBind() {
        // 系统选项框
        this.osComboBox.addActionListener {
            // 新系统
            val newOsName = (this.osComboBox.selectedItem as String).lowercase()
            this.curOsName = newOsName

            updateScrollPanelView()
        }

    }

    /**
     * 刷盘到 json 文件中
     */
    private fun flushToJsonConfigFile() {
        // to data list
        val dataList = osToNameToDataMap.values
            .stream()
            .map { it.values }
            .flatMap { it.stream() }
            .toList()
        val jsonPretty = JSON.toJSONString(dataList, JSONWriter.Feature.PrettyFormat)

        // 写入到文件
        FileUtils.write(this.dataFile, jsonPretty, StandardCharsets.UTF_8)
    }

    private fun buildPanel2(): JPanel {
        val part2Panel = JPanel()
        part2Panel.layout = BoxLayout(part2Panel, BoxLayout.Y_AXIS)
        val commandLabel = JLabel("Select Command:")

        // 读取配置文件的 cmdName List
        val osName = (this.osComboBox.selectedItem as String).lowercase()
        this.curOsName = osName

        val pair: Pair<JScrollPane, JPanel> = createScrollPaneForSelectItem()
        val selectItemScrollPanel = pair.first
        this.selectItemOperateBtnPanel = pair.second
        // 添加选择监听器
        this.selectCmdNameJList = (selectItemScrollPanel.viewport.view as JBList<String>)


        part2Panel.add(commandLabel)
        // 操作按钮
        part2Panel.add(selectItemOperateBtnPanel)
        // 选择
        part2Panel.add(selectItemScrollPanel)
        return part2Panel
    }

    private fun getCurrentChooseCmdData(
    ): MyTerminalData? {
        val osName = this.curOsName
        val cmdName = this.curCmdName

        val myTerminalData = osToNameToDataMap.getOrDefault(osName, MapUtils233.empty())
            .get(cmdName)
        return myTerminalData
    }

    private fun getCurrentDataList(
    ): ArrayList<MyTerminalData> {
        val osName = this.curOsName

        return ArrayList(osToNameToDataMap.getOrDefault(osName, MapUtils233.empty()).values)

    }


    /**
     * 刷新, 根据 CMD 推算 k-v 面板渲染
     */
    private fun refreshCmdKvArgsPanel() {
        val cmdTemplate = this.cmdTemplateInputTextArea.text

        // clear
        this.cmdKvArgsPanel.removeAll()

        val myTerminalData = osToNameToDataMap.getOrDefault(this.curOsName, emptyMap())
            .get(this.curCmdName)

        // 添加 kv
        val keySet = extractKeysFromCmdTemplate(cmdTemplate)
        val kvInputMap = HashMap<JLabel, JTextField>()
        val kvMap = HashMap<String, String>()
        for (key in keySet) {
            // key = value
            val labelKey = JLabel(key)
            val inputFieldValue = JTextField()
            kvInputMap.put(labelKey, inputFieldValue)

            // change listener
            inputFieldValue.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    // Schedule the button click after a delay
                    Timer(300) {
                        generateCmdButton.doClick()
                    }.apply {
                        isRepeats = false
                        start()
                    }
                }

                override fun removeUpdate(e: DocumentEvent?) {}

                override fun changedUpdate(e: DocumentEvent?) {}
            })

            this.cmdKvArgsPanel.add(labelKey)

            // 默认值
            if (myTerminalData != null) {
                val value = myTerminalData.defaultKvMap.getOrDefault(key, "").toString()
                inputFieldValue.text = value

                kvMap.put(key, value)
            }
            this.cmdKvArgsPanel.add(inputFieldValue)


            // click
        }
        this.inputKvMap = kvInputMap


        this.terminalOutputTextArea?.text = KvTemplate233.builder(cmdTemplate)
            .put(kvMap)
            .build()
    }

    /**
     * 从 panel 中提取子项 kv
     */
    private fun extractKvMapFromChildLabelField(inputPanel: JPanel): Map<String, String> {
        val kvMap = mutableMapOf<String, String>()

        // label + field = 2
        for (i in 0 until inputPanel.componentCount step 2) {
            val label = inputPanel.getComponent(i) as? JLabel
            val textField = inputPanel.getComponent(i + 1) as? JTextField

            label?.let {
                val key = it.text
                val value = textField?.text ?: ""
                kvMap[key] = value
            }
        }

        return kvMap
    }


    // Function to create scrollable column with Add/Remove buttons
    private fun createScrollPaneForSelectItem(): Pair<JScrollPane, JPanel> {
        val data: MutableCollection<MyTerminalData> = osToNameToDataMap.getOrDefault(
            this.curOsName, MapUtils233.empty()
        ).values
        val cmdNameList = ArrayList(data)
            .stream()
            .map { it.name }
            .toList()

        this.selectCmdNameJList = JBList(cmdNameList)
        this.selectCmdNameJList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // 滚动面板
        this.selectItemScrollPanel = JBScrollPane(this.selectCmdNameJList)
        selectItemScrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        val buttonPanel = JPanel()
        addHandlerForPanel2Comp(buttonPanel)

        return Pair(selectItemScrollPanel, buttonPanel)
    }

    private fun addHandlerForPanel2Comp(
        buttonPanel: JPanel,
    ) {

        val addButton = JButton("Add")
        val removeButton = JButton("Remove")
        val modifyNameButton = JButton("Modify Name")


        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(modifyNameButton)


        // add
        addButton.addActionListener {
            val newName = JOptionPane.showInputDialog("Enter a new name:")
            if (newName != null && newName.isNotBlank()) {
                addCmdData(newName)
                updateScrollPanelView()
            }
        }

        // remove
        removeButton.addActionListener {
            removeCmdData(this.curCmdName)
            updateScrollPanelView()
        }

        // modify
        modifyNameButton.addActionListener {
            val selectedIndex = this.selectCmdNameJList.selectedIndex
            if (selectedIndex != -1) {
                val currentName = this.curCmdDisplayList.getOrNull(selectedIndex)
                    ?: return@addActionListener
                val modifiedName = JOptionPane.showInputDialog("Modify name:", currentName)

                updateCmdData(currentName, modifiedName)
                updateScrollPanelView()

            }
        }

    }


    /**
     * 更新 cmd 数据
     */
    private fun updateCmdData(
        currentName: String,
        modifiedName: String,
    ) {
        // default content
        val osName = this.curOsName

        val data = this.osToNameToDataMap.computeIfAbsent(osName) { _ -> HashMap() }
            .remove(currentName)
            ?: return
        data.name = modifiedName

        addCmdData(data.name, data)

        flushToJsonConfigFile()
    }

    private fun removeCmdData(curCmdName: String) {
        // default content
        val osName = this.curOsName

        this.osToNameToDataMap.computeIfAbsent(osName) { _ -> HashMap() }
            .remove(curCmdName)

        flushToJsonConfigFile()
    }

    private fun addCmdData(newName: String) {
        // default content
        val osName = this.curOsName
        val newData = MyTerminalData().apply {
            this.os = osName
            this.name = newName
            this.cmdTemplate = ""
        }

        this.osToNameToDataMap.computeIfAbsent(osName) { _ -> HashMap() }
            .put(newName, newData)
    }

    private fun addCmdData(
        newName: String,
        data: MyTerminalData,
    ) {
        // default content
        val osName = this.curOsName

        this.osToNameToDataMap.computeIfAbsent(osName) { _ -> HashMap() }
            .put(newName, data)

        flushToJsonConfigFile()
    }


    /**
     * 更新当前选项 Panel 内容
     */
    private fun updateScrollPanelView() {

        val cmdAliasList: MutableList<String> = mutableListOf()
        // 全部
        val dataList = getCurrentDataList()
        val cmdNameList = dataList.stream()
            .map { it.name }
            .toList()
        cmdAliasList.addAll(cmdNameList)

        this.curCmdDisplayList.clear()
        this.curCmdDisplayList.addAll(cmdAliasList)

        val existingModel = this.selectCmdNameJList.model as DefaultListModel<String>
        existingModel.removeAllElements()
        existingModel.addAll(cmdAliasList)
    }


    private fun extractKeysFromCmdTemplate(cmdTemplate: String): Set<String> {
        val regex = Regex("\\$\\{([a-zA-Z0-9_]+)}")
        return regex.findAll(cmdTemplate).map { it.groupValues[1] }.toSet()
    }


    // 处理命令结果的函数
    private fun dialogCmdResult(result: CommandResult) {
        // 在这里处理命令结果，例如创建 CommandResultDialog 并显示
        val dialog = CommandResultDialog(project, result)
        dialog.show()
    }
}
