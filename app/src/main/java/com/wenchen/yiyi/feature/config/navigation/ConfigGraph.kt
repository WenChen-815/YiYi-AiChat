package com.wenchen.yiyi.feature.config.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * 配置模块导航图
 *
 * @param navController 导航控制器
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.configGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    // 设置相关界面
    configScreen(navController, sharedTransitionScope)

    // 世界书相关界面
    worldBookScreen(navController, sharedTransitionScope)

    // 正则表达式相关界面
    regexScreen(navController, sharedTransitionScope)
}
