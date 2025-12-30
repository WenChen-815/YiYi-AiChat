package com.wenchen.yiyi.core.common.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/*
    主要颜色 (Primary Colors)
        primary: 主要品牌色，用于重要组件和交互元素
        onPrimary: 在 primary 颜色上显示的文本和图标颜色
        primaryContainer: 主要颜色的容器背景色
        onPrimaryContainer: 在 primaryContainer 上显示的内容颜色
        inversePrimary: 反转模式下的主要颜色（深色主题中使用）
    次要颜色 (Secondary Colors)
        secondary: 次要品牌色，用于次要操作和元素
        onSecondary: 在 secondary 颜色上显示的文本和图标颜色
        secondaryContainer: 次要颜色的容器背景色
        onSecondaryContainer: 在 secondaryContainer 上显示的内容颜色
    第三颜色 (Tertiary Colors)
        tertiary: 第三品牌色，用于装饰性元素
        onTertiary: 在 tertiary 颜色上显示的文本和图标颜色
        tertiaryContainer: 第三颜色的容器背景色
        onTertiaryContainer: 在 tertiaryContainer 上显示的内容颜色
    背景和表面颜色
        background: 应用程序的主要背景色
        onBackground: 在 background 上显示的文本和图标颜色
        surface: UI 组件的表面颜色（卡片、对话框等）
        onSurface: 在 surface 上显示的文本和图标颜色
        surfaceVariant: 变体表面颜色，用于区分不同层次
        onSurfaceVariant: 在 surfaceVariant 上显示的内容颜色
    特殊用途颜色
        error: 错误状态颜色
        onError: 在 error 颜色上显示的文本和图标颜色
        errorContainer: 错误状态的容器背景色
        onErrorContainer: 在 errorContainer 上显示的内容颜色
        outline: 边框和分隔线颜色
        outlineVariant: 变体边框颜色
    反转和特殊效果颜色
        inverseSurface: 反转模式下的表面颜色
        inverseOnSurface: 在 inverseSurface 上显示的内容颜色
        scrim: 浮层遮罩颜色（半透明覆盖层）
 */
private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = Pink,
    background = BlackBg,
)

private val LightColorScheme = lightColorScheme(
    primary = Gold,
    secondary = Pink,
    background = WhiteBg,
)

@Composable
fun AIChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // 在动态颜色基础上进行自定义覆盖
            dynamicScheme.copy(
                primary = Gold,
                secondary = Pink,
                background = if (darkTheme) BlackBg else WhiteBg,
                onBackground = if (darkTheme) WhiteText else BlackText,
            )
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}