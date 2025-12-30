package com.wenchen.yiyi.feature.aiChat.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import com.wenchen.yiyi.App
import com.wenchen.yiyi.core.common.entity.AICharacter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通用图片管理器：管理聊天图片和角色图片
 */
class ImageManager(private val context: Context = App.instance) {

    /**
     * 获取图片存储的根目录
     * @param type 图片类型 ("chatImg", "characterImg")
     */
    private fun getImageRootDir(type: String): File {
        return File(context.getExternalFilesDir(null), type).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 保存图片到指定目录
     *
     * @param rootDir 根目录
     * @param subDir 子目录名
     * @param fileName 文件名
     * @param bitmap 要保存的图片
     * @param format 图片格式
     * @param quality 压缩质量
     * @return 保存成功的文件对象，失败返回null
     */
    private fun saveImage(
        rootDir: File,
        subDir: String,
        fileName: String,
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): File? {
        val dir = File(rootDir, subDir).apply {
            if (!exists()) mkdirs()
        }

        val imageFile = File(dir, fileName)

        return try {
            imageFile.outputStream().use { outputStream ->
                bitmap.compress(format, quality, outputStream)
            }
            imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 生成时间戳
     */
    private fun generateTimeStamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    /**
     * 保存聊天图片到指定会话的文件夹
     * 最终路径：<外部存储应用私有目录>/chatImg/<conversationId>/img_<conversationId>_<时间戳>.jpg
     *
     * @param conversationId 会话ID，用于创建子目录
     * @param bitmap 要保存的图片
     * @param format 图片格式，默认JPEG
     * @param quality 压缩质量，默认100（无损）
     * @return 保存成功的文件对象，失败返回null
     */
    fun saveChatImage(
        conversationId: String,
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): File? {
        val rootDir = getImageRootDir("chatImg")
        val timeStamp = generateTimeStamp()
        val fileExtension = if (format == CompressFormat.PNG) "png" else "jpg"
        val fileName = "img_${conversationId}_${timeStamp}.$fileExtension"

        return saveImage(rootDir, conversationId, fileName, bitmap, format, quality)
    }

    /**
     * 保存角色头像图片
     * 最终路径：<外部存储应用私有目录>/characterImg/avatar/<characterId>_avatar_<时间戳>.jpg
     *
     * @param characterId 角色ID，用于创建子目录
     * @param bitmap 要保存的图片
     * @param format 图片格式，默认JPEG
     * @param quality 压缩质量，默认100（无损）
     * @return 保存成功的文件对象，失败返回null
     */
    fun saveAvatarImage(
        characterId: String,
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): File? {
        val rootDir = getImageRootDir("characterImg")
        val timeStamp = generateTimeStamp()
        val fileExtension = if (format == CompressFormat.PNG) "png" else "jpg"
        val fileName = "${characterId}_avatar_$timeStamp.$fileExtension"

        return saveImage(rootDir, "avatar", fileName, bitmap, format, quality)
    }

    /**
     * 保存角色背景图片
     * 最终路径：<外部存储应用私有目录>/characterImg/background/<characterId>_background_<时间戳>.jpg
     *
     * @param characterId 角色ID，用于创建子目录
     * @param bitmap 要保存的图片
     * @param format 图片格式，默认JPEG
     * @param quality 压缩质量，默认100（无损）
     * @return 保存成功的文件对象，失败返回null
     */
    fun saveBackgroundImage(
        characterId: String,
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): File? {
        val rootDir = getImageRootDir("characterImg")
        val timeStamp = generateTimeStamp()
        val fileExtension = if (format == CompressFormat.PNG) "png" else "jpg"
        val fileName = "${characterId}_background_$timeStamp.$fileExtension"

        return saveImage(rootDir, "background", fileName, bitmap, format, quality)
    }

    /**
     * 获取指定会话的所有图片文件
     * @param conversationId 会话ID
     * @return 按修改时间排序的图片文件列表（最新的在前面）
     */
    fun getChatImages(conversationId: String): List<File> {
        val rootDir = getImageRootDir("chatImg")
        val conversationDir = File(rootDir, conversationId)
        return if (conversationDir.exists() && conversationDir.isDirectory) {
            conversationDir.listFiles()?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 获取角色头像文件
     * @param characterId 角色ID
     * @return 最新的头像文件对象，如果不存在返回null
     */
    fun getAvatarImage(characterId: String): File? {
        val rootDir = getImageRootDir("characterImg")
        val avatarDir = File(rootDir, "avatar")
        return if (avatarDir.exists() && avatarDir.isDirectory) {
            avatarDir.listFiles()?.find { it.name.startsWith("${characterId}_avatar_") }
        } else {
            null
        }
    }

    /**
     * 获取角色背景文件
     * @param characterId 角色ID
     * @return 最新的背景文件对象，如果不存在返回null
     */
    fun getBackgroundImage(characterId: String): File? {
        val rootDir = getImageRootDir("characterImg")
        val backgroundDir = File(rootDir, "background")
        return if (backgroundDir.exists() && backgroundDir.isDirectory) {
            backgroundDir.listFiles()?.find { it.name.startsWith("${characterId}_background_") }
        } else {
            null
        }
    }

    /**
     * 删除指定会话的单张图片
     * @param conversationId 会话ID
     * @param fileName 图片文件名
     * @return 是否删除成功
     */
    fun deleteChatImage(conversationId: String, fileName: String): Boolean {
        val rootDir = getImageRootDir("chatImg")
        val imageFile = File(File(rootDir, conversationId), fileName)
        return if (imageFile.exists() && imageFile.isFile) {
            imageFile.delete()
        } else {
            false
        }
    }

    /**
     * 删除指定会话的所有图片
     * @param conversationId 会话ID
     * @return 是否删除成功
     */
    fun deleteAllChatImages(conversationId: String): Boolean {
        val rootDir = getImageRootDir("chatImg")
        val conversationDir = File(rootDir, conversationId)
        return if (conversationDir.exists() && conversationDir.isDirectory) {
            conversationDir.deleteRecursively()
        } else {
            true
        }
    }

    /**
     * 删除角色头像图片
     * @param characterId 角色ID
     * @return 是否删除成功
     */
    fun deleteAvatarImage(characterId: String): Boolean {
        val avatarFile = getAvatarImage(characterId)
        return avatarFile?.delete() != false
    }

    /**
     * 删除角色背景图片
     * @param characterId 角色ID
     * @return 是否删除成功
     */
    fun deleteBackgroundImage(characterId: String): Boolean {
        val backgroundFile = getBackgroundImage(characterId)
        return backgroundFile?.delete() != false
    }

    /**
     * 删除角色所有相关图片
     * @param character 角色对象
     * @return 是否删除成功
     */
    fun deleteAllCharacterImages(character: AICharacter): Boolean {
        return deleteAvatarImage(character.aiCharacterId) &&
                deleteBackgroundImage(character.aiCharacterId)
    }

    /**
     * 获取图片文件的完整路径（聊天图片）
     * @param conversationId 会话ID
     * @param fileName 图片文件名
     * @return 完整路径字符串
     */
    fun getChatImagePath(conversationId: String, fileName: String): String {
        val rootDir = getImageRootDir("chatImg")
        return File(File(rootDir, conversationId), fileName).absolutePath
    }

    /**
     * 获取头像图片的完整路径
     * @param characterId 角色ID
     * @return 完整路径字符串
     */
    fun getAvatarImagePath(characterId: String): String? {
        return getAvatarImage(characterId)?.absolutePath
    }

    /**
     * 获取背景图片的完整路径
     * @param characterId 角色ID
     * @return 完整路径字符串
     */
    fun getBackgroundImagePath(characterId: String): String? {
        return getBackgroundImage(characterId)?.absolutePath
    }

    /**
     * 获取指定会话的图片存储目录
     * @param conversationId 会话ID
     * @return 目录文件对象
     */
    fun getChatImageDir(conversationId: String): File {
        val rootDir = getImageRootDir("chatImg")
        return File(rootDir, conversationId).apply {
            if (!exists()) mkdirs()
        }
    }
}
