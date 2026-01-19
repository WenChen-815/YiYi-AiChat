package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.YiYiRegexGroupRepository
import com.wenchen.yiyi.core.data.repository.YiYiRegexScriptRepository
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.ConfigRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegexDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    private val regexScriptRepository: YiYiRegexScriptRepository,
    private val regexGroupRepository: YiYiRegexGroupRepository
) : BaseViewModel(navigator, userState, userConfigState) {

    private val routeData = savedStateHandle.toRoute<ConfigRoutes.RegexDetail>()
    val groupId = routeData.groupId

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    init {
        viewModelScope.launch {
            regexGroupRepository.getGroupById(groupId)?.let {
                _groupName.value = it.name ?: ""
            }
        }
    }

    val regexScripts: StateFlow<List<YiYiRegexScript>> = regexScriptRepository.getScriptsByGroupIdFlow(groupId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateGroupName(newName: String) {
        _groupName.value = newName
        viewModelScope.launch {
            regexGroupRepository.getGroupById(groupId)?.let { group ->
                regexGroupRepository.updateGroup(group.copy(name = newName))
            }
        }
    }

    fun addOrUpdateScript(script: YiYiRegexScript) {
        viewModelScope.launch {
            if (script.id.isBlank()) {
                val newScript = script.copy(id = NanoIdUtils.randomNanoId(), groupId = groupId)
                regexScriptRepository.insertScript(newScript)
            } else {
                regexScriptRepository.updateScript(script)
            }
        }
    }

    fun deleteScript(script: YiYiRegexScript) {
        viewModelScope.launch {
            regexScriptRepository.deleteScript(script)
        }
    }
}
