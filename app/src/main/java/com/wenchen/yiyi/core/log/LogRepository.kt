package com.wenchen.yiyi.core.log

import android.os.Build
import androidx.annotation.RequiresApi
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.util.storage.FilesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogRepository {
    private const val MAX_LOG_COUNT = 200
    private const val LOG_FILE_NAME = "app_logs.txt"
    private const val BACKUP_FILE_NAME = "$LOG_FILE_NAME.old"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 限制为 5MB
    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSS",
        Locale.getDefault()
    )
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()

    // 用于后台写入文件的协程作用域
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLog(logEntry: LogEntry) {
        // 更新内存中的日志列表（用于 UI 显示）
        val currentLogs = _logEntries.value.toMutableList()
        currentLogs.add(0, logEntry)
        if (currentLogs.size > MAX_LOG_COUNT) {
            currentLogs.removeLast()
        }
        _logEntries.value = currentLogs

        // 将日志持久化到文件
        // 2. 异步将日志持久化到文件（IO 线程）
        ioScope.launch {
            saveLogToFile(logEntry)
        }
    }
    /**
     * 将日志项格式化并追加到文件中
     */
    private fun saveLogToFile(logEntry: LogEntry) {
        checkAndRotateFile()

        val timestamp = dateFormat.format(Date())
        val priorityChar = when (logEntry.priority) {
            2 -> "V" // VERBOSE
            3 -> "D" // DEBUG
            4 -> "I" // INFO
            5 -> "W" // WARN
            6 -> "E" // ERROR
            7 -> "A" // ASSERT
            else -> "?"
        }
        val logLine = "$timestamp $priorityChar/${logEntry.tag ?: "Unknown"}: ${logEntry.message}"

        // 使用 FilesUtil 追加到文件
        FilesUtils.appendToFile(logLine, LOG_FILE_NAME)
    }

    fun logThrowable(t: Throwable) {
        val stackTrace = android.util.Log.getStackTraceString(t)
        // 手动构造一个 ERROR 级别的 LogEntry 并保存
        addLog(LogEntry(android.util.Log.ERROR, "CrashReport", stackTrace))
    }
    /**
    * 导出日志文件到公共下载目录
    */
    fun exportLogs(): Boolean {
        // 导出当前日志
        val currentResult = FilesUtils.exportLogFile(LOG_FILE_NAME, "YiYi")

        // 检查并导出备份日志（如果存在）
        val backupFile = File(Application.instance.filesDir, BACKUP_FILE_NAME)
        val backupResult = if (backupFile.exists()) {
            FilesUtils.exportLogFile(BACKUP_FILE_NAME, "YiYi")
        } else {
            true // 不存在备份文件视作“导出成功”
        }

        return currentResult && backupResult    }
    fun clearLogs() {
        _logEntries.value = emptyList()
    }

    /**
     * 检查日志文件大小，如果超过限制则重命名为备份文件
     */
    private fun checkAndRotateFile() {
        try {
            val context = Application.instance.applicationContext
            val file = File(context.filesDir, LOG_FILE_NAME)

            if (file.exists() && file.length() > MAX_FILE_SIZE) {
                val backupFile = File(context.filesDir, "$LOG_FILE_NAME.old")
                // 如果已有备份则删除，将当前日志转为备份
                if (backupFile.exists()) backupFile.delete()
                file.renameTo(backupFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
