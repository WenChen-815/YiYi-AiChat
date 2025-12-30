package com.wenchen.yiyi.core.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

object BitMapUtil {
    /**
    * 将Bitmap转换为Base64字符串
    */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos) // 压缩图片质量为100%
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
    * 压缩图片到指定大小以下
    */
    fun compressBitmapToLimit(bitmap: Bitmap, maxSize: Int): Bitmap {
        var currentBitmap = bitmap
        var quality = 100

        // 首先尝试通过调整质量来压缩
        while (getBitmapSize(currentBitmap) > maxSize && quality > 10) {
            quality -= 10
            currentBitmap = compressBitmapQuality(currentBitmap, quality)
        }

        // 如果仍然太大，则缩放尺寸
        var scale = 1.0f
        while (getBitmapSize(currentBitmap) > maxSize && scale > 0.1f) {
            scale -= 0.1f
            currentBitmap = scaleBitmap(currentBitmap, scale)
            // 重新压缩质量
            currentBitmap = compressBitmapQuality(currentBitmap, quality)
        }

        return currentBitmap
    }

    /**
     * 获取Bitmap的大致字节大小
     */
    fun getBitmapSize(bitmap: Bitmap): Int {
        return bitmap.allocationByteCount
    }

    /**
     * 按质量压缩Bitmap
     */
    fun compressBitmapQuality(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    /**
     * 按比例缩放Bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return bitmap.scale(width, height)
    }
}