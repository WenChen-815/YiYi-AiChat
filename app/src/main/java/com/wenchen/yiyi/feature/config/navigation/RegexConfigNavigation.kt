package com.wenchen.yiyi.feature.config.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.config.view.RegexConfigRoute
import com.wenchen.yiyi.feature.config.view.RegexDetailRoute
import com.wenchen.yiyi.navigation.routes.RegexRoutes

/**
 * 注册正则页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.regexScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<RegexRoutes.RegexConfig> {
        RegexConfigRoute()
    }

    composable<RegexRoutes.RegexDetail> {
        RegexDetailRoute()
    }
}