package com.wenchen.yiyi.feature.config.viewmodel

import androidx.lifecycle.viewModelScope
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.data.repository.YiYiRegexGroupRepository
import com.wenchen.yiyi.core.data.repository.YiYiRegexScriptRepository
import com.wenchen.yiyi.core.database.entity.YiYiRegexGroup
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
class RegexConfigViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
    private val regexGroupRepository: YiYiRegexGroupRepository,
    private val regexScriptRepository: YiYiRegexScriptRepository
) : BaseViewModel(navigator, userState, userConfigState) {

    val regexGroups: StateFlow<List<YiYiRegexGroup>> = regexGroupRepository.getAllGroupsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGroup(name: String, description: String) {
        viewModelScope.launch {
            val newGroup = YiYiRegexGroup(
                id = NanoIdUtils.randomNanoId(),
                name = name,
                description = description,
                createTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis()
            )
            regexGroupRepository.insertGroup(newGroup)
        }
    }

    fun deleteGroup(group: YiYiRegexGroup) {
        viewModelScope.launch {
            regexGroupRepository.deleteGroup(group)
            regexScriptRepository.deleteScriptsByGroupId(group.id)
        }
    }

    fun navigateToDetail(groupId: String, groupName: String) {
        navigate(ConfigRoutes.RegexDetail(groupId))
    }
}
