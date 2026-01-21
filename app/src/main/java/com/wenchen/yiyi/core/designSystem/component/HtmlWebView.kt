package com.wenchen.yiyi.core.designSystem.component

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wenchen.yiyi.core.util.system.WebViewPool
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun HtmlWebView(
    modifier: Modifier = Modifier,
    id: String,
    html: String,
    height: Int? = null,
    textColor: Color = Color.White,
    onHeight: (Int) -> Unit = {},
    onLongClick: (Offset) -> Unit = {}
) {
    // 如果有缓存(height)，初始值就用缓存值，避免由0展开的闪烁；
    // 如果无缓存，给一个默认最小高度(50)，等待 JS 回调撑开。
    // 使用 mutableIntStateOf 避免拆箱装箱开销
    var webViewHeight by remember(id) { mutableIntStateOf(height ?: 50) }

    // 标记是否已有缓存数据，用于后续判断是否需要激进更新
    val hasCachedHeight = remember(id) { height != null }
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
                    /* 禁用点击高亮，减少布局反馈 */
                    -webkit-tap-highlight-color: transparent;
                }
                html, body { 
                    background-color: transparent; 
                    margin: 0; padding: 0;
                    /* 关键：防止 body 自动撑满 WebView 高度 */
                    height: auto !important;
                    min-height: 0 !important;
                    /* 开启硬件加速渲染 */
                    transform: translateZ(0); 
                    
                   -webkit-backface-visibility: hidden;
                    backface-visibility: hidden;
                    perspective: 1000px;
                }
                body { 
                    color: $colorHex; 
                    font-family: -apple-system, system-ui, sans-serif;
                    font-size: 15px;
                    line-height: 1.4;
                    word-wrap: break-word;
                    overflow: hidden; 
                    /* 文本渲染优化 */
                    text-rendering: optimizeLegibility;
                    -webkit-font-smoothing: antialiased;
                }
                img { 
                    max-width: 100%; height: auto; border-radius: 8px; display: block;
                    /* 强制图片也有独立的合成层，防止滑动闪烁 */
                    transform: translateZ(0);
                }
                #container {
                    width: 100%;
                    /* 关键：使用 flow-root 确保内部 margin 不会溢出到 body 之外 */
                    display: flow-root; 
                    position: relative;
                    /* 核心优化：渲染隔离。包含布局、样式和绘制，防止子元素变化影响全局 */
                    contain: content;
                    /* 提示浏览器该元素将有变化，预先分配 GPU 资源 */
                    will-change: transform;
                }
            </style>
        </head>
        <body>
            <div id="container">$html</div>
            <script>
                var lastReportedHeight = 0;
                function reportHeight() {
                    const container = document.getElementById('container');
                    if (!container) return;
                    
                    // 核心修改：只测量 container 的高度，严禁使用 scrollHeight 或 body 高度
                    // getBoundingClientRect().height 能获取包含小数的精确物理高度
                    const height = Math.ceil(container.getBoundingClientRect().height);
                    
                    // 优化 2：JS 端防抖，只有高度变化超过 2px 才上报，减少 Bridge 调用频率
                    if (height > 0 && Math.abs(height - lastReportedHeight) > 2) {
                        lastReportedHeight = height;
                        window.android.onHeight(height);
                    }
                }

                window.onload = reportHeight;
                
                // ResizeObserver 观察 container 的尺寸变化
                const resizeObserver = new ResizeObserver(() => {
                    // 使用 requestAnimationFrame 确保在浏览器渲染帧中处理
                    window.requestAnimationFrame(reportHeight);
                });
                resizeObserver.observe(document.getElementById('container'));
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(webViewHeight.dp)
    ) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                WebViewPool.acquire(context).apply {
                    // 强制开启硬件加速
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    // 确保即使从池中获取，也是非焦点状态
                    isFocusable = false
                    isFocusableInTouchMode = false

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
                            // 页面加载完立即触发一次高度检测
                            view?.evaluateJavascript("reportHeight()") {}
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onHeight(reportedHeight: Int) {
                            if (isDisposed) return

                            // 3. 核心高度修正逻辑：
                            // 计算新旧高度的差值绝对值
                            val diff = abs(reportedHeight - webViewHeight)

                            // 阈值设定：
                            // 如果当前是默认高度(无缓存情况)，任何大于0的高度都应该更新。
                            // 如果已有缓存高度，只有当差异超过一定阈值(例如 10dp)才更新，
                            // 这样可以避免因为字体渲染微小差异导致的界面抖动。
                            val threshold = if (hasCachedHeight) 10 else 0

                            if (reportedHeight > 0 && diff > threshold) {
                                post {
                                    if (!isDisposed) {
                                        webViewHeight = reportedHeight
                                        // 总是回调给上层，让上层有机会更新缓存
                                        onHeight(reportedHeight)
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
                    webView.loadDataWithBaseURL(
                        "https://tavo.ai/",
                        styledHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webView.tag = styledHtml
                }
            },
            onRelease = { webView ->
                WebViewPool.release(webView)
            }
        )
    }
}
