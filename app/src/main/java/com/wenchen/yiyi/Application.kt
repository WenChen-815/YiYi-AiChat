package com.wenchen.yiyi

import android.app.Application
import android.content.res.Configuration
import com.wenchen.yiyi.core.database.AppDatabase
import com.wenchen.yiyi.core.log.GlobalLogcatTree
import com.wenchen.yiyi.core.log.LogRepository
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.storage.MMKVUtils
import com.wenchen.yiyi.core.util.toast.ToastUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    @Inject
    lateinit var userState: UserState
    @Inject
    lateinit var userConfigState : UserConfigState
    @Inject
    lateinit var injectAppDatabase: AppDatabase

    companion object {
        lateinit var instance: Application
        lateinit var appDatabase: AppDatabase
        lateinit var globalUserConfigState: UserConfigState
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        globalUserConfigState = userConfigState
        initToast()
        initLog()
        initMMKV()
        // 必须在MMKV初始化后调用
        userState.initialize{
            userConfigState.initialize()
        }

        //构建数据库
//        appDatabase = AppDatabase.getDatabase(instance.applicationContext)
        appDatabase = injectAppDatabase
    }

    /**
     * 初始化 Toast 框架
     */
    private fun initToast() {
        // 检测当前是否为深色模式
        val isDarkTheme = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // 初始化Toast，传递深色模式参数
        ToastUtils.init(this, isDarkTheme)
    }

    /**
     * 初始化 Log 框架
     */
    private fun initLog() {
//        if (BuildConfig.DEBUG) {
        Timber.plant(GlobalLogcatTree())
        initCrashHandler()
//        }
    }
    private fun initCrashHandler() {val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 1. 将异常信息记录到文件
            LogRepository.logThrowable(throwable)
            // 2. 依然让系统执行默认的崩溃操作（弹出“应用已停止”对话框）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 初始化 MMKV 框架
     */
    private fun initMMKV() {
        MMKVUtils.init(this)
    }

    /**
     * 应用配置变化时调用（如切换深色模式）
     *
     * @param newConfig 新的配置信息
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // 检测深色模式变化并更新Toast样式
        val isDarkTheme = newConfig.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // 根据当前主题重新设置Toast样式
        if (isDarkTheme) {
            ToastUtils.setWhiteStyle()
        } else {
            ToastUtils.setBlackStyle()
        }
    }
}
