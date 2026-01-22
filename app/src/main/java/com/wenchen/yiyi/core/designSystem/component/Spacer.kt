package com.wenchen.yiyi.core.designSystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalXXLarge
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalXLarge
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalLarge
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalMedium
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalSmall
import com.wenchen.yiyi.core.designSystem.theme.SpaceVerticalXSmall
import com.wenchen.yiyi.core.designSystem.theme.SpaceHorizontalMedium
import com.wenchen.yiyi.core.designSystem.theme.SpaceHorizontalSmall
import com.wenchen.yiyi.core.designSystem.theme.SpaceHorizontalXLarge
import com.wenchen.yiyi.core.designSystem.theme.SpaceHorizontalXSmall
import com.wenchen.yiyi.core.designSystem.theme.SpaceHorizontalXXLarge


/**
 * 创建一个超大垂直间距(32dp)的Spacer组件
 * 使用方式：SpaceVerticalXXLarge()
 */
@Composable
fun SpaceVerticalXXLarge() {
    Spacer(modifier = Modifier.height(SpaceVerticalXXLarge))
}

/**
 * 创建一个特大垂直间距(24dp)的Spacer组件
 * 使用方式：SpaceVerticalXLarge()
 */
@Composable
fun SpaceVerticalXLarge() {
    Spacer(modifier = Modifier.height(SpaceVerticalXLarge))
}

/**
 * 创建一个大垂直间距(16dp)的Spacer组件
 * 使用方式：SpaceVerticalLarge()
 */
@Composable
fun SpaceVerticalLarge() {
    Spacer(modifier = Modifier.height(SpaceVerticalLarge))
}

/**
 * 创建一个中等垂直间距(12dp)的Spacer组件
 * 使用方式：SpaceVerticalMedium()
 */
@Composable
fun SpaceVerticalMedium() {
    Spacer(modifier = Modifier.height(SpaceVerticalMedium))
}

/**
 * 创建一个小垂直间距(8dp)的Spacer组件
 * 使用方式：SpaceVerticalSmall()
 */
@Composable
fun SpaceVerticalSmall() {
    Spacer(modifier = Modifier.height(SpaceVerticalSmall))
}

/**
 * 创建一个超小垂直间距(4dp)的Spacer组件
 * 使用方式：SpaceVerticalXSmall()
 */
@Composable
fun SpaceVerticalXSmall() {
    Spacer(modifier = Modifier.height(SpaceVerticalXSmall))
}
//endregion

//region 水平间距组件
/**
 * 创建一个超大水平间距(32dp)的Spacer组件
 * 使用方式：SpaceHorizontalXXLarge()
 */
@Composable
fun SpaceHorizontalXXLarge() {
    Spacer(modifier = Modifier.width(SpaceHorizontalXXLarge))
}

/**
 * 创建一个特大水平间距(24dp)的Spacer组件
 * 使用方式：SpaceHorizontalXLarge()
 */
@Composable
fun SpaceHorizontalXLarge() {
    Spacer(modifier = Modifier.width(SpaceHorizontalXLarge))
}

/**
 * 创建一个大水平间距(16dp)的Spacer组件
 * 使用方式：SpaceHorizontalLarge()
 */
@Composable
fun SpaceHorizontalLarge() {
    Spacer(modifier = Modifier.width(SpaceHorizontalXLarge))
}

/**
 * 创建一个中等水平间距(12dp)的Spacer组件
 * 使用方式：SpaceHorizontalMedium()
 */
@Composable
fun SpaceHorizontalMedium() {
    Spacer(modifier = Modifier.width(SpaceHorizontalMedium))
}

/**
 * 创建一个小水平间距(8dp)的Spacer组件
 * 使用方式：SpaceHorizontalSmall()
 */
@Composable
fun SpaceHorizontalSmall() {
    Spacer(modifier = Modifier.width(SpaceHorizontalSmall))
}

/**
 * 创建一个超小水平间距(4dp)的Spacer组件
 * 使用方式：SpaceHorizontalXSmall()
 */
@Composable
fun SpaceHorizontalXSmall() {
    Spacer(modifier = Modifier.width(SpaceHorizontalXSmall))
}