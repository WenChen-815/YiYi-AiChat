package com.wenchen.yiyi.feature.worldBook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.storage.FilesUtils
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WorldBookListViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
){
    init {
        cleanAbnormalWorldBookFiles()
    }
    /**
     * 清理异常命名的世界文件
     * 异常文件通常是指文件名包含 @ 符号和哈希值的文件（如 le.c0@a7a2c49.json）
     * 这些文件是由于之前的 bug 导致的错误命名
     */
    private fun cleanAbnormalWorldBookFiles() {
        val directoryPath = "world_book"
        val directory = File(Application.instance.applicationContext?.filesDir, directoryPath)

        // 检查目录是否存在且为目录
        if (!directory.exists() || !directory.isDirectory) {
            return
        }

        // 查找所有符合异常模式的文件（包含 @ 符号的 .json 文件）
        val abnormalFiles = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".json") && file.name.contains('@')
        }

        var deletedCount = 0
        abnormalFiles?.forEach { file ->
            try {
                if (file.delete()) {
                    deletedCount++
                    Timber.d("已删除异常世界文件: ${file.name}")
                } else {
                    Timber.e("删除异常世界文件失败: ${file.name}")
                }
            } catch (e: Exception) {
                Timber.e(e, "删除异常世界文件时发生错误: ${file.name}")
            }
        }

        if (deletedCount > 0) {
            Timber.d("清理完成，共删除了 $deletedCount 个异常世界文件")
        }
    }
    // 从文件加载世界书列表
    fun loadWorldBooks(
        adapter: JsonAdapter<WorldBook>,
        onLoaded: (List<WorldBook>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val worldBookFiles = FilesUtils.listFileNames("world_book")
        Timber.tag("WorldBookListActivity").d("loadWorldBooks: $worldBookFiles")
        val worldBooks = mutableListOf<WorldBook>()

        worldBookFiles.forEach { fileName ->
            try {
                val json = FilesUtils.readFile("world_book/$fileName")
                val worldBook = adapter.fromJson(json)
                worldBook?.let { worldBooks.add(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onLoaded(worldBooks)
    }
}