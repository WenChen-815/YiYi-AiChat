package com.wenchen.yiyi.core.log

data class LogEntry(
    val priority: Int,
    val tag: String?,
    val message: String
)
