package com.wenchen.yiyi.core.log

import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber

class GlobalLogcatTree : Timber.DebugTree() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        LogRepository.addLog(LogEntry(priority, tag, message))
    }
}
