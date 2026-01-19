package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.YiYiRegexScriptRepository
import com.wenchen.yiyi.core.database.entity.YiYiRegexScript
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.routes.ConfigRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegexDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    private val regexScriptRepository: YiYiRegexScriptRepository
) : BaseViewModel(navigator, userState, userConfigState) {

    private val routeData = savedStateHandle.toRoute<ConfigRoutes.RegexDetail>()
    val groupId = routeData.groupId

    val regexScripts: StateFlow<List<YiYiRegexScript>> = regexScriptRepository.getScriptsByGroupIdFlow(groupId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
