package com.wenchen.yiyi.feature.main.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.wenchen.yiyi.feature.main.navigation.mainScreen

/**
 * 主模块导航图
 *
 * @param navController 导航控制器
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    // 只调用页面级导航函数，不包含其他逻辑
     mainScreen(navController, sharedTransitionScope)
}
