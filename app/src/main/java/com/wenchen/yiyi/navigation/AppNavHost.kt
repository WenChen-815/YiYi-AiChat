package com.wenchen.yiyi.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wenchen.yiyi.feature.main.navigation.mainGraph
import com.wenchen.yiyi.navigation.routes.MainRoutes
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
    navigator: AppNavigator,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // 监听导航事件
    LaunchedEffect(navController) {
        navigator.navigationEvents.collectLatest { event ->
            navController.handleNavigationEvent(event)
        }
    }

    SharedTransitionLayout {
        Scaffold(bottomBar = {StackStatusView(navController)
        }) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.Main,
                modifier = modifier,
                // 页面进入动画
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                // 页面退出动画
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                // 返回时页面进入动画
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                },
                // 返回时页面退出动画
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            ) {
                mainGraph(navController, this@SharedTransitionLayout)
//                composable<HomeRoutes.Home> { HomeScreen(navController) }
//                composable<NavWithDataRoutes.SetDataScreen> { SetDataScreen(navController) }
//                composable<NavWithDataRoutes.GetDataScreen> { GetDataScreen(navController) }
//                composable<BackDataRoutes.BackData> { BackDataScreen(navController) }
//                composable<BackDataRoutes.SetData2Back> { SetData2BackScreen(navController) }
//                composable<FakeLoginRoutes.Login> { LoginScreen(navController) }
//                composable<FakeLoginRoutes.LoginSuccess> { LoginSuccessScreen(navController) }
            }
        }
    }
}


@SuppressLint("RestrictedApi")
@Composable
fun StackStatusView(navController: NavController) {
    val backStackEntry = navController.currentBackStack.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
        Text(
            text = "Navigation Stack:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = buildString {
                backStackEntry.value.forEachIndexed { index, entry ->
                    append("[$index] ${entry.destination.route}\n")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(4.dp)
        )
        Button(
            onClick = { /* 可用于触发刷新或其他操作 */ },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Refresh Stack Info")
        }
    }
}
