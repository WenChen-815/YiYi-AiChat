package com.wenchen.yiyi.common.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object StatusBarUtil {
    fun transparentNavBar(activity: Activity) {
        val window = activity.window
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (window.attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION == 0) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
        }

        val decorView = window.decorView
        val vis = decorView.systemUiVisibility
        val option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.systemUiVisibility = vis or option
    }

    fun transparentStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        var systemUiVisibility = window.decorView.systemUiVisibility
        systemUiVisibility =
            systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = systemUiVisibility
        window.statusBarColor = android.graphics.Color.TRANSPARENT

    }

    // ===========Compose相关===========
    /**
     * 在Activity中直接设置状态栏文字颜色
     * @param activity 当前Activity
     * @param isDarkText 是否设置为深色文字（true: 深色文字，适合浅色背景; false: 浅色文字，适合深色背景）
     * @param statusBarColor 状态栏背景颜色，默认透明
     */
    fun setStatusBarTextColor(
        activity: ComponentActivity,
        isDarkText: Boolean,
        statusBarColor: Color = Color.Transparent
    ) {
        val window = activity.window
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // 设置状态栏文字颜色模式
        windowInsetsController.isAppearanceLightStatusBars = isDarkText

        // 设置状态栏背景色
        window.statusBarColor = statusBarColor.toArgb()
    }

    /**
     * 在Compose组件中设置状态栏文字颜色
     * @param isDarkText 是否设置为深色文字
     * @param statusBarColor 状态栏背景颜色
     */
    @Composable
    fun ConfigureStatusBar(
        isDarkText: Boolean,
        statusBarColor: Color = Color.Black
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                val windowInsetsController = WindowCompat.getInsetsController(window, view)

                // 设置状态栏文字颜色模式
                windowInsetsController.isAppearanceLightStatusBars = isDarkText

                // 设置状态栏背景色
                window.statusBarColor = statusBarColor.toArgb()
            }
        }
    }
}