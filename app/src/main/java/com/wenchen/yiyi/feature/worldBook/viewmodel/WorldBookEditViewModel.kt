package com.wenchen.yiyi.feature.worldBook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.wenchen.yiyi.Application
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.storage.FilesUtil
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.toast.ToastUtils
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import com.wenchen.yiyi.feature.worldBook.model.WorldBookItem
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WorldBookEditViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
){
    val _worldId = MutableStateFlow("")
    var worldId: StateFlow<String> = _worldId.asStateFlow()

    val _isNewWorld = MutableStateFlow(false)
    val isNewWorld: StateFlow<Boolean> = _isNewWorld.asStateFlow()

    val _worldBook = MutableStateFlow<WorldBook?>(null)
    val worldBook: StateFlow<WorldBook?> = _worldBook.asStateFlow()

    val moshi: Moshi = Moshi.Builder().build()
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)

    init {
        val route = savedStateHandle.toRoute<WorldBookRoutes.WorldBookEdit>()
        _isNewWorld.value = route.isNewWorld
        if (isNewWorld.value) {
            _worldId.value = NanoIdUtils.randomNanoId()
            // 对于新世界，初始化一个空的 WorldBook
            _worldBook.value = WorldBook(
                id = _worldId.value,
                worldName = "",
                worldDesc = "",
                worldItems = emptyList()
            )
        } else {
            _worldId.value = route.worldId
            loadWorldBook()
        }
    }

    fun loadWorldBook() {
        viewModelScope.launch {
            try {
                if (!isNewWorld.value) {
                    val json = FilesUtil.readFile("world_book/${worldId.value}.json")
                    Timber.tag("WorldBookEditViewModel").d("Loading world book json: $json")
                    val loadedWorldBook = worldBookAdapter.fromJson(json)

                    if (loadedWorldBook != null) {
                        _worldBook.value = loadedWorldBook
                    } else {
                        // 如果文件不存在或解析失败，创建默认的世界书
                        val worldList = FilesUtil.listFileNames("world_book")
                        _worldBook.value = WorldBook(
                            id = worldId.value,
                            worldName = "无名世界${worldList.size + 1}",
                            worldDesc = "",
                            worldItems = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load world book")
                // 发生错误时创建默认的世界书
                val worldList = FilesUtil.listFileNames("world_book")
                _worldBook.value = WorldBook(
                    id = worldId.value,
                    worldName = "无名世界${worldList.size + 1}",
                    worldDesc = "",
                    worldItems = emptyList()
                )
            }
        }
    }

    fun saveWorldBook(
        name: String,
        description: String,
        worldItems: List<WorldBookItem>
    ) {
        viewModelScope.launch {
            try {
                val worldBook = WorldBook(
                    id = worldId.value,
                    worldName = name,
                    worldDesc = description,
                    worldItems = worldItems
                )
                val json = worldBookAdapter.toJson(worldBook)
                val filePath = FilesUtil.saveToFile(json, "world_book/${worldId.value}.json")
                if (filePath.isNotEmpty()) {
                    Timber.tag("WorldBookEditActivity").d("saveWorldBook: $filePath")
                }
                ToastUtils.showToast("世界保存成功")
                navigateBack()
            } catch (e: Exception) {
                ToastUtils.showToast("世界保存失败: ${e.message}")
            }
        }
    }
}