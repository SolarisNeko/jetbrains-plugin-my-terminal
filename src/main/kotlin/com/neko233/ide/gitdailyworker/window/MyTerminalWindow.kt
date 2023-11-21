package com.neko233.ide.gitdailyworker.window

import com.alibaba.fastjson2.JSON
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.neko233.ide.gitdailyworker.myterminal.MyTerminalConstant
import com.neko233.ide.gitdailyworker.myterminal.MyTerminalData
import com.neko233.skilltree.commons.core.utils.CollectionUtils233
import com.neko233.skilltree.commons.core.utils.KvTemplate233
import com.neko233.skilltree.commons.core.utils.MapUtils233
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jdesktop.swingx.JXComboBox
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.swing.*

/**
 * 我的终端
 */
class MyTerminalWindow : ToolWindowFactory {

    private var osToNameToDataMap = HashMap<String, MutableMap<String, MyTerminalData>>()

    companion object {
        val LOG = Logger.getInstance(MyTerminalWindow::class.java)
    }

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val contentManager = toolWindow.contentManager

        // 创建一个内容面板
        val contentFactory = ContentFactory.getInstance()
        val newPanel = createPanel(project)
        val content = contentFactory.createContent(newPanel, "My Terminal", false)
        contentManager.addContent(content)
    }

    private fun createPanel(project: Project): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)

        // 第 1 部分
        val part1Panel = JPanel()
        part1Panel.layout = BoxLayout(part1Panel, BoxLayout.Y_AXIS)

        val configFileLabel = JLabel("Config File Path:")
        val configFileTextField = JTextField()
        configFileTextField.columns = 15
        val osLabel = JLabel("Select OS:")
        val osComboBox = JXComboBox(arrayOf("Windows", "Linux", "Mac"))
        val searchLabel = JLabel("Search:")
        val searchTextField = JTextField()
        searchTextField.columns = 10

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
        val part2Panel = JPanel()
        part2Panel.layout = BoxLayout(part2Panel, BoxLayout.Y_AXIS)
        val commandLabel = JLabel("Select Command:")

        // 读取配置文件的 cmdName List
        val osName = (osComboBox.selectedItem as String).lowercase()
        val data: MutableCollection<MyTerminalData> = osToNameToDataMap.getOrDefault(osName, MapUtils233.empty()).values
        val cmdNameList = ArrayList(data)
            .stream()
            .map { it.name }
            .toList()
        val selectItemScrollPanel = createScrollPaneForSelectItem(cmdNameList)
        // 添加选择监听器
        val selectCmdNameJList = (selectItemScrollPanel.viewport.view as JList<String>)


        part2Panel.add(commandLabel)
        part2Panel.add(selectItemScrollPanel)

        // 第 3 部分
        val part3Panel = JPanel(BorderLayout())
        part3Panel.layout = BoxLayout(part3Panel, BoxLayout.Y_AXIS)

        val cmdTemplateLabel = JLabel("Command Template:")
        val cmdTemplateTextArea = JTextArea("Template with \${key1} and \${key2}")

        val generateButton = JButton("Generate")
        val executeButton = JButton("Execute")

        val inputPanel = JPanel(GridLayout(0, 2))
        this.refreshInputCmdTemplate(cmdTemplateTextArea, inputPanel)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(generateButton)
        buttonPanel.add(executeButton)

        part3Panel.add(cmdTemplateLabel, BorderLayout.NORTH)
        part3Panel.add(cmdTemplateTextArea, BorderLayout.CENTER)
        part3Panel.add(inputPanel, BorderLayout.SOUTH)
        part3Panel.add(buttonPanel, BorderLayout.EAST)

        // 第 4 部分
        val part4Panel = JPanel(BorderLayout())
        part4Panel.layout = BoxLayout(part4Panel, BoxLayout.Y_AXIS)

        val consoleTextArea = JTextArea()
        val scrollPane = JScrollPane(consoleTextArea)

        val clearButton = JButton("Clear Console")
        clearButton.addActionListener { consoleTextArea.text = "" }

        part4Panel.add(scrollPane, BorderLayout.CENTER)
        part4Panel.add(clearButton, BorderLayout.EAST)

        // 添加到主面板
        panel.add(part1Panel, BorderLayout.EAST)
        panel.add(part2Panel, BorderLayout.CENTER)
        panel.add(part3Panel, BorderLayout.WEST)
        panel.add(part4Panel, BorderLayout.SOUTH)


        // 选择命令
        selectCmdNameJList.selectionModel?.addListSelectionListener {
            val cmdName = selectCmdNameJList.selectedValue

            val osName = (osComboBox.selectedItem as String).lowercase()
            val myTerminalData = osToNameToDataMap.getOrDefault(osName, MapUtils233.empty())
                .get(cmdName)
            if (myTerminalData == null) {
                return@addListSelectionListener
            }
            val cmdTemplate = myTerminalData.cmdTemplate

            cmdTemplateTextArea.text = cmdTemplate
            this.refreshInputCmdTemplate(cmdTemplateTextArea, inputPanel)
        }
        // 生成按钮
        generateButton.addActionListener {
            val kvMap = this.extractKvMapFromChildLabelField(inputPanel)

            val build = KvTemplate233(cmdTemplateTextArea.text)
                .put(kvMap)
                .build()

            consoleTextArea.text = build
        }
        // 生成默认配置按钮
        generateConfigButton.addActionListener {
            val basePath = project.basePath
            val terminalJsonFile = File(basePath, MyTerminalConstant.defaultFileName)

            if (!terminalJsonFile.exists()) {
                try {
                    terminalJsonFile.createNewFile()
                    // default content
                    val defaultData = MyTerminalData().apply {
                        this.os = "any"
                        this.name = "demo"
                        this.cmdTemplate = "echo \$\\{key} = \$\\{value}"
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
            val terminalJsonFile = File(basePath, MyTerminalConstant.defaultFileName)
            if (!terminalJsonFile.exists()) {
                return@addActionListener
            }
            // json 数据
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


            val hashMap = HashMap<String, MutableMap<String, MyTerminalData>>()
            myTerminalDataList.forEach {
                hashMap.computeIfAbsent(it.os.lowercase()) { _ ->
                    HashMap()
                }[it.name] = it
            }

            // update cache
            this.osToNameToDataMap = hashMap

            val osName = (osComboBox.selectedItem as String).lowercase()
            this.updateScrollPaneContent(selectCmdNameJList, osName)
        }


        return panel
    }


    /**
     * 刷新输入的 cmd 面板
     */
    private fun refreshInputCmdTemplate(
        cmdTemplateTextArea: JTextArea,
        inputPanel: JPanel,
    ) {
        val cmdTemplate = cmdTemplateTextArea.text

        // clear
        inputPanel.removeAll()

        // 添加 kv
        val keySet = extractKeysFromCmdTemplate(cmdTemplate)
        for (key in keySet) {
            val label = JLabel(key)
            val inputField = JTextField()
            inputPanel.add(label)
            inputPanel.add(inputField)
        }


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


    // Function to create scrollable column
    private fun createScrollPaneForSelectItem(options: List<String>): JScrollPane {
        val columnList = JBList(options)
        columnList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val selectItemScrollPanel = JBScrollPane(columnList)
        selectItemScrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        return selectItemScrollPanel
    }


    // 外部更新滚动窗格内容的方法
    private fun updateScrollPaneContent(
        columnList: JList<String>,
        osName: String,
    ) {
        val nameList = this.osToNameToDataMap.getOrDefault(osName, MapUtils233.empty()).values
            .stream()
            .map { it.name }
            .toList()

        // 获取列列表的模型
        val existingModel = columnList.model as DefaultListModel<String>

        // 清空模型
        existingModel.clear()

        // 将新选项添加到模型
        nameList.forEach { existingModel.addElement(it) }

        // 重新设置列列表的模型，以刷新滚动窗格
        columnList.model = existingModel
    }


    private fun extractKeysFromCmdTemplate(cmdTemplate: String): Set<String> {
        val regex = Regex("\\$\\{([a-zA-Z0-9_]+)}")
        return regex.findAll(cmdTemplate).map { it.groupValues[1] }.toSet()
    }

}
