package com.wenchen.yiyi.core.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap

object BitMapUtil {
    // 定义一个魔术字，用于识别追加的数据
    const val MAGIC_WORD = "YIYI_DATA"

    /**
     * 将 Composable 内容捕获为 Bitmap。
     */
    fun captureComposableAsBitmap(context: Context, width: Int, height: Int = 0,content: @Composable () -> Unit): Bitmap {
        val activity = findActivity(context) ?: throw IllegalArgumentException("Context must be an Activity or wrapped by an Activity")
        val composeView = ComposeView(activity).apply {
            setContent(content)
            // 强制使用软件渲染层，防止 hardware bitmap 冲突并确保离屏捕获更稳定
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            // 离屏渲染时设置组合策略
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

        // 临时挂载到 DecorView 以获得 WindowRecomposer 环境
        val rootLayout = activity.window.decorView as ViewGroup
        val tempContainer = FrameLayout(activity).apply {
            visibility = View.INVISIBLE
        }
        tempContainer.addView(composeView)
        rootLayout.addView(tempContainer)

        return try {
            composeView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
            composeView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)

//            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.UNSPECIFIED)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.UNSPECIFIED)

            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

            val bitmap = createBitmap(composeView.measuredWidth, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)

            bitmap
        } finally {
            // 截图完毕必须移除，避免内存泄漏
            rootLayout.removeView(tempContainer)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * 将 Bitmap 保存到设备的公共图库目录。
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        return saveBitmapToGalleryInternal(context, bitmap, displayName)
    }

    /**
     * 使用 Exif (UserComment) 将数据写入图片。
     * ⚠️ 注意：数据量不能太大（通常限制在 64KB 以内）。
     */
    inline fun <reified T> saveBitmapWithDataToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        data: T
    ): Uri? {
        return saveBitmapToGalleryInternal(context, bitmap, displayName) { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    val jsonData = Json.encodeToString(data)
                    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, jsonData)
                    exif.saveAttributes()
                }
            }
        }
    }

    /**
     * 使用文件末尾追加法 (EOF Append) 将大容量数据写入图片。
     * 适用于包含 ByteArray 等大数据。
     */
    inline fun <reified T> saveBitmapWithLargeDataToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        data: T
    ): Uri? {
        val jsonData = Json.encodeToString(data)
        val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
        
        return saveBitmapToGalleryInternal(
            context = context,
            bitmap = bitmap,
            displayName = displayName,
            onStreamWrite = { os ->
                // 1. 追加 JSON 数据
                os.write(dataBytes)
                // 2. 追加 4 字节的数据长度（BigEndian）
                val lengthBytes = ByteBuffer.allocate(4).putInt(dataBytes.size).array()
                os.write(lengthBytes)
                // 3. 追加魔术字作为标识
                os.write(MAGIC_WORD.toByteArray(Charsets.UTF_8))
            }
        )
    }

    /**
     * 从图片的 Uri 中读取末尾追加的大容量数据。
     */
    inline fun <reified T> readLargeDataFromUri(context: Context, uri: Uri): T? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val allBytes = inputStream.readBytes()
                val magicBytes = MAGIC_WORD.toByteArray(Charsets.UTF_8)
                val footerSize = magicBytes.size + 4
                
                if (allBytes.size < footerSize) return null
                
                // 1. 验证魔术字
                val magicStart = allBytes.size - magicBytes.size
                val magicFound = allBytes.copyOfRange(magicStart, allBytes.size)
                if (!magicFound.contentEquals(magicBytes)) return null
                
                // 2. 读取长度信息
                val lengthStart = magicStart - 4
                val lengthBuffer = ByteBuffer.wrap(allBytes, lengthStart, 4)
                val dataLength = lengthBuffer.int
                
                // 3. 提取并解析 JSON
                val dataStart = lengthStart - dataLength
                if (dataStart < 0) return null
                val jsonData = String(allBytes, dataStart, dataLength, Charsets.UTF_8)
                Json.decodeFromString<T>(jsonData)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read large data from image")
            null
        }
    }

    /**
     * 内部通用的图片保存逻辑
     */
    @PublishedApi
    internal fun saveBitmapToGalleryInternal(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        onStreamWrite: ((OutputStream) -> Unit)? = null,
        afterSave: ((Uri) -> Unit)? = null
    ): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(imageCollection, contentValues)

        uri?.let {
            try {
                // 1. 写入图片数据及追加数据
                resolver.openOutputStream(it)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        throw Exception("Failed to compress and save bitmap.")
                    }
                    // 执行额外的流写入操作
                    onStreamWrite?.invoke(outputStream)
                    outputStream.flush()
                }

                // 2. 执行额外的保存后操作（如 Exif 写入）
                afterSave?.invoke(it)

                // 3. 更新 Pending 状态
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                return it
            } catch (e: Exception) {
                resolver.delete(it, null, null)
                Timber.e(e, "Failed to save bitmap to gallery internal")
            }
        }
        return null
    }

    /**
     * 将Bitmap转换为ByteArray
     */
    fun bitmapToByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int = 100): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
            outputStream.flush()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将ByteArray转换为Bitmap
     */
    fun byteArrayToBitmap(byteArray: ByteArray, options: BitmapFactory.Options? = null): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从Uri加载Bitmap
     */
    fun loadBitmapFromUri(context: Context, uri: Uri?, useHardware: Boolean = true): Bitmap? {
        return try {
            uri?.let {
                val options = BitmapFactory.Options().apply {
                    // 强制不使用硬件位图
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !useHardware) {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                }
                val scheme = it.scheme
                if (scheme == null || scheme == "file") {
                    // 处理纯文件路径或 file:// 协议，避免调用 ContentResolver
                    BitmapFactory.decodeFile(it.path ?: it.toString(), options)
                } else {
                    // 处理 content:// 等内容提供者协议
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
    * 将Bitmap转换为Base64字符串
    */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
    * 压缩图片到指定大小以下
    */
    fun compressBitmapToLimit(bitmap: Bitmap, maxSize: Int): Bitmap {
        var currentBitmap = bitmap
        var quality = 100

        while (getBitmapSize(currentBitmap) > maxSize && quality > 10) {
            quality -= 10
            currentBitmap = compressBitmapQuality(currentBitmap, quality)
        }

        var scale = 1.0f
        while (getBitmapSize(currentBitmap) > maxSize && scale > 0.1f) {
            scale -= 0.1f
            currentBitmap = scaleBitmap(currentBitmap, scale)
            currentBitmap = compressBitmapQuality(currentBitmap, quality)
        }

        return currentBitmap
    }

    fun getBitmapSize(bitmap: Bitmap): Int {
        return bitmap.allocationByteCount
    }

    fun compressBitmapQuality(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return bitmap.scale(width, height)
    }
}
