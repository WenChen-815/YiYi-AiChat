package com.wenchen.yiyi.feature.output.view

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalSmall
import com.wenchen.yiyi.core.designSystem.component.SpaceVerticalXLarge
import com.wenchen.yiyi.feature.output.viewmodel.CardParserViewModel

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardParserScreen(
    viewModel: CardParserViewModel = viewModel(),
    navController: NavHostController,
) {
    val context = LocalContext.current
    val rawJson by viewModel.rawJson.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.parseImage(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色卡解析工具") },
                actions = {
                    if (rawJson != null && !rawJson!!.startsWith("解析失败") && !rawJson!!.startsWith("等待解析")) {
                        IconButton(onClick = { viewModel.saveJsonToDownloads(context) }) {
                            Icon(Icons.Default.Save, contentDescription = "保存JSON")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isParsing
            ) {
                if (isParsing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("选择酒馆角色卡图片")
                }
            }

             SpaceVerticalXLarge()

            Text(
                text = "解析结果 (原始文本):",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )

            SpaceVerticalSmall()

            Box(
                modifier = Modifier
                    .weight(1f) // 使用 weight 占据剩余空间
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.medium
                    )
                    .padding(8.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = rawJson ?: "等待解析...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}
