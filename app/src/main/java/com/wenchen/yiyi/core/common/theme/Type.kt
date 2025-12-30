package com.wenchen.yiyi.core.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
    1. Display 系列（大标题，用于页面最醒目位置，如封面标题）
        样式	fontSize	fontWeight	lineHeight	letterSpacing
        displayLarge	57sp	FontWeight.Light（300）	64sp	-0.25sp
        displayMedium	45sp	FontWeight.Light（300）	52sp	0sp
        displaySmall	36sp	FontWeight.Normal（400）	44sp	0sp
    2. Headline 系列（标题，用于页面或大区块的主标题）
        样式	fontSize	fontWeight	lineHeight	letterSpacing
        headlineLarge	32sp	FontWeight.Normal（400）	40sp	0sp
        headlineMedium	28sp	FontWeight.Normal（400）	36sp	0sp
        headlineSmall	24sp	FontWeight.Normal（400）	32sp	0sp
    3. Title 系列（小标题，用于卡片、列表、弹窗等的标题）
        样式	fontSize	fontWeight	lineHeight	letterSpacing
        titleLarge	22sp	FontWeight.Normal（400）	28sp	0sp
        titleMedium	16sp	FontWeight.Medium（500）	24sp	0.15sp	（通常带半粗体）
        titleSmall	14sp	FontWeight.Medium（500）	20sp	0.1sp	（通常带半粗体）
    4. Body 系列（正文，用于主要内容文本，可读性优先）
        样式	fontSize	fontWeight	lineHeight	letterSpacing
        bodyLarge	16sp	FontWeight.Normal（400）	24sp	0.5sp	（默认正文样式）
        bodyMedium	14sp	FontWeight.Normal（400）	20sp	0.25sp	（次要正文）
        bodySmall	12sp	FontWeight.Normal（400）	16sp	0.4sp	（辅助文本，如注释）
    5. Label 系列（标签，用于按钮、输入框提示、小标签等简短文本）
        样式	fontSize	fontWeight	lineHeight	letterSpacing
        labelLarge	14sp	FontWeight.Medium（500）	20sp	0.1sp	（如按钮文本、大标签）
        labelMedium	12sp	FontWeight.Medium（500）	16sp	0.5sp	（中等标签）
        labelSmall	11sp	FontWeight.Medium（500）	16sp	0.5sp	（小标签，如输入框提示）
 */
val Typography = Typography(
    // headlineLarge	32sp	FontWeight.Normal（400）	40sp	0sp
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // headlineMedium	28sp	FontWeight.Normal（400）	36sp	0sp
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    // headlineSmall	24sp	FontWeight.Normal（400）	32sp	0sp
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // titleLarge	22sp	FontWeight.Normal（400）	28sp	0sp
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    //        titleMedium	16sp	FontWeight.Medium（500）	24sp	0.15sp	（通常带半粗体）
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    //        titleSmall	14sp	FontWeight.Medium（500）	20sp	0.1sp	（通常带半粗体）
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // bodyLarge	16sp	FontWeight.Normal（400）	24sp	0.5sp	（默认正文样式）
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // bodyMedium	14sp	FontWeight.Normal（400）	20sp	0.25sp	（次要正文）
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // bodySmall	12sp	FontWeight.Normal（400）	16sp	0.4sp	（辅助文本，如注释）
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)