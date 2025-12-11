package com.wenchen.yiyi.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.common.theme.AIChatTheme

class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIChatTheme {
                TestScreen()
            }
        }
    }
}


// 预览示例
@Preview(showBackground = true)
@Composable
fun TestScreen() {
    Column (modifier = Modifier.padding(16.dp, 48.dp)) {

    }
}
