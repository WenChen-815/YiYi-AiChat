package com.wenchen.yiyi.feature.profile.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.feature.config.ui.activity.ConfigActivity

@Composable
fun ProfileScreen(
    context: Context,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "敬请期待",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { context.startActivity(Intent(context, ConfigActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("全局设置")
        }
    }
}
