package com.wenchen.yiyi.core.designSystem.component

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wenchen.yiyi.core.util.WebViewPool
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun HtmlWebView(
    id: String,
    html: String,
    height: Int? = null,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    onHeight: (Int) -> Unit = {},
    onLongClick: (Offset) -> Unit = {}
) {
    val isNew = height == null
    // 使用 id 作为 key，确保新消息能重置高度，但同一条消息切换 Tab 时保持最大高度
    val heightValue = height ?: 50
    var webViewHeight by remember(id) { mutableIntStateOf(heightValue) }
    // 增加一个标记，确保在 Composable 被销毁后不再更新高度状态
    var isDisposed by remember { mutableStateOf(false) }
    DisposableEffect(id) {
        onDispose { isDisposed = true }
    }

    val currentOnLongClick by rememberUpdatedState(onLongClick)
    var lastTouchX by remember { mutableFloatStateOf(0f) }
    var lastTouchY by remember { mutableFloatStateOf(0f) }

    val styledHtml = remember(html, textColor) {
        val colorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { 
                    box-sizing: border-box; 
                    /* 禁用选择和系统默认长按菜单 */
                    -webkit-user-select: none;  
                    user-select: none;
                    -webkit-touch-callout: none; 
                }
                html, body { 
                    background-color: transparent; 
                    margin: 0; padding: 0;
                    /* 关键：防止 body 自动撑满 WebView 高度 */
                    height: auto !important;
                    min-height: 0 !important;
                }
                body { 
                    color: $colorHex; 
                    font-family: -apple-system, system-ui, sans-serif;
                    font-size: 15px;
                    line-height: 1.4;
                    word-wrap: break-word;
                    overflow: hidden; 
                }
                img { max-width: 100%; height: auto; border-radius: 8px; display: block; }
                #container {
                    width: 100%;
                    /* 关键：使用 flow-root 确保内部 margin 不会溢出到 body 之外 */
                    display: flow-root; 
                    position: relative;
                }
            </style>
        </head>
        <body>
            <div id="container">$html</div>
            <script>
                function reportHeight() {
                    const container = document.getElementById('container');
                    if (!container) return;
                    
                    // 核心修改：只测量 container 的高度，严禁使用 scrollHeight 或 body 高度
                    // getBoundingClientRect().height 能获取包含小数的精确物理高度
                    const height = Math.ceil(container.getBoundingClientRect().height);
                    
                    if (height > 0) {
                        window.android.onHeight(height);
                    }
                }

                window.onload = reportHeight;
                
                // ResizeObserver 观察 container 的尺寸变化
                const resizeObserver = new ResizeObserver(() => {
                    reportHeight();
                });
                resizeObserver.observe(document.getElementById('container'));
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(webViewHeight.dp),
        factory = { context ->
            WebViewPool.acquire(context).apply {
                // 记录最后一次点击位置
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                    false
                }

                // 启用长按并触发回调
                isLongClickable = true
                setOnLongClickListener {
                    currentOnLongClick(Offset(lastTouchX, lastTouchY))
                    true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("reportHeight()") {}
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeight(height: Int) {
                        // 1. 检查高度是否有效
                        // 2. 检查当前 View 是否还挂载在窗口上
                        // 3. 检查 Composable 是否已被销毁
                        // 增加阈值判断，避免微小抖动导致无限重绘
                        if (height > 0 && abs(height - webViewHeight) > 5 && !isDisposed) {
                            // 主线程更新 Compose 状态
                            post {
                                if (!isDisposed && isNew) {
                                    webViewHeight = height
                                    onHeight(height)
                                }
                            }
                        }
                    }
                }, "android")
            }
        },
        update = { webView ->
            val currentTag = webView.tag as? String
            if (currentTag != styledHtml) {
                webView.loadDataWithBaseURL("https://tavo.ai/", styledHtml, "text/html", "UTF-8", null)
                webView.tag = styledHtml
            }
        },
        onRelease = { webView ->
            WebViewPool.release(webView)
        }
    )
}
