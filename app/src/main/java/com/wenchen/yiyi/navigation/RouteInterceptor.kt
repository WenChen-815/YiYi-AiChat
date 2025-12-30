package com.wenchen.yiyi.navigation

import com.wenchen.yiyi.navigation.routes.AuthRoutes
import com.wenchen.yiyi.navigation.routes.UserRoutes
import kotlin.reflect.KClass

/**
 * 路由拦截器（类型安全版本）
 *
 * 负责管理需要登录的页面配置和路由拦截逻辑
 * 使用类型安全的方式处理路由拦截
 */
class RouteInterceptor {

    /**
     * 需要登录的路由类型集合
     * 在这里配置所有需要登录才能访问的页面类型
     */
    private val loginRequiredRouteTypes: MutableSet<KClass<out Any>> = mutableSetOf(
        UserRoutes.Info::class
    )

    /**
     * 检查指定路由对象是否需要登录
     *
     * @param route 要检查的路由对象（类型安全）
     * @return true表示需要登录，false表示不需要登录
     */
    fun requiresLogin(route: Any): Boolean {
        val routeClass = route::class
        return loginRequiredRouteTypes.contains(routeClass)
    }

    /**
     * 获取登录页面路由对象
     *
     * @return 登录页面的路由对象
     */
    fun getLoginRoute(): Any = AuthRoutes.Login

    /**
     * 添加需要登录的路由类型
     *
     * @param routeClass 需要登录的路由类型
     */
    fun addLoginRequiredRoute(routeClass: KClass<*>) {
        loginRequiredRouteTypes.add(routeClass)
    }

    /**
     * 移除需要登录的路由类型
     *
     * @param routeClass 不再需要登录的路由类型
     */
    fun removeLoginRequiredRoute(routeClass: KClass<*>) {
        loginRequiredRouteTypes.remove(routeClass)
    }

    /**
     * 获取所有需要登录的路由类型
     *
     * @return 需要登录的路由类型集合
     */
    fun getLoginRequiredRoutes(): Set<KClass<*>> {
        return loginRequiredRouteTypes.toSet()
    }
}
