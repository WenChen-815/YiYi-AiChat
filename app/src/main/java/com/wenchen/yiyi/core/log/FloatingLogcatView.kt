package com.wenchen.yiyi.core.log

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt


@Composable
fun FloatingLogcatView() {
    var isVisible by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(true) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    val logViewModel: LogViewModel = viewModel()
    val logs by logViewModel.logs.collectAsState()

    // Clipboard and context
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Screen dimensions for boundary checks
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    var composableSize by remember { mutableStateOf(IntSize.Zero) }

    if (isVisible) {
        Box(
            modifier = Modifier
                .offset { offset }
                .onSizeChanged { composableSize = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newOffsetX = (offset.x + dragAmount.x)
                            .roundToInt()
                            .coerceIn(0, (screenWidthPx - composableSize.width).roundToInt())
                        val newOffsetY = (offset.y + dragAmount.y)
                            .roundToInt()
                            .coerceIn(0, (screenHeightPx - composableSize.height).roundToInt())
                        offset = IntOffset(newOffsetX, newOffsetY)
                    }
                }
                .animateContentSize()
        ) {
            if (isExpanded) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 300.dp)
                        .heightIn(max = 400.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Logcat", style = MaterialTheme.typography.titleSmall)
                            Row {
                                IconButton(onClick = {
                                    val logText = logs.joinToString("\n") { "[${it.tag ?: "NO_TAG"}] ${it.message}" }
                                    clipboardManager.setText(AnnotatedString(logText))
                                    Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
                                }
                                IconButton(onClick = { logViewModel.clearLogs() }) {
                                    Icon(Icons.Default.AutoDelete, contentDescription = "Clear logs")
                                }
                                IconButton(onClick = { isExpanded = false }) {
                                    Icon(Icons.Default.UnfoldLess, contentDescription = "Collapse logcat")
                                }
//                                IconButton(onClick = { isVisible = false }) {
//                                    Icon(Icons.Default.Close, contentDescription = "Close logcat")
//                                }
                            }
                        }

                        // Logs
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                            reverseLayout = true
                        ) {
                            items(logs) {
                                log -> LogcatItem(log)
                            }
                        }
                    }
                }
            } else {
                // Collapsed View
                Card(
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(56.dp)
                        .clickable { isExpanded = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Expand logcat"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatItem(logEntry: LogEntry) {
    val color = when (logEntry.priority) {
        Log.ERROR -> Color.Red
        Log.WARN -> Color(0xFFFFA500) // Orange
        Log.INFO -> Color.Green
        Log.DEBUG -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = logEntry.tag ?: "NO_TAG",
            color = color,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = logEntry.message,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
