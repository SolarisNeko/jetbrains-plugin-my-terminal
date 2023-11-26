package com.neko233.ide.myterminal.utils

import com.neko233.ide.myterminal.utils.terminal.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-11
 * */
object TerminalUtils {


    fun splitCommand(command: String): List<String> {
        val regex = Regex("""[^\s"]+|"([^"]*)"""")

        return regex.findAll(command).map { it.groupValues[0] }.toList()
    }

    /**
     * @param command 单个命令
     * @return 封装的命令执行结果
     */
    fun executeCmdSyncReturnResult(
        command: String,
        targetDirPath: String = "",
    ): CommandResult {
        try {
            val cmdArgs = splitCommand(command)
            val processBuilder = ProcessBuilder(
                cmdArgs
            )
                .redirectErrorStream(true)
            if (StringUtils.isNotBlank(targetDirPath)) {
                val file = File(targetDirPath.trim())
                if (file.exists()) {
                    processBuilder.directory(file)
                }
            }

            val process = processBuilder.start()

            val inputStream = process.inputStream
            val errorStream = process.errorStream

            val successList = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()
            val errorList = BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8)).readLines()

            val exitCode = process.waitFor()
            val isOk = exitCode == 0

            return CommandResult(isOk, successList, errorList)
        } catch (e: IOException) {
            e.printStackTrace()
            return CommandResult(false, emptyList(), listOf("IOException: ${e.message}"))
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return CommandResult(false, emptyList(), listOf("InterruptedException: ${e.message}"))
        }
    }


    /**
     * @param command 单个命令
     * @return 封装的命令执行结果
     */
    suspend fun executeCmdAsyncReturnResult(
        command: String,
        targetDirPath: String = ""
    ): CommandResult = runBlocking {
        try {
            val cmdArgs = splitCommand(command)
            val processBuilder = ProcessBuilder(cmdArgs)
                .redirectErrorStream(true)

            if (targetDirPath.isNotBlank()) {
                val file = File(targetDirPath.trim())
                if (file.exists()) {
                    processBuilder.directory(file)
                }
            }

            val process = processBuilder.start()

            val inputStream = process.inputStream
            val errorStream = process.errorStream

            // Use async to read output and error streams concurrently
            val successDeferred = async(Dispatchers.IO) {
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()
            }

            val errorDeferred = async(Dispatchers.IO) {
                BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8)).readLines()
            }

            val successList = successDeferred.await()
            val errorList = errorDeferred.await()

            val exitCode = process.waitFor()
            val isOk = exitCode == 0

            CommandResult(isOk, successList, errorList)
        } catch (e: IOException) {
            e.printStackTrace()
            CommandResult(false, emptyList(), listOf("IOException: ${e.message}"))
        } catch (e: InterruptedException) {
            e.printStackTrace()
            CommandResult(false, emptyList(), listOf("InterruptedException: ${e.message}"))
        }
    }

    /**
     * @param command 单个命令
     * @return 响应的每一行
     */
    fun executeCommandSync(command: String?): List<String> {

        val commandCallbackList: List<String>
        commandCallbackList = try {
            val exec = Runtime.getRuntime().exec(command)
            // block
            val inputStream = exec.inputStream
            Optional.of(IOUtils.readLines(inputStream, StandardCharsets.UTF_8))
                .orElse(ArrayList())
        } catch (e: IOException) {
            return ArrayList()
        }
        return commandCallbackList
    }

    fun executeCommandToOneLineSync(command: String?): String {
        return Optional.ofNullable(executeCommandSync(command))
            .orElse(ArrayList())
            .stream()
            .collect(Collectors.joining(System.lineSeparator()))
    }

    fun executeCommandAsync(command: String?) {
        try {
            val exec = Runtime.getRuntime().exec(command)
        } catch (e: IOException) {
        }
    }
}

