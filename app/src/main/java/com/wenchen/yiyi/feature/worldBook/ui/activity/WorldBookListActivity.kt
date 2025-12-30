package com.wenchen.yiyi.feature.worldBook.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonAdapter
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.core.common.theme.BlackBg
import com.wenchen.yiyi.core.common.theme.WhiteBg
import com.wenchen.yiyi.core.common.utils.FilesUtil
import com.wenchen.yiyi.feature.worldBook.entity.WorldBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorldBookListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIChatTheme{
                WorldBookListScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookListScreen(
    context: Context,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    var worldBooks by remember { mutableStateOf<List<WorldBook>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val moshi: Moshi = Moshi.Builder().build()
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)

    // 当屏幕恢复时加载世界书列表
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    loadWorldBooks(worldBookAdapter) { loadedBooks ->
                        worldBooks = loadedBooks
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界列表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, WorldBookEditActivity::class.java)
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("创建")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)  // 使用Scaffold提供的内边距
                .padding(16.dp),
        ) {
            if (worldBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无世界书，请创建新的世界书")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(worldBooks.size) { index ->
                        val worldBook = worldBooks[index]
                        WorldBookItem(
                            worldBook = worldBook,
                            onItemClick = {
                                val intent = Intent(context, WorldBookEditActivity::class.java)
                                intent.putExtra("WORLD_BOOK_ID", worldBook.id)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

// 从文件加载世界书列表
private suspend fun loadWorldBooks(
    adapter: JsonAdapter<WorldBook>,
    onLoaded: (List<WorldBook>) -> Unit
) = withContext(Dispatchers.IO) {
    val worldBookFiles = FilesUtil.listFileNames("world_book")
    Log.d("WorldBookListActivity", "loadWorldBooks: $worldBookFiles")
    val worldBooks = mutableListOf<WorldBook>()

    worldBookFiles.forEach { fileName ->
        try {
            val json = FilesUtil.readFile("world_book/$fileName")
            val worldBook = adapter.fromJson(json)
            worldBook?.let { worldBooks.add(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    withContext(Dispatchers.Main) {
        onLoaded(worldBooks)
    }
}

@Composable
fun WorldBookItem(
    worldBook: WorldBook,
    onItemClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSystemInDarkTheme()) BlackBg else WhiteBg)
            .clickable { onItemClick() }
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = worldBook.worldName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = worldBook.worldDesc ?: "无描述",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "物品: ${worldBook.worldItems?.size ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}