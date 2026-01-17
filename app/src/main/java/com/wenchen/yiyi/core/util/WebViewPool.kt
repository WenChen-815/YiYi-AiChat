package com.wenchen.yiyi.core.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Stack

/**
 * WebView 缓存池，用于解决首次加载卡顿问题
 */
object WebViewPool {
    private val pool = Stack<WebView>()

    // 默认最大限制
    private const val ABSOLUTE_MAX_SIZE = 4

    /**
     * 获取当前设备允许的最大池大小
     */
    private fun getMaxPoolSize(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        // availMem 是字节单位
        val totalMemGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)

        return when {
            memoryInfo.lowMemory -> 1          // 系统已处于低内存状态
            totalMemGb <= 4 -> 1               // 4GB RAM 及以下设备：只存 1 个
            totalMemGb <= 6 -> 2               // 6GB RAM 设备：存 2 个
            totalMemGb <= 8 -> 3               // 8GB RAM 设备：存 3 个
            else -> ABSOLUTE_MAX_SIZE          // 12GB+ 设备：存 4 个
        }
    }

    /**
     * 预创建 WebView，建议在 Application 初始化或进入聊天界面前调用
     */
    fun prepare(context: Context) {
        val maxSize = getMaxPoolSize(context)
        if (pool.size < maxSize) {
            repeat(maxSize - pool.size) {
                pool.push(createWebView(context.applicationContext))
            }
        }
    }

    /**
     * 从池中获取一个 WebView，如果池为空则创建新实例
     */
    fun acquire(context: Context): WebView {
        return if (pool.isNotEmpty()) {
            pool.pop()
        } else {
            createWebView(context.applicationContext)
        }
    }

    /**
     * 回收 WebView 到池中，重置状态以便复用
     */
    fun release(webView: WebView) {
        // 将 WebView 从父容器移除
        (webView.parent as? ViewGroup)?.removeView(webView)
        
        // 重置状态
        webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            tag = null
            // 移除监听器和接口，防止旧数据干扰
            webViewClient = WebViewClient()
            webChromeClient = null
            removeJavascriptInterface("android")
            setOnTouchListener(null)
            setOnLongClickListener(null)
        }

        val maxSize = getMaxPoolSize(webView.context)
        if (pool.size < maxSize) {
            pool.push(webView)
        } else {
            webView.destroy()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            }
            setBackgroundColor(0)
            // 禁用一切滚动，由 Compose 外部容器负责滚动
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
        }
    }
}
