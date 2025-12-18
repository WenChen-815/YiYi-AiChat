package com.wenchen.yiyi.common.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.common.theme.WhiteText

@Composable
fun BlinkingReplyIndicator(text: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = text,
        color = WhiteText.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(HalfTransparentBlack).padding(4.dp)
    )
}