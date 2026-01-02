package com.wenchen.yiyi.feature.aiChat.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.wenchen.yiyi.feature.aiChat.view.CharacterEditRoute
import com.wenchen.yiyi.feature.aiChat.view.GroupChatRoute
import com.wenchen.yiyi.feature.aiChat.view.SingleChatRoute
import com.wenchen.yiyi.navigation.routes.AiChatRoutes

/**
 * 注册AI相关页面路由
 *
 * @param navController NavHostController
 * @param sharedTransitionScope 共享转场作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.aiChatScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AiChatRoutes.SingleChat> {
        SingleChatRoute(navController = navController)
    }

    composable<AiChatRoutes.GroupChat> {
        GroupChatRoute(navController = navController)
    }

    composable<AiChatRoutes.CharacterEdit> {
        CharacterEditRoute(navController = navController)
    }
}