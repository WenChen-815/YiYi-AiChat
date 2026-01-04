package com.wenchen.yiyi.feature.main.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.wenchen.yiyi.core.designSystem.component.NoBorderTextField
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.common.theme.BlackText
import com.wenchen.yiyi.core.common.theme.GrayBg
import com.wenchen.yiyi.core.common.theme.GrayText
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.feature.main.viewmodel.HomeViewModel
import com.wenchen.yiyi.navigation.routes.AiChatRoutes
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    context: Context,
    viewModel: HomeViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    var searchQuery by remember { mutableStateOf("") }
    // 从ViewModel获取角色列表数据
    val characters by viewModel.characters.collectAsState()
    // 控制删除确认对话框的显示状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    // 保存待删除的角色
    var characterToDelete by remember { mutableStateOf<AICharacter?>(null) }

    // 添加生命周期观察者，当界面恢复时刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // 每次回到主页时重新加载数据
                    viewModel.refreshCharacters()
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(10.dp),
    ) {
        // 搜索框
        NoBorderTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                viewModel.searchCharacters(query)
            },
            modifier = Modifier.padding(bottom = 16.dp),
            placeholder = {
                Text(
                    "搜索角色",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GrayText),
                )
            },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BlackText),
        )

        // 角色列表
        if (characters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无角色，请添加新角色")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(characters.size) { index ->
                    CharacterItem(
                        character = characters[index],
                        onItemClick = { character ->
                            viewModel.navigate(AiChatRoutes.SingleChat(character.aiCharacterId))
                        },
                    )
                }
            }
        }
    }
    // 删除确认对话框
    if (showDeleteDialog && characterToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                characterToDelete = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除角色 \"${characterToDelete?.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        characterToDelete?.let { character ->
                            Timber.tag("HomeScreen").d("删除角色: ${character.name}")
                            viewModel.deleteCharacter(character)
                        }
                        showDeleteDialog = false
                        characterToDelete = null
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        characterToDelete = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
fun CharacterItem(
    character: AICharacter,
    onItemClick: (AICharacter) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Transparent, // 设置Card本身透明
            ),
        // 上面的colors针对Card本身，而modifier.background针对所处控件的内容区域（默认是透明的）,两者的作用不同
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
        //        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { onItemClick(character) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = character.backgroundPath,
                contentDescription = "角色背景",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp, 8.dp, 9.dp, 9.dp)),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Transparent,
                                            GrayBg.copy(alpha = 0.9f),
                                            GrayBg,
                                        ),
                                    startY = 0f, // 渐变起点Y坐标
                                    endY = Float.POSITIVE_INFINITY, // 渐变终点Y坐标
                                ),
                        ),
            )
            Column(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 角色信息
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(8.dp, 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            AsyncImage(
                                model = character.avatarPath,
                                contentDescription = "角色头像",
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape),
                            )
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.bodyMedium.copy(color = WhiteText),
                                maxLines = 1,
                            )
                        }
                        val prompt =
                            buildString {
                                if (character.roleIdentity.isNotBlank()) {
                                    append("${character.roleIdentity}\n")
                                }
                                if (character.roleAppearance.isNotBlank()) {
                                    append("${character.roleAppearance}\n")
                                }
                                if (character.roleDescription.isNotBlank()) {
                                    append("${character.roleDescription}\n")
                                }
                            }.trim()
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall.copy(color = WhiteText),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
