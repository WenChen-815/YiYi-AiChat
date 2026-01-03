package com.wenchen.yiyi.feature.worldBook.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.worldBook.view.WorldBookEditRoute
import com.wenchen.yiyi.feature.worldBook.view.WorldBookListRoute
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes

/**
 * 注册世界相关页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.worldBookScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<WorldBookRoutes.WorldBookList> {
        WorldBookListRoute(navController = navController)
    }

    composable<WorldBookRoutes.WorldBookEdit> {
        WorldBookEditRoute(navController = navController)
    }
}