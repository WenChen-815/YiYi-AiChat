package com.wenchen.yiyi.aiChat.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wenchen.yiyi.common.App
import com.wenchen.yiyi.common.components.SettingTextFieldItem
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.StatusBarUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationEditActivity : ComponentActivity() {
    private lateinit var conversationId: String
    private var conversationDao = App.appDatabase.conversationDao()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        conversationId = intent.getStringExtra("CONVERSATION_ID") ?: run {
            Toast.makeText(this, "无效的对话ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AIChatTheme {
                ConversationEditScreen(
                    activity = this,
                    conversationId = conversationId,
                    onSaveClick = { name, playerName, playGender, playerDescription, chatWorldId, chatSceneDescription ->
                        lifecycleScope.launch {
                            saveConversation(
                                name,
                                playerName,
                                playGender,
                                playerDescription,
                                chatWorldId,
                                chatSceneDescription
                            )
                        }
                    },
                    onCancelClick = { finish() })
            }
        }
    }

    private suspend fun saveConversation(
        name: String,
        playerName: String,
        playGender: String,
        playerDescription: String,
        chatWorldId: String,
        chatSceneDescription: String
    ) {
        if (name.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ConversationEditActivity, "请输入对话名称", Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val existingConversation = conversationDao.getById(conversationId)
                if (existingConversation != null) {
                    val updatedConversation = existingConversation.copy(
                        name = name,
                        playerName = playerName,
                        playGender = playGender,
                        playerDescription = playerDescription,
                        chatWorldId = chatWorldId,
                        chatSceneDescription = chatSceneDescription
                    )

                    val result = conversationDao.update(updatedConversation)

                    withContext(Dispatchers.Main) {
                        if (result > 0) {
                            Toast.makeText(
                                this@ConversationEditActivity, "更新成功", Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@ConversationEditActivity, "更新失败", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ConversationEditActivity, "对话不存在", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ConversationEditActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationEditScreen(
    activity: ComponentActivity,
    conversationId: String,
    onSaveClick: (String, String, String, String, String, String) -> Unit,
    onCancelClick: () -> Unit
) {
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }

    var name by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf("") }
    var playGender by remember { mutableStateOf("") }
    var playerDescription by remember { mutableStateOf("") }
    var chatWorldId by remember { mutableStateOf("") }
    var chatSceneDescription by remember { mutableStateOf("") }
    var characterNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val conversationDao = App.appDatabase.conversationDao()
    val aiCharacterDao = App.appDatabase.aiCharacterDao()

    LaunchedEffect(conversationId) {
        launch(Dispatchers.IO) {
            try {
                val conversation = conversationDao.getById(conversationId)
                if (conversation != null) {
                    name = conversation.name
                    playerName = conversation.playerName
                    playGender = conversation.playGender
                    playerDescription = conversation.playerDescription
                    chatWorldId = conversation.chatWorldId
                    chatSceneDescription = conversation.chatSceneDescription

                    // 获取角色名称列表
                    val characters = aiCharacterDao.getCharactersByIds(conversation.characterIds)
                    characterNames = characters.map { it.name }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "加载对话数据失败: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "返回",
                    modifier = Modifier
                        .clickable { activity.finish() }
                        .size(18.dp)
                        .align(Alignment.CenterStart))
                Text(
                    text = "对话配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("取消编辑", color = WhiteText)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        onSaveClick(
                            name,
                            playerName,
                            playGender,
                            playerDescription,
                            chatWorldId,
                            chatSceneDescription
                        )
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("保存对话", color = WhiteText)
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "对话名称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("请输入对话名称") })

            Text(
                text = "参与角色",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            characterNames.forEach { characterName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = characterName,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(WhiteText)
                    )
                }
            }

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "我的名称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playerName,
                onValueChange = { playerName = it },
                placeholder = { Text("请输入名称") })

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "我的性别",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playGender,
                onValueChange = { playGender = it },
                placeholder = { Text("请输入性别，如沃尔玛购物袋") })

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "我的描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = playerDescription,
                onValueChange = { playerDescription = it },
                placeholder = { Text("请输入对自己的描述") },
                minLines = 3,
                maxLines = 10
            )

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = "世界ID",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = chatWorldId,
                onValueChange = { chatWorldId = it },
                placeholder = { Text("未实现，敬请期待") })

            SettingTextFieldItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 250.dp),
                label = "场景描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = chatSceneDescription,
                onValueChange = { chatSceneDescription = it },
                placeholder = { Text("请输入场景描述") },
                minLines = 5,
                maxLines = 15
            )
        }
    }
}
