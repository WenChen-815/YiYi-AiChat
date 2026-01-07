package com.wenchen.yiyi.core.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LogViewModel : ViewModel() {
    val logs = LogRepository.logEntries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun clearLogs() {
        LogRepository.clearLogs()
    }
}
