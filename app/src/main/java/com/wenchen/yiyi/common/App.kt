package com.wenchen.yiyi.common

import android.app.Application
import com.wenchen.yiyi.common.database.AppDatabase

class App : Application() {
    companion object {
        lateinit var instance: App
        lateinit var appDatabase: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        //构建数据库
        appDatabase = AppDatabase.getDatabase(instance.applicationContext)
    }
}