package com.wenchen.yiyi.feature.main.viewmodel

import com.wenchen.yiyi.core.base.viewmodel.BaseViewModel
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

/**
 * 主界面 ViewModel
 *
 * @param navigator 导航控制器
 * @param userState 全局用户状态
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    navigator: AppNavigator,
    userConfigState: UserConfigState,
    userState: UserState,
) : BaseViewModel(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
}
