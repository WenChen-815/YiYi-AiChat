package com.wenchen.yiyi.feature.config.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.config.view.ApiConfigRoute
import com.wenchen.yiyi.feature.config.view.ChatConfigRoute
import com.wenchen.yiyi.feature.config.view.RegexConfigRoute
import com.wenchen.yiyi.feature.config.view.RegexDetailRoute
import com.wenchen.yiyi.feature.config.view.SettingsRoute
import com.wenchen.yiyi.navigation.routes.ConfigRoutes

/**
 * 注册主页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.configScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<ConfigRoutes.Settings> {
        SettingsRoute()
    }

    composable<ConfigRoutes.ChatConfig> {
        ChatConfigRoute(navController = navController)
    }

    composable<ConfigRoutes.ApiConfig> {
        ApiConfigRoute(navController = navController)
    }

    composable<ConfigRoutes.RegexConfig> {
        RegexConfigRoute()
    }

    composable<ConfigRoutes.RegexDetail> {
        RegexDetailRoute()
    }
}
