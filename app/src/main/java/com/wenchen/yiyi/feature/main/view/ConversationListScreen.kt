package com.wenchen.yiyi.feature.main.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.database.entity.Conversation
import com.wenchen.yiyi.core.database.entity.ConversationType
import com.wenchen.yiyi.feature.main.viewmodel.ConversationListViewModel
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.util.business.ChatUtils
import com.wenchen.yiyi.navigation.routes.AiChatRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    context: Context,
    viewModel: ConversationListViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val conversations by viewModel.conversations.collectAsState()
    val chatMessageDao = Application.appDatabase.chatMessageDao()
    val imageManager = ImageManager(context)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // 每次回到主页时重新加载数据
                    viewModel.refreshConversations()
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
                .padding(16.dp),
    ) {
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无群组，请创建新群组")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(conversations.size) {
                    val conversation = conversations[it]
                    // 不能在GroupItem内部去调用挂起函数，在这里就获取到需要的内容
                    var lastMessage by remember(conversation.id) { mutableStateOf("加载中...") }
                    LaunchedEffect(conversation.id) {
                        try {
                            val message = chatMessageDao.getLastMessageByConversationId(conversation.id)
                            lastMessage = ChatUtils.parseMessage(message).cleanedContent
                        } catch (e: Exception) {
                            lastMessage = ""
                        }
                    }
                    GroupItem(
                        conversation = conversation,
                        lastMessageContent = lastMessage,
                        imageManager = imageManager,
                        onItemClick = { conv ->
                            if (conversation.type == ConversationType.GROUP) {
                                viewModel.navigate(AiChatRoutes.GroupChat(conversation.id))
                            } else {
                                viewModel.navigate(AiChatRoutes.SingleChat(conv.characterIds.keys.first()))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun GroupItem(
    conversation: Conversation,
    lastMessageContent: String,
    imageManager: ImageManager,
    onItemClick: (Conversation) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isSystemInDarkTheme()) BlackBg else WhiteBg)
                .clickable { onItemClick(conversation) },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 群组类型标识
            Box(
                modifier =
                    Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (conversation.type == ConversationType.GROUP) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                val avatarImage = imageManager.getAvatarImage(
                    if (conversation.type == ConversationType.GROUP) {
                        conversation.id
                    } else {
                        conversation.characterIds.keys.first()
                    }
                )
                if (avatarImage != null) {
                    AsyncImage(
                        model = avatarImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = if (conversation.type == ConversationType.GROUP) "群" else "单",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
            ) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMessageContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.type == ConversationType.GROUP) {
                Text(
                    text = " ${conversation.characterIds.size + 1}人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
