package com.wenchen.yiyi.core.designSystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体规范
 * 1px = 0.5sp 转换结果，未提供的尺寸按层级递减补齐
 */
val Typography = Typography(

    /**
     * 中粗体 · 22sp / 31sp
     * 使用场景：超大标题，文章标题
     */
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 中粗体
        fontSize = 22.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中粗体 · 18sp / 27sp
     * 使用场景：大标题
     */
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 中粗体
        fontSize = 18.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中粗体 · 16sp / 24sp
     * 使用场景：展示级文案（中等长度）
     */
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 16sp / 24sp
     * 使用场景：二级标题、导航栏、列表、段落标题、按钮文字
     */
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 中黑体
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 13sp / 20sp
     * 使用场景：信息分组小标题
     */
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 14sp / 22sp
     * 使用场景：类别名称
     */
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // 中黑体
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 17sp / 20sp
     * 使用场景：模块标题、弹窗标题
     */
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 15sp / 20sp
     * 使用场景：列表项标题、辅助性标题
     */
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 13sp / 18sp
     * 使用场景：段落内小标题、二级描述
     */
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 常规体 · 15sp / 22sp
     * 使用场景：正文内容、段落文字
     */
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 常规体
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 常规体 · 13sp / 18sp
     * 使用场景：底部导航栏文字、辅助性文字、标签文字
     */
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 常规体
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 常规体 · 12sp / 16sp
     * 使用场景：次级正文、辅助段落
     */
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),

    /**
     * 中黑体 · 12sp / 16sp
     * 使用场景：按钮、标签等操作文字
     */
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),

    /**
     * 中黑体 · 11sp / 16sp
     * 使用场景：辅助标签、徽标说明
     */
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),

    /**
     * 中黑体 · 10sp / 14sp
     * 使用场景：最小标签、角标
     */
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    )
)
