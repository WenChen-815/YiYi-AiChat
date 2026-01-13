package com.wenchen.yiyi.feature.worldBook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.util.storage.FilesUtil
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.feature.worldBook.model.WorldBook
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
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
    // 从文件加载世界书列表
    fun loadWorldBooks(
        adapter: JsonAdapter<WorldBook>,
        onLoaded: (List<WorldBook>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val worldBookFiles = FilesUtil.listFileNames("world_book")
        Timber.tag("WorldBookListActivity").d("loadWorldBooks: $worldBookFiles")
        val worldBooks = mutableListOf<WorldBook>()

        worldBookFiles.forEach { fileName ->
            try {
                val json = FilesUtil.readFile("world_book/$fileName")
                val worldBook = adapter.fromJson(json)
                worldBook?.let { worldBooks.add(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onLoaded(worldBooks)
    }
}