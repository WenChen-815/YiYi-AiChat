package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.YiYiWorldBookEntryRepository
import com.wenchen.yiyi.core.database.entity.YiYiWorldBookEntry
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorldBookEntryEditViewModel @Inject constructor(
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
    private val route = savedStateHandle.toRoute<WorldBookRoutes.WorldBookEntryEdit>()
    private val worldId = route.worldId
    private val entryId = route.entryId

    private val _entryState = MutableStateFlow(YiYiWorldBookEntry(
        entryId = entryId ?: NanoIdUtils.randomNanoId(),
        bookId = worldId,
        keys = emptyList(),
        content = ""
    ))
    val entryState: StateFlow<YiYiWorldBookEntry> = _entryState.asStateFlow()

    private val _showDiscardDialog = MutableStateFlow(false)
    val showDiscardDialog: StateFlow<Boolean> = _showDiscardDialog.asStateFlow()

    private var initialEntry: YiYiWorldBookEntry? = null

    init {
        if (entryId != null) {
            loadEntry(entryId)
        } else {
            initialEntry = _entryState.value
        }
    }

    private fun loadEntry(id: String) {
        viewModelScope.launch {
            val result = worldBookEntryRepository.getEntryById(id)
            result?.let {
                initialEntry = it
                _entryState.value = it
            }
        }
    }

    fun updateState(updater: (YiYiWorldBookEntry) -> YiYiWorldBookEntry) {
        _entryState.update(updater)
    }

    private fun isChanged(): Boolean {
        return _entryState.value != initialEntry
    }

    private fun isValid(): Boolean {
        val current = _entryState.value
        return !current.name.isNullOrBlank() && !current.content.isNullOrBlank()
    }

    fun handleBack() {
        if (!isChanged()) {
            navigateBack()
            return
        }

        if (isValid()) {
            saveAndExit()
        } else {
            _showDiscardDialog.value = true
        }
    }

    private fun saveAndExit() {
        viewModelScope.launch {
            val current = _entryState.value
            if (entryId == null) {
                worldBookEntryRepository.insertEntry(current)
            } else {
                worldBookEntryRepository.updateEntry(current)
            }
            navigateBack()
        }
    }

    fun discardAndExit() {
        _showDiscardDialog.value = false
        navigateBack()
    }

    fun dismissDiscardDialog() {
        _showDiscardDialog.value = false
    }
}
