package com.wenchen.yiyi.feature.output.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.core.designSystem.theme.GrayBg
import com.wenchen.yiyi.core.designSystem.theme.WhiteText
import com.wenchen.yiyi.core.database.entity.AICharacter

/**
 * 角色卡片
 * @param character 角色
 * @param avatarBitmap? 角色头像
 * @param backgroundBitmap? 角色背景
 * @param width 宽度(默认150dp)
 * @param height 高度(默认225dp)
 */
@Composable
fun OutputCharacterView(
    character: AICharacter,
    avatarBitmap: Bitmap? = null,
    backgroundBitmap: Bitmap? = null,
    width: Int = 150,
    height: Int = 225,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        modifier = Modifier
            .width(width.dp)
            .height(height.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (LocalInspectionMode.current) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GrayBg)
                        .clip(RoundedCornerShape(8.dp, 8.dp, 9.dp, 9.dp))
                )
            } else if (backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap.asImageBitmap(),
                    contentDescription = "角色背景",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp, 8.dp, 9.dp, 9.dp)),
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                GrayBg.copy(alpha = 0.9f),
                                GrayBg,
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )
            
            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp, 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            if (LocalInspectionMode.current) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            } else if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = "角色头像",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.bodyMedium.copy(color = WhiteText),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
