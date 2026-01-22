package com.wenchen.yiyi.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle

/**
 * 图标下方带文本的组件
 *
 * @param icon 图标资源
 * @param text 显示的文本
 * @param iconModifier 图标修饰符
 * @param textModifier 文本修饰符
 * @param textStyle 文本样式
 */
@Composable
fun IconWithText(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    iconTint: Color = LocalContentColor.current,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = iconModifier
        )

        Text(
            text = text,
            style = textStyle,
            modifier = textModifier
        )
    }
}
