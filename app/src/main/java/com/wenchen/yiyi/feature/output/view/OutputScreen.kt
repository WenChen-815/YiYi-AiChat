package com.wenchen.yiyi.feature.output.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.wenchen.yiyi.core.designSystem.theme.AppTheme
import com.wenchen.yiyi.core.designSystem.theme.BgWhiteDark
import com.wenchen.yiyi.core.database.entity.AICharacter
import com.wenchen.yiyi.core.designSystem.theme.TextWhite
import com.wenchen.yiyi.feature.output.viewmodel.OutputViewModel

@Composable
internal fun OutputRoute(
    viewModel: OutputViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val context = LocalContext.current
    val characters by viewModel.characters.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    OutputScreen(
        characters = characters,
        selectedIndex = selectedIndex,
        isExporting = isExporting,
        onItemClick = { index ->
            viewModel.onItemClick(index)
        },
        outputSelected = { viewModel.outputSelected(context) }
    )
}

@Composable
fun OutputScreen(
    characters: List<AICharacter>,
    selectedIndex: List<Int>,
    isExporting: Boolean,
    onItemClick: (Int) -> Unit,
    outputSelected: () -> Unit
) {
    OutputScreenContent(
        characters = characters,
        selectedIndex = selectedIndex,
        isExporting = isExporting,
        onItemClick = onItemClick,
        outputSelected = outputSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputScreenContent(
    characters: List<AICharacter>,
    selectedIndex: List<Int>,
    isExporting: Boolean,
    onItemClick: (Int) -> Unit,
    outputSelected: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "角色导出")
                },
                actions = {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 28.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "导出${selectedIndex.size}个角色",
                            style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .clickable {
                                    // 导出角色
                                    outputSelected()
                                }
                                .padding(horizontal = 8.dp),
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (characters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无角色")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(characters.size, key = { index -> characters[index].id }) { index ->
                    val character = characters[index]
                    CharacterItem(
                        character = character,
                        isSelected = selectedIndex.contains(index),
                        onItemClick = {
                            onItemClick(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterItem(
    character: AICharacter,
    isSelected: Boolean,
    onItemClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors =
        CardDefaults.cardColors(
            containerColor = Color.Transparent, // 设置Card本身透明
        ),
        // 上面的colors针对Card本身，而modifier.background针对所处控件的内容区域（默认是透明的）,两者的作用不同
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .border(
                width = 4.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onItemClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (LocalInspectionMode.current) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgWhiteDark)
                        .clip(RoundedCornerShape(8.dp, 8.dp, 9.dp, 9.dp))
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(character.background)
                        .build(),
                    contentDescription = "角色背景",
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp, 8.dp, 9.dp, 9.dp)),
                )
            }
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                        Brush.verticalGradient(
                            colors =
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                BgWhiteDark.copy(alpha = 0.9f),
                                BgWhiteDark,
                            ),
                            startY = 0f, // 渐变起点Y坐标
                            endY = Float.POSITIVE_INFINITY, // 渐变终点Y坐标
                        ),
                    ),
            )
            Column(
                modifier =
                Modifier
                    .background(Color.Transparent)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 角色信息
                    Column(
                        modifier =
                        Modifier
                            .weight(1f)
                            .padding(8.dp, 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            if (LocalInspectionMode.current) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(character.avatar)
                                        .build(),
                                    contentDescription = "角色头像",
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                    Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun OutputScreenPreview() {
    val characters = emptyList<AICharacter>()
    AppTheme {
        OutputScreenContent(
            characters = characters,
            selectedIndex = listOf(0, 1, 2),
            isExporting = false,
            onItemClick = { _ -> },
            outputSelected = {}
        )
    }

}
