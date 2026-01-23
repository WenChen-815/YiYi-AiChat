package com.wenchen.yiyi.core.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wenchen.yiyi.core.util.common.hrefPattern
import com.wenchen.yiyi.core.util.common.importPattern
import com.wenchen.yiyi.core.util.common.linkTagPattern
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
    highlightColor: String = "#FFBF4D",
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

    // 预处理 HTML，分离 import
    val (processedContent, asyncCssLinks) = remember(html) {
        extractAndAsyncCss(html)
    }
    val styledHtml = remember(processedContent, asyncCssLinks, textColor) {
        val colorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            $asyncCssLinks
            <style>
                /* 全局隐藏滚动条 */
                ::-webkit-scrollbar { display: none; }
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
                    /*white-space: pre-line;*/
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
                /* 定义高亮样式 */
                .hl-text { color: $highlightColor !important; }
                
                /* 专门用于包裹“有效文本”的样式 只有被包裹的文字才会保留换行符*/
                .content-text {
                    white-space: pre-wrap; 
                    word-break: break-word;
                }
            </style>
        </head>
        <body>
            <div id="container">${processedContent.trimIndent()}</div>
            <script>
                // 文本高亮逻辑 ---
                function highlightContent() {
                    const root = document.getElementById('container');
                    if (!root) return;
                    
                    // 定义匹配正则：
                    // 包含：""、“” (中文引号)、【】、[]、{}、()、 （）
                    // 注意：正则中特殊符号 []{}() 需要转义
                    const pattern = /(“[^”]*”|"[^"]*"|【[^】]*】|\[[^\]]*\]|\{[^}]*\}|\([^)]*\)|（[^）]*）)/g;
                    // === 添加过滤器 ===
                    const filter = {
                        acceptNode: function(node) {
                            // 1. 检查父节点标签名
                            const parentTag = node.parentNode.tagName.toUpperCase();
                            
                            // 2. 严禁进入 STYLE, SCRIPT, NOSCRIPT, TEXTAREA 内部
                            if (["STYLE", "SCRIPT", "NOSCRIPT", "TEXTAREA", "CODE", "PRE"].includes(parentTag)) {
                                return NodeFilter.FILTER_REJECT;
                            }
                            
                            // 3. 避免重复处理已经高亮过的节点（防止死循环）
                            if (node.parentNode.classList && node.parentNode.classList.contains('hl-text')) {
                                return NodeFilter.FILTER_REJECT;
                            }
                            
                            return NodeFilter.FILTER_ACCEPT;
                        }
                    };
                    // 使用 TreeWalker 只遍历文本节点，绝对安全，不会破坏 HTML 标签
                    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, filter, false);
                    const textNodes = [];
                    let node;
                    while(node = walker.nextNode()) textNodes.push(node);

                    textNodes.forEach(textNode => {
                        const text = textNode.nodeValue;
                        
                        // === 智能判断 ===
                        // 如果这个节点只包含空格、换行符（即 HTML 结构缩进），则跳过
                        // 浏览器会自动合并它们，不会破坏布局
                        if (!text.trim()) {
                            return;
                        }
                        
                        // === 如果有有效内容，则进行处理 ===
                        const fragment = document.createDocumentFragment();
                        
                        // 创建一个容器 span，赋予 pre-wrap 属性
                        // 这样这个节点内部的 \n 就会生效，但不会影响外部布局
                        const wrapper = document.createElement('span');
                        wrapper.className = 'content-text';
                        
                        let lastIndex = 0;
                        // 执行高亮匹配
                        text.replace(pattern, (match, p1, offset) => {
                            if (offset > lastIndex) {
                                wrapper.appendChild(document.createTextNode(text.substring(lastIndex, offset)));
                            }
                            
                            const span = document.createElement('span');
                            span.className = 'hl-text';
                            span.textContent = match;
                            wrapper.appendChild(span);
                            
                            lastIndex = offset + match.length;
                            return match;
                        });

                        if (lastIndex < text.length) {
                            wrapper.appendChild(document.createTextNode(text.substring(lastIndex)));
                        }
                        
                        // 将处理好的 wrapper 放入 fragment
                        fragment.appendChild(wrapper);
                        
                        // 替换原始文本节点
                        textNode.parentNode.replaceChild(fragment, textNode);
                    });
                }
                
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

                // 页面加载流程
                window.onload = function() {
                    // 1. 先执行高亮（因为高亮可能会改变文字换行，影响高度）
                    highlightContent();
                    // 2. 再计算高度
                    reportHeight();
                };
                
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

/**
 * 提取 HTML 中的阻塞式 CSS (@import 和 <link rel="stylesheet">)，
 * 并将其转换为异步加载的 <link> 标签字符串。
 *
 * @param sourceHtml 原始 HTML
 * @return Pair(清理后的HTML, 生成的异步Link标签字符串)
 */
private fun extractAndAsyncCss(sourceHtml: String): Pair<String, String> {
    val asyncLinks = StringBuilder()
    var processedHtml = sourceHtml

    // --- 1. 处理 @import ---
    processedHtml = importPattern.replace(processedHtml) { matchResult ->
        val url = matchResult.groupValues[1]
        // 生成异步 link
        asyncLinks.append(createAsyncLinkTag(url))
        "" // 移除原 @import
    }

    // --- 2. 处理 <link rel="stylesheet"> ---
    processedHtml = linkTagPattern.replace(processedHtml) { matchResult ->
        val tagContent = matchResult.value

        // 步骤 B: 检查是否是样式表 (rel="stylesheet" 或 rel='stylesheet')
        val isStylesheet = tagContent.contains("rel=\"stylesheet\"", ignoreCase = true) ||
                tagContent.contains("rel='stylesheet'", ignoreCase = true)

        if (isStylesheet) {
            // 步骤 C: 提取 href 属性
            val hrefMatch = hrefPattern.find(tagContent)

            if (hrefMatch != null) {
                val url = hrefMatch.groupValues[1]
                asyncLinks.append(createAsyncLinkTag(url))
                return@replace "" // 是样式表，提取后移除原标签
            }
        }

        // 如果不是样式表（例如 icon, preconnect），或者没找到 href，则保留原样
        tagContent
    }

    return processedHtml to asyncLinks.toString()
}

// 辅助函数：生成防阻塞的 Link 标签
private fun createAsyncLinkTag(url: String): String {
    return """<link rel="stylesheet" href="$url" media="print" onload="this.media='all';this.onload=null;">"""
}