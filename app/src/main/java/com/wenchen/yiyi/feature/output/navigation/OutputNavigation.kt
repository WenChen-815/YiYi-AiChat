package com.wenchen.yiyi.feature.output.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.output.view.CardParserScreen
import com.wenchen.yiyi.feature.output.view.OutputRoute
import com.wenchen.yiyi.navigation.routes.OutputRoutes

/**
 * 注册导出页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.outputScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<OutputRoutes.Output> {
        OutputRoute(navController = navController)
    }

    composable<OutputRoutes.CardParser> {
        CardParserScreen(navController = navController)
    }
}
