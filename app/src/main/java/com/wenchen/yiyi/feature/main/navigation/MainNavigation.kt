package com.wenchen.yiyi.feature.main.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.main.view.MainRoute
import com.wenchen.yiyi.navigation.routes.MainRoutes

/**
 * 注册主页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.mainScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<MainRoutes.Main> {
        MainRoute(navController = navController)
    }
}
