package com.wenchen.yiyi.common.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wenchen.yiyi.common.theme.DarkGray
import com.wenchen.yiyi.common.theme.GrayBorder
import com.wenchen.yiyi.common.theme.LightGray

@Composable
fun SettingTextFieldItem(
    modifier: Modifier = Modifier,
    label: String = "",
    labelPadding: PaddingValues = PaddingValues(0.dp),
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    placeholder: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()){
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(labelPadding)
            )
        }
        YiYiOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            minHeight = 48.dp,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isSystemInDarkTheme()) DarkGray else LightGray,
                unfocusedContainerColor = if (isSystemInDarkTheme()) DarkGray else LightGray,
                unfocusedIndicatorColor = GrayBorder.copy(0.5f) // 边框颜色
            ),
            placeholder = placeholder,
            maxLines = maxLines,
            minLines = minLines,
        )
    }
}