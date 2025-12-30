package com.wenchen.yiyi.core.state

import com.wenchen.yiyi.core.state.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户状态管理类(暂无具体实现)
 *
 * 该类负责管理整个应用的全局状态，包括：
 * - 用户登录状态
 * - 用户认证信息（token等）
 * - 用户个人信息
 *
 * 通过StateFlow提供响应式的状态管理，任何组件都可以订阅状态变化
 *
 * @param authStoreRepository 认证信息存储仓库
 * @param userInfoStoreRepository 用户信息存储仓库
 * @param userInfoRepository 用户信息网络仓库
 * @param applicationScope 应用级协程作用域
 */
@Singleton
class UserState @Inject constructor(
//    private val authStoreRepository: AuthStoreRepository,
//    private val userInfoStoreRepository: UserInfoStoreRepository,
//    private val userInfoRepository: UserInfoRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    // 用户登录状态
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
}
