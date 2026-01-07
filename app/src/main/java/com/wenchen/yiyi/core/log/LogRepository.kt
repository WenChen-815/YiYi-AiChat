package com.wenchen.yiyi.core.log

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LogRepository {
    private const val MAX_LOG_COUNT = 200

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLog(logEntry: LogEntry) {
        val currentLogs = _logEntries.value.toMutableList()
        currentLogs.add(0, logEntry)
        if (currentLogs.size > MAX_LOG_COUNT) {
            currentLogs.removeLast()
        }
        _logEntries.value = currentLogs
    }

    fun clearLogs() {
        _logEntries.value = emptyList()
    }
}
