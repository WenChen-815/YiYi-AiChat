package com.wenchen.yiyi.feature.worldBook.view

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.core.designSystem.component.SettingTextFieldItem
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.WhiteText
import com.wenchen.yiyi.core.util.storage.FilesUtil
import com.wenchen.yiyi.core.util.StatusBarUtil
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import com.wenchen.yiyi.feature.worldBook.model.WorldBookItem
import com.wenchen.yiyi.feature.worldBook.viewmodel.WorldBookEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import com.wenchen.yiyi.core.util.toast.ToastUtils
import timber.log.Timber

@Composable
internal fun WorldBookEditRoute(
    viewModel: WorldBookEditViewModel = hiltViewModel(),
    navController: NavController
) {
    AIChatTheme {
        WorldBookEditScreen(viewModel, navController)
    }
}

//}
@Composable
private fun WorldBookEditScreen(
    viewModel: WorldBookEditViewModel = hiltViewModel(),
    navController: NavController
) {
    WorldBookEditScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldBookEditScreenContent(
    viewModel: WorldBookEditViewModel,
    navController: NavController
) {
    val activity = LocalActivity.current as ComponentActivity
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val worldItems = remember { mutableStateListOf<WorldBookItem>() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val worldBookState = viewModel.worldBook.collectAsState()
    // 根据 ViewModel 中的 WorldBook 数据更新 UI 状态
    LaunchedEffect(worldBookState.value) {
        worldBookState.value?.let { worldBook ->
            name = worldBook.worldName
            description = worldBook.worldDesc ?: ""

            worldItems.clear()
            worldItems.addAll(worldBook.worldItems ?: emptyList())
        }
    }
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个世界吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 执行删除操作
                        Timber.tag("WorldBookEditScreen").d("delete worldId: ${viewModel.worldId.value}")
                        val deleted = FilesUtil.deleteFile("world_book/${viewModel.worldId.value}.json")
                        if (deleted) {
                            ToastUtils.showToast("世界删除成功")
                            viewModel.navigateBack()
                        } else {
                            ToastUtils.showToast("世界删除失败")
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
//    LaunchedEffect(viewModel.worldId.collectAsState().value) {
//        launch(Dispatchers.IO) {
//            try {
//                if (!viewModel.isNewWorld.value) {
//                    val json = FilesUtil.readFile("world_book/${viewModel.worldId.value}.json")
//                    Timber.tag("WorldBookEditScreen").d("json: $json")
//                    val worldBookAdapter: JsonAdapter<WorldBook> =
//                        Moshi.Builder().build().adapter(WorldBook::class.java)
//                    // 转换为 WorldBook 对象
//                    val worldList = FilesUtil.listFileNames("world_book")
//                    worldBook = worldBookAdapter.fromJson(json) ?: WorldBook(
//                        viewModel.worldId.value,
//                        "无名世界${worldList.size + 1}"
//                    )
//                    name = worldBook?.worldName ?: "无名世界${worldList.size + 1}"
//                    description = worldBook?.worldDesc ?: ""
//
//                    //worldItems = worldBook?.worldItems?.toMutableStateList() ?: mutableStateListOf()
//                    /*
//                     知识点：对象引用
//                     使用这种方法会替换worldItems的引用，导致remember的worldItems失效，当数据更新时无法触发Compose的重新渲染
//                     */
//
//                    worldItems.clear() // 清空原有元素
//                    worldBook?.worldItems?.let { worldItems.addAll(it) } // 添加新元素
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast
//                        .makeText(context, "加载世界数据失败: ${e.message}", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//        }
//    }

    Scaffold(
        topBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "返回",
                    modifier =
                        Modifier
                            .clickable { viewModel.navigateBack() }
                            .size(18.dp)
                            .align(Alignment.CenterStart),
                )
                Text(
                    text = "世界配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
                // 删除按钮 仅在编辑现有世界时显示
                if (!viewModel.isNewWorld.collectAsState().value) {
                    Text(
                        text = "删除世界",
                        modifier = Modifier
                            .clickable { showDeleteDialog = true }
                            .align(Alignment.CenterEnd)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = {
                        viewModel.saveWorldBook(name, description, worldItems.toList())
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("保存世界", color = WhiteText)
                }
            }
        },
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
        ) {
            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                singleLine = true,
                label = "世界名称",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("请输入世界名称") },
            )

            SettingTextFieldItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 250.dp),
                label = "世界描述",
                labelPadding = PaddingValues(bottom = 6.dp),
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("请输入世界描述") },
                minLines = 5,
                maxLines = 15,
            )

            Text(
                text = "词条&释义",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(6.dp)
            )


            WorldItem(
                modifier = Modifier.fillMaxWidth(),
                isAddOne = true,
                name = "",
                desc = "",
                update = { _, _ -> },
                addItem = { newName, newDesc ->
                    worldItems.add(WorldBookItem(newName, newDesc))
                    Timber.tag("WorldBookEditActivity").d("worldItems: $worldItems")
                },
                deleteItem = { _ -> }
            )
            // 分割线
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            worldItems.forEach { (name, desc) ->
                // 知识点：使用key强制重组
                key(name) {
                    WorldItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        name = name,
                        desc = desc,
                        update = { newName, newDesc ->
                            val index = worldItems.indexOfFirst { it.name == name }
                            if (index != -1 && newName.isNotEmpty() && newDesc.isNotEmpty()) {
                                worldItems[index] =
                                    worldItems[index].copy(name = newName, desc = newDesc)
                            }
                        },
                        addItem = { _, _ -> },
                        deleteItem = { name ->
                            worldItems.removeIf { it.name == name }
                            Timber.tag("WorldBookEditActivity").d("worldItems: $worldItems")
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun WorldItem(
    modifier: Modifier = Modifier,
    isAddOne: Boolean = false,
    name: String,
    desc: String,
    update: (String, String) -> Unit,
    addItem: (String, String) -> Unit,
    deleteItem: (String) -> Unit,
) {
    var lastName by remember { mutableStateOf(name) }
    var lastDesc by remember { mutableStateOf(desc) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingTextFieldItem(
            value = lastName,
            label = "",
            onValueChange = {
                if (!isAddOne) update(it, desc)
                lastName = it
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            placeholder = { Text("请输入") },
        )
        SettingTextFieldItem(
            value = lastDesc,
            label = "",
            onValueChange = {
                if (!isAddOne) update(name, it)
                lastDesc = it
            },
            modifier = Modifier.weight(3f),
            placeholder = { Text("请输入") },
        )
        Text(
            if (isAddOne) "添加" else "删除",
            Modifier.clickable {
                if (isAddOne) {
                    addItem(lastName, lastDesc)
                    lastName = ""
                    lastDesc = ""
                } else {
                    deleteItem(name)
                }
            },
            color = MaterialTheme.colorScheme.primary
        )
    }
}
