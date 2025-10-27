package com.wenchen.yiyi.aiChat.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wenchen.yiyi.aiChat.vm.ChatUiState
import com.wenchen.yiyi.common.theme.HalfTransparentBlack
import com.wenchen.yiyi.common.theme.WhiteText

@Composable
fun ChatActivityTopBar(
    onOpenDrawer: () -> Unit,
    uiState: ChatUiState,
    isScrolling: Boolean = false,
    isScrollEnd: Boolean = true
) {
    // 定义动画状态
    val titleOffsetX by animateFloatAsState(
        targetValue = if (isScrolling) -100f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )

    val iconOffsetX by animateFloatAsState(
        targetValue = if (isScrolling) 100f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "iconOffset"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (isScrolling) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (isScrolling) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "iconAlpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color.Transparent)
            .padding(start = 10.dp, top = 4.dp, bottom = 6.dp)
            .padding(top = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 标题部分
        Row(
            modifier = Modifier
                .offset(x = titleOffsetX.dp)
                .alpha(titleAlpha)
                .clip(RoundedCornerShape(24.dp))
                .background(HalfTransparentBlack.copy(alpha = 0.8f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = uiState.currentCharacter?.avatarPath,
                contentScale = ContentScale.Crop,
                contentDescription = "角色头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = if (uiState.isAiReplying) "对方输入中..."
                    else uiState.currentCharacter?.name ?: "未选择角色",
                    style = MaterialTheme.typography.titleMedium.copy(color = WhiteText),
                )
            }

        }


        // 动作按钮部分
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = "打开抽屉",
            tint = WhiteText.copy(alpha = 0.8f),
            modifier = Modifier
                .offset(x = iconOffsetX.dp)
                .alpha(iconAlpha)
                .clip(RoundedCornerShape(13.dp, 0.dp, 0.dp, 13.dp))
                .background(HalfTransparentBlack)
                .padding(start = 10.dp, end = 24.dp, top = 4.dp, bottom = 4.dp)
                .clickable { onOpenDrawer() }
        )
    }
}