package com.wenchen.yiyi.core.log

import timber.log.Timber

class GlobalLogcatTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        LogRepository.addLog(LogEntry(priority, tag, message))
    }
}
