package com.wenchen.yiyi.feature.output.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.util.business.CharaCardParser
import com.wenchen.yiyi.core.util.common.PrettyJson
import com.wenchen.yiyi.core.util.ui.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import timber.log.Timber

class CardParserViewModel : ViewModel() {
    private val _rawJson = MutableStateFlow<String?>(null)
    val rawJson = _rawJson.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)




    fun parseImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isParsing.value = true
            // 获取文件名（去掉扩展名）
            val fileName = getFileNameFromUri(context, uri)?.substringBeforeLast(".") ?: "character_card"
            _fileName.value = fileName
            
            val result = withContext(Dispatchers.IO) {
                val raw = CharaCardParser.parseTavernCard(context, uri)
                raw?.let { formatJson(it) }
            }
            _rawJson.value = result ?: "解析失败：未找到角色卡数据或文件格式错误"
            _isParsing.value = false
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    /**
     * 格式化 JSON 字符串，增加缩进
     */
    private fun formatJson(jsonString: String): String {
        return try {
            val jsonElement = PrettyJson.decodeFromString<JsonElement>(jsonString)
            PrettyJson.encodeToString(JsonElement.serializer(), jsonElement)
        } catch (e: Exception) {
            jsonString
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveJsonToDownloads(context: Context) {
        val jsonContent = _rawJson.value ?: return
        if (jsonContent.startsWith("解析失败")) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = "${_fileName.value ?: "character"}.json"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(jsonContent.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        ToastUtils.showToast("已保存到下载目录: $name")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "保存文件失败")
                withContext(Dispatchers.Main) {
                    ToastUtils.showToast("保存失败: ${e.message}")
                }
            }
        }
    }
}
