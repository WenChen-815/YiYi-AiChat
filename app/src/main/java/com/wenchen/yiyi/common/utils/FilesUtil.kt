package com.wenchen.yiyi.common.utils

import com.wenchen.yiyi.common.App
import java.io.File
import java.io.FileWriter

object FilesUtil {
    private val app: App = App.instance

    fun deleteFile(fileName: String): Boolean {
        return try {
            val file = File(app.applicationContext?.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 保存到内部存储的files目录
    fun saveToFile(message: String, fileName: String): String {
        try {
            // 获取内部存储文件路径
            val file = File(app.applicationContext?.filesDir, fileName)
            ensureFileExists(file)
            // 写入文件(覆盖模式)
            FileWriter(file, false).use { it.write(message) }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    fun appendToFile(message: String, fileName: String) {
        try {
            val file = File(app.applicationContext?.filesDir, fileName)
            ensureFileExists(file)
            // 在末尾新的一行添加内容，true为追加模式
            /*
            use是 Kotlin 提供的扩展函数，专门用于资源自动释放（类似 Java 的 try-with-resources 语法）。
            作用：在 lambda 表达式执行完毕后（无论正常结束还是抛出异常），会自动调用资源的close()方法，确保文件流被关闭，避免资源泄漏（比如文件被长期占用无法删除）。
            lambda 中的writer：是FileWriter的实例，通过它可以调用写入方法。
            */
            FileWriter(file, true).use { writer ->
                writer.append(message)
                writer.append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 读取文件所有内容
    fun readFile(fileName: String): String {
        val file = File(app.applicationContext?.filesDir, fileName)
        if (!file.exists()) {
            return ""
        }
        /*
        readText()详解
        readText()是Kotlin标准库中File类的扩展函数，具有以下特点：
        功能：一次性读取整个文件的内容为字符串
        编码：默认使用UTF-8编码
        实现原理：内部使用FileInputStream打开文件，将文件内容读入内存，自动关闭文件流，返回完整的字符串内容
        优点：使用简单，一行代码即可读取整个文件，自动资源管理，无需手动关闭流，异常处理封装
        注意事项：适用于小文件读取，对于大文件可能导致内存问题，读取失败时会抛出IOException
         */
        return file.readText()
    }

    /**
     * 获取指定目录下所有文件的名称
     * @param directoryPath 相对于应用files目录的路径
     * @return 文件名列表
     */
    fun listFileNames(directoryPath: String): List<String> {
        val directory = File(app.applicationContext?.filesDir, directoryPath)

        // 检查目录是否存在且为目录
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        // 获取目录下所有文件并过滤出文件（非目录）
        return directory.listFiles { file -> file.isFile }
            ?.map { file -> file.name }
            ?: emptyList()
    }

    /**
     * 获取指定目录下所有文件的完整路径
     * @param directoryPath 相对于应用files目录的路径
     * @return 文件完整路径列表
     */
    fun listFilePaths(directoryPath: String): List<String> {
        val directory = File(app.applicationContext?.filesDir, directoryPath)

        // 检查目录是否存在且为目录
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        // 获取目录下所有文件并过滤出文件（非目录）
        return directory.listFiles { file -> file.isFile }
            ?.map { file -> file.absolutePath }
            ?: emptyList()
    }
    /**
     * 确保文件及其父目录存在，如果不存在则创建
     * @param file 需要确保存在的文件
     */
    private fun ensureFileExists(file: File) {
        // 确保父目录存在，如果不存在则逐级创建
        file.parentFile?.also { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        if (!file.exists()) {
            file.createNewFile()
        }
    }
}