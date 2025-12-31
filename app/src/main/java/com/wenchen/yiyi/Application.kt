package com.wenchen.yiyi

import android.app.Application
import com.wenchen.yiyi.core.common.database.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class Application : Application() {
    companion object {
        lateinit var instance: Application
        lateinit var appDatabase: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        initLog()
        instance = this
        //构建数据库
        appDatabase = AppDatabase.getDatabase(instance.applicationContext)
    }
}

/**
 * 初始化 Log 框架
 */
private fun initLog() {
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
}