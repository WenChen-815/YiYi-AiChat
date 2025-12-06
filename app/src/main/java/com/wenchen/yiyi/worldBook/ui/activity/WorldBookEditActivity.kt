package com.wenchen.yiyi.worldBook.ui.activity

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.common.components.SettingTextFieldItem
import com.wenchen.yiyi.common.theme.AIChatTheme
import com.wenchen.yiyi.common.theme.WhiteText
import com.wenchen.yiyi.common.utils.FilesUtil
import com.wenchen.yiyi.common.utils.StatusBarUtil
import com.wenchen.yiyi.worldBook.entity.WorldBook
import com.wenchen.yiyi.worldBook.entity.WorldBookItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class WorldBookEditActivity : ComponentActivity() {
    private lateinit var worldId: String
    private var isCreateNew = mutableStateOf(false)
    val moshi: Moshi = Moshi.Builder().build()
    // 生成WorldBook类的适配器
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        worldId = intent.getStringExtra("WORLD_BOOK_ID")?: run {
            isCreateNew.value = true
            UUID.randomUUID().toString()
        }
        Log.d("WorldBookEditActivity", "onCreate: $worldId")

        setContent {
            AIChatTheme {
                WorldBookEditScreen(
                    activity = this,
                    isCreateNew = isCreateNew.value,
                    worldId = worldId,
                    onSaveClick = { name, description, worldItems ->
                        lifecycleScope.launch {
                            saveWorldBook(name, description, worldItems)
                        }
                    }
                )
            }
        }
    }

    private suspend fun saveWorldBook(
        name: String,
        description: String,
        worldItems: List<WorldBookItem>
    ) {
        try {
            val worldBook = WorldBook(
                id = worldId,
                worldName = name,
                worldDesc = description,
                worldItems = worldItems
            )
            val json = worldBookAdapter.toJson(worldBook)
            val filePath = FilesUtil.saveToFile(json, "world_book/$worldId.json")
            if (filePath.isNotEmpty()) {
                Log.d("WorldBookEditActivity", "saveWorldBook: $filePath")
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@WorldBookEditActivity, "世界书保存成功", Toast.LENGTH_SHORT).show()
                this@WorldBookEditActivity.finish()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@WorldBookEditActivity, "世界书保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookEditScreen(
    activity: ComponentActivity,
    isCreateNew: Boolean,
    worldId: String,
    onSaveClick: (String, String, List<WorldBookItem>) -> Unit,
) {
    if (isSystemInDarkTheme()) {
        StatusBarUtil.setStatusBarTextColor(activity, false)
    } else {
        StatusBarUtil.setStatusBarTextColor(activity, true)
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val worldItems = remember { mutableStateListOf<WorldBookItem>() }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var worldBook by remember { mutableStateOf<WorldBook?>(null) }
    LaunchedEffect(worldId) {
        launch(Dispatchers.IO) {
            try {
                if (!isCreateNew) {
                    val json = FilesUtil.readFile("world_book/$worldId.json")
                    Log.d("WorldBookEditScreen", "json: $json")
                    val worldBookAdapter: JsonAdapter<WorldBook> = Moshi.Builder().build().adapter(WorldBook::class.java)
                    // 转换为 WorldBook 对象
                    val worldList = FilesUtil.listFileNames("world_book")
                    worldBook = worldBookAdapter.fromJson(json) ?: WorldBook(worldId, "无名世界${worldList.size + 1}")
                    name = worldBook?.worldName ?: "无名世界${worldList.size + 1}"
                    description = worldBook?.worldDesc ?: ""

                    //worldItems = worldBook?.worldItems?.toMutableStateList() ?: mutableStateListOf()
                    /*
                     知识点：对象引用
                     使用这种方法会替换worldItems的引用，导致remember的worldItems失效，当数据更新时无法触发Compose的重新渲染
                     */

                    worldItems.clear() // 清空原有元素
                    worldBook?.worldItems?.let { worldItems.addAll(it) } // 添加新元素
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(context, "加载世界书数据失败: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

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
                            .clickable { activity.finish() }
                            .size(18.dp)
                            .align(Alignment.CenterStart),
                )
                Text(
                    text = "世界书配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
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
                        onSaveClick(name, description, worldItems.toList())
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("保存世界书", color = WhiteText)
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
                    Log.d("WorldBookEditActivity", "worldItems: $worldItems")
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                            Log.d("WorldBookEditActivity", "worldItems: $worldItems")
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
    Row(modifier = modifier,
        verticalAlignment = Alignment.CenterVertically) {
        SettingTextFieldItem(
            value = lastName,
            onValueChange = {
                if (!isAddOne) update(it, desc)
                lastName = it
            },
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            placeholder = { Text("请输入") },
        )
        SettingTextFieldItem(
            value = lastDesc,
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
            color = MaterialTheme.colorScheme.primary)
    }
}
