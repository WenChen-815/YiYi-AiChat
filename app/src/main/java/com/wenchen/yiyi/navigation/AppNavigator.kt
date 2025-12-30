package com.wenchen.yiyi.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.wenchen.learn.navigation.NavigationResultKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航管理器
 */
@Singleton
class AppNavigator @Inject constructor() {
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    /**
     * 导航到指定路由
     *
     * @param route 类型安全的路由对象（必须是 @Serializable）
     * @param navOptions 导航选项（可选）
     */
    suspend fun navigateTo(route: Any, navOptions: NavOptions? = null) {
        _navigationEvents.emit(NavigationEvent.NavigateTo(route, navOptions))
    }

    /**
     * 返回上一页（简单返回，无结果）
     */
    suspend fun navigateBack() {
        _navigationEvents.emit(NavigationEvent.NavigateUp)
    }

    /**
     * 返回上一页并携带类型安全的结果（使用 NavigationResultKey）
     *
     * @param key 类型安全的结果 Key
     * @param result 要传递的结果对象
     */
    suspend fun <T> popBackStackWithResult(key: NavigationResultKey<T>, result: T) {
        _navigationEvents.emit(NavigationEvent.PopBackStackWithResult(key, result))
    }

    /**
     * 返回到指定路由
     *
     * @param route 目标路由对象（必须是 @Serializable）
     * @param inclusive 是否包含目标路由本身（true: 目标也会被弹出，false: 保留目标）
     */
    suspend fun navigateBackTo(route: Any, inclusive: Boolean = false) {
        _navigationEvents.emit(NavigationEvent.NavigateBackTo(route, inclusive))
    }
}

/**
 * 处理导航事件的 NavController 扩展函数
 *
 * 将导航事件转换为实际的导航操作
 *
 * @param event 导航事件
 */
fun NavController.handleNavigationEvent(event: NavigationEvent) {
    when (event) {
        is NavigationEvent.NavigateTo -> {
            // 使用类型安全的 navigate 方法
            this.navigate(event.route, event.navOptions)
        }

        is NavigationEvent.NavigateUp -> {
            // 简单返回，不携带结果
            this.popBackStack()
        }

        is NavigationEvent.PopBackStackWithResult<*> -> {
            // 使用 NavigationResultKey 的类型安全结果回传
            // 通过 key.serialize 将结果转换为 SavedStateHandle 支持的类型
            @Suppress("UNCHECKED_CAST")
            val key = event.key as NavigationResultKey<Any?>
            val rawValue = key.serialize(event.result)
            previousBackStackEntry?.savedStateHandle?.set(key.key, rawValue)
            this.popBackStack()
        }

        is NavigationEvent.NavigateBackTo -> {
            // 弹出回退栈到指定路由
            this.popBackStack(event.route, event.inclusive)
        }
    }
}
