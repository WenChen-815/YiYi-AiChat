package com.wenchen.yiyi.core.base.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import com.wenchen.learn.navigation.NavigationResultKey
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import com.wenchen.yiyi.navigation.RouteInterceptor
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 基础ViewModel（类型安全版本）
 *
 * 提供所有ViewModel通用的功能：
 * 1. 类型安全的导航
 * 2. 路由拦截（登录检查）
 * 3. 类型安全的结果返回
 *
 * @param navigator 导航控制器
 * @param userState 应用状态
 * @param routeInterceptor 路由拦截器
 */
abstract class BaseViewModel(
    protected val navigator: AppNavigator,
    protected val userState: UserState,
    protected val routeInterceptor: RouteInterceptor = RouteInterceptor()
) : ViewModel() {

    // ==================== 基础导航方法 ====================

    /**
     * 导航到指定路由（类型安全）
     * 自动处理登录拦截逻辑
     *
     * @param route 目标路由对象（必须是 @Serializable）
     * @param navOptions 导航选项（可选）
     */
    fun navigate(route: Any, navOptions: NavOptions? = null) {
        viewModelScope.launch {
            val targetRoute = checkRouteInterception(route)
            navigator.navigateTo(targetRoute, navOptions)
        }
    }

    /**
     * 导航到指定路由并关闭当前页面
     * 自动处理登录拦截逻辑
     *
     * @param route 目标路由对象
     * @param currentRoute 当前页面路由对象，将被关闭
     */
    fun navigateAndCloseCurrent(route: Any, currentRoute: Any) {
        viewModelScope.launch {
            val targetRoute = checkRouteInterception(route)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    route = currentRoute,
                    inclusive = true,  // 设为true表示当前页面也会被弹出
                    saveState = false  // 不保存状态
                )
                .build()
            navigator.navigateTo(targetRoute, navOptions)
        }
    }

    // ==================== 返回导航方法 ====================

    /**
     * 返回上一页
     */
    fun navigateBack() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    /**
     * 返回上一页并携带类型安全的结果（使用 NavigationResultKey）
     *
     * 这是 V3.2 版本的最终方案，实现了端到端的类型安全。
     *
     * @param key 类型安全的结果 Key
     * @param result 要传递的结果对象
     *
     */
    fun <T> popBackStackWithResult(key: NavigationResultKey<T>, result: T) {
        viewModelScope.launch {
            navigator.popBackStackWithResult(key, result)
        }
    }

    /**
     * 返回到指定路由
     *
     * @param route 目标路由对象
     * @param inclusive 是否包含目标路由本身
     */
    fun navigateBackTo(route: Any, inclusive: Boolean = false) {
        viewModelScope.launch {
            navigator.navigateBackTo(route, inclusive)
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 检查路由是否需要登录拦截（类型安全）
     *
     * @param route 目标路由对象
     * @return 如果需要拦截返回登录页面路由，否则返回原路由
     */
    private fun checkRouteInterception(route: Any): Any {
        return if (routeInterceptor.requiresLogin(route) && !userState.isLoggedIn.value) {
            Timber.d("需要登录但未登录，跳转到登录页面：$route")
            // 需要登录但未登录，跳转到登录页面
            routeInterceptor.getLoginRoute()
        } else {
            // 不需要登录或已登录，正常跳转
            route
        }
    }
}
