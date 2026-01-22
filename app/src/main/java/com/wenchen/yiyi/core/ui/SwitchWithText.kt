package com.wenchen.yiyi.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wenchen.yiyi.core.designSystem.theme.TextWhite
import com.wenchen.yiyi.core.designSystem.component.SpaceHorizontalSmall

@Composable
fun SwitchWithText(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text)
        SpaceHorizontalSmall()
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}