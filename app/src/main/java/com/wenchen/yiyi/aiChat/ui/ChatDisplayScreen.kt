package com.wenchen.yiyi.aiChat.ui

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenchen.yiyi.aiChat.entity.Conversation
import com.wenchen.yiyi.aiChat.entity.ConversationType
import com.wenchen.yiyi.aiChat.ui.activity.ChatActivity
import com.wenchen.yiyi.aiChat.ui.activity.GroupChatActivity
import com.wenchen.yiyi.aiChat.vm.ChatDisplayViewModel
import com.wenchen.yiyi.common.App
import com.wenchen.yiyi.common.theme.BlackBg
import com.wenchen.yiyi.common.theme.WhiteBg
import com.wenchen.yiyi.common.utils.ChatUtil
import com.wenchen.yiyi.config.common.ConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDisplayScreen(
    context: Context,
    viewModel: ChatDisplayViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val conversations by viewModel.conversations.collectAsState()
    val chatMessageDao = App.appDatabase.chatMessageDao()
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
                            lastMessage = ChatUtil.parseMessage(message)
                        } catch (e: Exception) {
                            lastMessage = ""
                        }
                    }
                    GroupItem(
                        conversation = conversation,
                        lastMessageContent = lastMessage,
                        onItemClick = { conv ->
                            if (conversation.type == ConversationType.GROUP) {
                                val intent = Intent(context, GroupChatActivity::class.java)
                                intent.putExtra("conversationId", conversation.id)
                                context.startActivity(intent)
                            } else {
                                ConfigManager().saveSelectedCharacterId(conv.characterIds.keys.first())
                                val intent = Intent(context, ChatActivity::class.java)
                                intent.putExtra(
                                    "SELECTED_CHARACTER_ID",
                                    conv.characterIds.keys.first(),
                                )
                                context.startActivity(intent)
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
                        .size(40.dp)
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
                Text(
                    text = if (conversation.type == ConversationType.GROUP) "群" else "单",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
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
