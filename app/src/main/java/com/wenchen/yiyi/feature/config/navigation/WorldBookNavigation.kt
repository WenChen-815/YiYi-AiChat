package com.wenchen.yiyi.feature.config.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.config.view.WorldBookEditRoute
import com.wenchen.yiyi.feature.config.view.WorldBookEntryEditRoute
import com.wenchen.yiyi.feature.config.view.WorldBookListRoute
import com.wenchen.yiyi.navigation.routes.WorldBookRoutes

/**
 * 注册世界页面路由
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

    composable<WorldBookRoutes.WorldBookEntryEdit> {
        WorldBookEntryEditRoute(navController = navController)
    }
}