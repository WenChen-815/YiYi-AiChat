package com.wenchen.yiyi.feature.worldBook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
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
import java.util.UUID
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

    val moshi: Moshi = Moshi.Builder().build()
    val worldBookAdapter: JsonAdapter<WorldBook> = moshi.adapter(WorldBook::class.java)

    init {
        val route = savedStateHandle.toRoute<WorldBookRoutes.WorldBookEdit>()
        _isNewWorld.value = route.isNewWorld
        if (isNewWorld.value) {
            _worldId.value = UUID.randomUUID().toString()
        } else {
            _worldId.value = route.worldId
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
                val filePath = FilesUtil.saveToFile(json, "world_book/$worldId.json")
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