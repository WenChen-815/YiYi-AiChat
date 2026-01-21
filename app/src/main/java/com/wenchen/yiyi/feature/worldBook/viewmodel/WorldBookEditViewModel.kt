package com.wenchen.yiyi.feature.worldBook.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookEntryRepository
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookRepository
import com.wenchen.yiyi.core.database.entity.YiYiWorldBook
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.ui.ToastUtils
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WorldBookEditViewModel @Inject constructor(
    private val worldBookRepository: YiYiWorldBookRepository,
    private val worldBookEntryRepository: YiYiWorldBookEntryRepository,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
    private val route = savedStateHandle.toRoute<WorldBookRoutes.WorldBookEdit>()

    private val _worldId = MutableStateFlow(route.worldId.ifEmpty { NanoIdUtils.randomNanoId() })
    val worldId: StateFlow<String> = _worldId.asStateFlow()

    private val _isNewWorld = MutableStateFlow(route.isNewWorld)
    val isNewWorld: StateFlow<Boolean> = _isNewWorld.asStateFlow()

    private val _worldBook = MutableStateFlow<YiYiWorldBook?>(null)
    val worldBook: StateFlow<YiYiWorldBook?> = _worldBook.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<YiYiWorldBookEntry>> = _worldId
        .flatMapLatest { id ->
            worldBookEntryRepository.getEntriesByBookIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (_isNewWorld.value) {
            // 如果是新世界，先不插入数据库，在 UI 层管理 metadata，直到点保存
            // 或者为了支持在新建时就添加条目，我们需要先创建一个占位符
            _worldBook.value = YiYiWorldBook(id = _worldId.value, name = "")
        } else {
            loadWorldBook()
        }
    }

    private fun loadWorldBook() {
        viewModelScope.launch {
            val book = worldBookRepository.getBookById(_worldId.value)
            if (book != null) {
                _worldBook.value = book
            }
        }
    }

    fun saveWorldBook(name: String, description: String) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    ToastUtils.showToast("名字不能为空")
                    return@launch
                }
                val now = System.currentTimeMillis()
                val book = YiYiWorldBook(
                    id = _worldId.value,
                    name = name,
                    description = description,
                    updateTime = now,
                    createTime = _worldBook.value?.createTime ?: now
                )
                worldBookRepository.insertBook(book)
                ToastUtils.showToast("保存成功")
                navigateBack()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save world book")
                ToastUtils.showToast("保存失败")
            }
        }
    }

    fun deleteWorldBook() {
        viewModelScope.launch {
            try {
                _worldBook.value?.let {
                    worldBookRepository.deleteBook(it)
                    ToastUtils.showToast("删除成功")
                    navigateBack()
                }
            } catch (e: Exception) {
                ToastUtils.showToast("删除失败")
            }
        }
    }

    fun toggleEntryEnabled(entry: YiYiWorldBookEntry, enabled: Boolean) {
        viewModelScope.launch {
            worldBookEntryRepository.updateEntry(entry.copy(enabled = enabled))
        }
    }

    fun deleteEntry(entry: YiYiWorldBookEntry) {
        viewModelScope.launch {
            worldBookEntryRepository.deleteEntry(entry)
        }
    }

    fun navigateToEntryEdit(entryId: String? = null) {
        // 在跳转到条目编辑前，如果这本书还没在数据库里（新建流程），需要先插入一个占位符，
        // 否则外键约束会报错。
        viewModelScope.launch {
            val existing = worldBookRepository.getBookById(_worldId.value)
            if (existing == null) {
                val now = System.currentTimeMillis()
                worldBookRepository.insertBook(
                    YiYiWorldBook(
                        id = _worldId.value,
                        name = "未命名世界",
                        createTime = now,
                        updateTime = now
                    )
                )
            }
            navigate(
                WorldBookRoutes.WorldBookEntryEdit(
                    worldId = _worldId.value,
                    entryId = entryId
                )
            )
        }
    }
}
