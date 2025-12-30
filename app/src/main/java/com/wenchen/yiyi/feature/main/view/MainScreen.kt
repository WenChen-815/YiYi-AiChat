package com.wenchen.yiyi.feature.main.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.wenchen.yiyi.core.base.state.BaseNetWorkUiState
import com.wenchen.yiyi.core.common.theme.AIChatTheme
import com.wenchen.yiyi.feature.main.viewmodel.MainViewModel

/**
 * 主页面路由
 *
 * @param viewModel 主页面 ViewModel
 */
@Composable
internal fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavController
) {
    MainScreen(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    viewModel : MainViewModel = hiltViewModel(),
    navController: NavController = NavController(LocalContext.current)
) {
    MainScreenContent(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    viewModel: MainViewModel,
    navController: NavController
) {
    val models = viewModel.models.collectAsState(emptyList())
    val uiState = viewModel.uiState.collectAsState(BaseNetWorkUiState.Loading)
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 添加按钮触发网络请求
            Button(
                onClick = { viewModel.executeGetModels() },
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(text = "发起网络请求")
            }
            // 显示加载状态
            if (uiState.value is BaseNetWorkUiState.Loading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // 显示错误状态
            if (uiState.value is BaseNetWorkUiState.Error) {
                Text(
                    text = "加载失败",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 显示模型列表数据
            if (models.value.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(models.value.size) { index ->
                        val model = models.value[index]
                        Text(
                            text = "ID: ${model.id}, Owner: ${model.owned_by}",
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                }
            } else if (uiState.value is BaseNetWorkUiState.Success && models.value.isEmpty()) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * 主页面界面浅色主题预览
 */
@Preview(showBackground = true)
@Composable
internal fun MainScreenPreview() {
    AIChatTheme {
        MainScreen(
        )
    }
}

/**
 * 主页面界面深色主题预览
 */
@Preview(showBackground = true)
@Composable
internal fun MainScreenPreviewDark() {
    AIChatTheme(darkTheme = true) {
        MainScreen(
        )
    }
}
