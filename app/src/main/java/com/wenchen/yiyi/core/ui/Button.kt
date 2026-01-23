package com.wenchen.yiyi.core.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.core.designSystem.theme.PrimaryGradient
import com.wenchen.yiyi.core.designSystem.theme.TextWhite

@Composable
fun BottomGradientButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding( top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.Center
    ){
        Button(
            onClick = { onClick() },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(24.dp)
                .background(
                    brush = Brush.horizontalGradient(colors = PrimaryGradient),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite, fontWeight = FontWeight.Bold))
        }
    }
}
