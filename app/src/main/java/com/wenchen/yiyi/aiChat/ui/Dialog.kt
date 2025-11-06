package com.wenchen.yiyi.aiChat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.common.App
import com.wenchen.yiyi.common.components.SettingTextFieldItem
import com.wenchen.yiyi.common.entity.Model

@Composable
fun ModelsDialog(
    supportedModels: List<Model>,
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedModel = remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择模型")
        },
        text = {
            LazyColumn {
                items(supportedModels.size) { index ->
                    val model = supportedModels[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedModel.value = model.id
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel.value == model.id,
                            onClick = { selectedModel.value = model.id }
                        )
                        Text(
                            text = model.id,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onModelSelected(selectedModel.value)
                    onDismiss()
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ClearChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "确认清空")
        },
        text = {
            Text(text = "确定要清空当前角色的所有聊天记录吗？")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DeleteCharacterDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    name: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除角色 \"$name\" 吗？") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ShowMemoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    characterId: String,
    conversationId: String,
) {
    val memoryContent = remember { mutableStateOf("") }
    LaunchedEffect(characterId, conversationId) {
        try {
            val memory = App.appDatabase.aiChatMemoryDao().getByCharacterIdAndConversationId(characterId, conversationId)
            memory?.content?.let {
                memoryContent.value = it
            } ?: run {
                memoryContent.value = ""
            }
        } catch (e: Exception) {
            memoryContent.value = ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "角色记忆"
            )
        },
        text = {
            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(255.dp)
                        .padding(top = 8.dp),
                labelPadding = PaddingValues(bottom = 6.dp),
                value = memoryContent.value,
                onValueChange = { memoryContent.value = it },
                placeholder = { Text("请输入角色记忆内容") },
                maxLines = 15
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(memoryContent.value)
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
