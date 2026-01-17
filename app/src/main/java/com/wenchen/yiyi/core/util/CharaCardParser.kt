package com.wenchen.yiyi.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

object CharaCardParser {

    // 定义一个魔术字，用于识别追加的数据
    const val MAGIC_WORD = "YIYI_DATA"

    /**
     * 从 PNG 文件的 tEXt 或 iTXt 块中提取 "chara" 数据
     * 返回原始的 JSON 字符串
     */
    fun parseTavernCard(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                extractCharaChunk(inputStream)
            }
        } catch (e: Exception) {
            Timber.e(e, "解析角色卡失败")
            null
        }
    }

    private fun extractCharaChunk(inputStream: InputStream): String? {
        val bytes = inputStream.readBytes()
        val buffer = ByteBuffer.wrap(bytes)

        // 检查 PNG 签名
        val signature = ByteArray(8)
        buffer.get(signature)
        if (!signature.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) {
            return null
        }

        while (buffer.hasRemaining()) {
            val length = try { buffer.int } catch (e: Exception) { return null }
            val type = ByteArray(4)
            if (buffer.remaining() < 4) return null
            buffer.get(type)
            val typeStr = String(type, StandardCharsets.US_ASCII)

            if (typeStr == "tEXt" || typeStr == "iTXt") {
                if (buffer.remaining() < length) return null
                val data = ByteArray(length)
                buffer.get(data)
                val chunkContent = String(data, StandardCharsets.UTF_8)

                // 酒馆角色卡通常以 "chara\u0000" 开头
                if (chunkContent.startsWith("chara")) {
                    val base64Data = if (typeStr == "tEXt") {
                        chunkContent.substringAfter("chara\u0000")
                    } else {
                        // iTXt 格式稍微复杂一点，跳过 null 分隔符和压缩标志
                        val nullIndex = data.indexOf(0x00)
                        if (nullIndex != -1) {
                            // iTXt: keyword (chara) + null + compression flag + compression method + lang tag + null + translated keyword + null + text
                            // 简单起见，找最后一个 null 之后的内容
                            val lastNullIndex = data.lastIndexOf(0x00)
                            String(data.sliceArray(lastNullIndex + 1 until data.size), StandardCharsets.UTF_8)
                        } else ""
                    }

                    return try {
                        String(Base64.decode(base64Data, Base64.DEFAULT), StandardCharsets.UTF_8)
                    } catch (e: Exception) {
                        // 有些版本可能没经过 Base64 编码，直接返回
                        base64Data
                    }
                }
                if (buffer.remaining() < 4) return null
                buffer.get(ByteArray(4)) // 跳过 CRC
            } else {
                if (buffer.remaining() < length + 4) return null
                buffer.position(buffer.position() + length + 4) // 跳过数据和 CRC
            }
        }
        return null
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
        return BitMapUtil.saveBitmapToGalleryInternal(context, bitmap, displayName) { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    val jsonData = AppJson.encodeToString(data)
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
        val jsonData = AppJson.encodeToString(data)
        val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
        
        return BitMapUtil.saveBitmapToGalleryInternal(
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
     * 使用 PNG tEXt 块写入数据，并可选在文件末尾追加大数据。
     */
    inline fun <reified T, reified E> saveBitmapWithTavernCardAndLargeDataToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        tavernData: T,
        extraLargeData: E
    ): Uri? {
        val tavernJson = AppJson.encodeToString(tavernData)
        val base64Data = Base64.encodeToString(tavernJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val payload = "chara\u0000$base64Data".toByteArray(Charsets.UTF_8)
        
        val largeDataJson = AppJson.encodeToString(extraLargeData)
        val largeDataBytes = largeDataJson.toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val pngBytes = baos.toByteArray()
        
        val pngWithTavern = injectPngChunk(pngBytes, "tEXt", payload)
        
        // 最终拼接：PNG(含块) + LargeData + Length(4) + MagicWord
        val footerBytes = ByteBuffer.allocate(largeDataBytes.size + 4 + MAGIC_WORD.length)
            .put(largeDataBytes)
            .putInt(largeDataBytes.size)
            .put(MAGIC_WORD.toByteArray(Charsets.UTF_8))
            .array()

        return BitMapUtil.saveBytesToGallery(context, pngWithTavern + footerBytes, displayName)
    }

    /**
     * 与 saveBitmapWithTavernCardAndLargeDataToGallery 对应的读取方法。
     * 同时读取 PNG 块中的标准酒馆数据和文件末尾 (EOF) 追加的大容量 YiYi 扩展数据。
     *
     * @param T 标准角色卡数据类型 (如 CharaCardV3)
     * @param E 扩展大容量数据类型 (如 CharaExtensionModel)
     * @return 返回包含两部分数据的 Pair
     */
    inline fun <reified T, reified E> readTavernCardAndLargeDataFromUri(
        context: Context,
        uri: Uri
    ): Pair<T?, E?> {
        // 1. 调用 parseTavernCard 解析 PNG 块中的标准酒馆 JSON 字符串
        val tavernJson = parseTavernCard(context, uri)
        val tavernData = tavernJson?.let {
            try {
                // 使用 ignoreUnknownKeys 增强兼容性
                AppJson.decodeFromString<T>(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode tavern block data")
                null
            }
        }

        // 2. 调用 readLargeDataFromUri 读取文件末尾追加的 YiYi 特有数据
        val largeData = readLargeDataFromUri<E>(context, uri)

        return Pair(tavernData, largeData)
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
                AppJson.decodeFromString<T>(jsonData)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read large data from image")
            null
        }
    }

    /**
     * 将自定义 PNG 块注入到 IEND 块之前。
     */
    fun injectPngChunk(pngBytes: ByteArray, type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(StandardCharsets.US_ASCII)
        val length = data.size
        
        val chunkBuffer = ByteBuffer.allocate(4 + 4 + length + 4)
        chunkBuffer.putInt(length)
        chunkBuffer.put(typeBytes)
        chunkBuffer.put(data)
        
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        chunkBuffer.putInt(crc.value.toInt())
        val newChunk = chunkBuffer.array()
        
        // 查找 IEND 块的位置以便在其之前插入
        val iendType = "IEND".toByteArray(StandardCharsets.US_ASCII)
        var iendOffset = -1
        for (i in pngBytes.size - 8 downTo 0) {
            if (pngBytes[i] == iendType[0] && pngBytes[i+1] == iendType[1] &&
                pngBytes[i+2] == iendType[2] && pngBytes[i+3] == iendType[3]) {
                iendOffset = i - 4 // 长度字段位于类型字段前 4 字节
                break
            }
        }
        
        return if (iendOffset != -1) {
            val result = ByteArray(pngBytes.size + newChunk.size)
            System.arraycopy(pngBytes, 0, result, 0, iendOffset)
            System.arraycopy(newChunk, 0, result, iendOffset, newChunk.size)
            System.arraycopy(pngBytes, iendOffset, result, iendOffset + newChunk.size, pngBytes.size - iendOffset)
            result
        } else {
            pngBytes + newChunk
        }
    }
}
