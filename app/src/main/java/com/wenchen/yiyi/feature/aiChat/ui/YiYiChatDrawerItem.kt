package com.wenchen.yiyi.feature.aiChat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wenchen.yiyi.core.common.theme.WhiteText

@Composable
fun YiYiChatDrawerItem(
    modifier: Modifier = Modifier,
    label: String,
    description: String? = null,
    onClick: () -> Unit = {},
) {
    ConstraintLayout(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp)
    ) {
        val (labelRef, descriptionRef, iconRef) = createRefs()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(WhiteText),
            modifier = Modifier.constrainAs(labelRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WhiteText,
            modifier = Modifier.constrainAs(iconRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(WhiteText.copy(0.66f)),
                modifier = Modifier.constrainAs(descriptionRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(iconRef.start)
                }
            )
        }
    }
}

@Composable
fun YiYiChatDrawerSwitchItem(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    ConstraintLayout(modifier.fillMaxWidth()) {
        val (labelRef, descriptionRef, switchRef) = createRefs()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(WhiteText),
            modifier = Modifier.constrainAs(labelRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.constrainAs(switchRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        )
    }
}