package com.wenchen.yiyi.feature.main.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.wenchen.yiyi.core.util.business.ChatUtils
import com.wenchen.yiyi.navigation.routes.AiChatRoutes

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
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshConversations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (conversations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("这里空空如也，去打个招呼吧", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(conversations.size, key = { conversations[it].id }) { index ->
                val conversation = conversations[index]
                var lastMessage by remember(conversation.id) { mutableStateOf("") }
                
                LaunchedEffect(conversation.id) {
                    try {
                        val message = chatMessageDao.getLastMessageByConversationId(conversation.id)
                        lastMessage = ChatUtils().parseMessage(message).cleanedContent
                    } catch (e: Exception) {
                        lastMessage = ""
                    }
                }

                ConversationListItem(
                    conversation = conversation,
                    lastMessage = lastMessage,
                    imageManager = imageManager,
                    onClick = {
                        if (conversation.type == ConversationType.GROUP) {
                            viewModel.navigate(AiChatRoutes.GroupChat(conversation.id))
                        } else {
                            viewModel.navigate(AiChatRoutes.SingleChat(conversation.characterIds.keys.first()))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ConversationListItem(
    conversation: Conversation,
    lastMessage: String,
    imageManager: ImageManager,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val avatarImage = imageManager.getAvatarImage(
                    if (conversation.type == ConversationType.GROUP) conversation.id 
                    else conversation.characterIds.keys.first()
                )
                if (avatarImage != null) {
                    AsyncImage(
                        model = avatarImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = if (conversation.type == ConversationType.GROUP) "群" else "单",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    
                    if (conversation.type == ConversationType.GROUP) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${conversation.characterIds.size + 1}",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = lastMessage.ifEmpty { "点击开启对话" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
