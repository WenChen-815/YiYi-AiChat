package com.wenchen.yiyi.core.designSystem.theme

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

/**
 * 深色主题配色方案
 * 定义MaterialTheme中深色模式下的各种颜色
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = TextWhite,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextWhite,
    inversePrimary = PrimaryLight,
    secondary = ColorPurpleDark,
    onSecondary = TextWhite,
    secondaryContainer = ColorPurpleDark,
    onSecondaryContainer = TextWhite,
    tertiary = ColorSuccessDark,
    onTertiary = TextWhite,
    tertiaryContainer = ColorSuccessDark,
    onTertiaryContainer = TextWhite,
    background = BgGreyDark,
    onBackground = TextPrimaryDark,
    surface = BgWhiteDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = BgContentDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = PrimaryDark,
    inverseSurface = BgGreyLight,
    inverseOnSurface = TextPrimaryLight,
    error = ColorDangerDark,
    onError = TextWhite,
    errorContainer = BgRedDark,
    onErrorContainer = ColorDangerDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    scrim = MaskDark,
    surfaceBright = BgWhiteDark,
    surfaceContainer = BgWhiteDark,
    surfaceContainerHigh = BgWhiteDark,
    surfaceContainerHighest = BgWhiteDark,
    surfaceContainerLow = BgContentDark,
    surfaceContainerLowest = BgGreyDark,
    surfaceDim = BgGreyDark,
    primaryFixed = PrimaryDark,
    primaryFixedDim = PrimaryDark,
    onPrimaryFixed = TextWhite,
    onPrimaryFixedVariant = TextWhite,
    secondaryFixed = ColorPurpleDark,
    secondaryFixedDim = ColorPurpleDark,
    onSecondaryFixed = TextWhite,
    onSecondaryFixedVariant = TextWhite,
    tertiaryFixed = ColorSuccessDark,
    tertiaryFixedDim = ColorSuccessDark,
    onTertiaryFixed = TextWhite,
    onTertiaryFixedVariant = TextWhite
)

/**
 * 浅色主题配色方案
 * 定义MaterialTheme中浅色模式下的各种颜色
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = TextWhite,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextWhite,
    inversePrimary = PrimaryDark,
    secondary = ColorPurple,
    onSecondary = TextWhite,
    secondaryContainer = ColorPurple,
    onSecondaryContainer = TextWhite,
    tertiary = ColorSuccess,
    onTertiary = TextWhite,
    tertiaryContainer = ColorSuccess,
    onTertiaryContainer = TextWhite,
    background = BgWhiteLight, // BgGreyLight
    onBackground = TextPrimaryLight,
    surface = BgWhiteLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BgContentLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = PrimaryLight,
    inverseSurface = BgGreyDark,
    inverseOnSurface = TextPrimaryDark,
    error = ColorDanger,
    onError = TextWhite,
    errorContainer = BgRedLight,
    onErrorContainer = ColorDanger,
    outline = BorderLight,
    outlineVariant = BorderLight,
    scrim = MaskLight,
    surfaceBright = BgWhiteLight,
    surfaceContainer = BgWhiteLight,
    surfaceContainerHigh = BgWhiteLight,
    surfaceContainerHighest = BgWhiteLight,
    surfaceContainerLow = BgContentLight,
    surfaceContainerLowest = BgGreyLight,
    surfaceDim = BgGreyLight,
    primaryFixed = PrimaryLight,
    primaryFixedDim = PrimaryLight,
    onPrimaryFixed = TextWhite,
    onPrimaryFixedVariant = TextWhite,
    secondaryFixed = ColorPurple,
    secondaryFixedDim = ColorPurple,
    onSecondaryFixed = TextWhite,
    onSecondaryFixedVariant = TextWhite,
    tertiaryFixed = ColorSuccess,
    tertiaryFixedDim = ColorSuccess,
    onTertiaryFixed = TextWhite,
    onTertiaryFixedVariant = TextWhite
)

/**
 * 应用主题 Composable 函数
 * 根据系统设置决定使用深色或浅色主题，并应用所有设计系统元素
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param dynamicColor 是否使用动态颜色（Android 12+特性），默认关闭
 * @param content 需要应用主题的内容
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}