package com.wenchen.yiyi.core.designSystem.component

import android.util.Log
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import java.util.ArrayDeque
import java.util.EnumMap

/**
 * 特殊处理括号内文本样式的控件
 * @param text 要显示的文本内容
 * @param modifier 控件修饰符
 * @param normalTextStyle 普通文本样式（字号、字体等）
 * @param specialTextStyle 括号内文本样式（默认为普通文本的斜体、颜色为LightGray）
 */
@Composable
fun StyledBracketText(
    text: String,
    modifier: Modifier = Modifier,
    normalTextStyle: TextStyle = LocalTextStyle.current,
    specialTextStyle: TextStyle = normalTextStyle.copy(
        color = Color.LightGray,
        fontStyle = FontStyle.Italic
    )
) {
    // 缓存解析后的富文本，避免重复计算
    val coloredText = remember(text) {
        buildColoredAnnotatedString(text, normalTextStyle, specialTextStyle)
    }
    Text(
        text = coloredText,
        modifier = modifier,
    )
}

// 括号类型定义：包含开括号、闭括号、对应显示颜色
private enum class BracketType(
    val openChar: Char,
    val closeChar: Char,
    val defaultStyle: TextStyle
) {
    SQUARE('[', ']', TextStyle(color = Color.Blue, fontStyle = FontStyle.Italic)),
    ROUND('(', ')', TextStyle(color = Color.Green, fontStyle = FontStyle.Italic)),
    SQUARE_CN('【', '】', TextStyle(color = Color.Red, fontStyle = FontStyle.Italic)),
    ROUND_CN('（', '）', TextStyle(color = Color.LightGray, fontStyle = FontStyle.Italic)),
}

// 区间数据类：记录括号配对的起始/结束索引和对应颜色
private data class ColorRange(
    val startIndex: Int,
    val endIndex: Int,
    val textStyle: TextStyle
)

private data class HalfBracket(
    val bracket: Char,
    val index: Int,
)

// 创建字符到BracketType的映射
private val bracketMap = buildMap {
    BracketType.entries.forEach { type ->
        put(type.openChar, type)
        put(type.closeChar, type)
    }
}

/**
 * 构建带颜色的AnnotatedString，用于Compose Text组件显示
 * @param text 要显示的文本内容
 * @param normalTextStyle 普通文本样式（字号、字体等）
 * @param specialTextStyle 特殊文本样式（如斜体、颜色等）
 * @return 包含颜色标注的AnnotatedString
 */
private fun buildColoredAnnotatedString(
    text: String,
    normalTextStyle: TextStyle,
    specialTextStyle: TextStyle
): AnnotatedString {
    // 解析所有有效的括号配对区间
    val colorRanges = parseRanges(text, normalTextStyle, specialTextStyle)
    Log.d("StyledBracketText", "colorRanges:" + colorRanges.joinToString("\n "))
    return buildAnnotatedString {
        colorRanges.forEach { range ->
            withStyle(range.textStyle.toSpanStyle()) {
                append(text.substring(range.startIndex, range.endIndex + 1))
            }
        }
    }
}

private fun parseRanges(
    text: String,
    normalTextStyle: TextStyle,
    specialTextStyle: TextStyle
): List<ColorRange> {
    val matchedBrackets = findMatchedBrackets(text)
    if (matchedBrackets.isEmpty()) {
        return listOf(ColorRange(0, text.lastIndex, normalTextStyle))
    }

    return buildColorRanges(text, matchedBrackets, normalTextStyle, specialTextStyle)
}

private fun findMatchedBrackets(text: String): List<HalfBracket> {
    val startTime = System.nanoTime()
    val bracketIndices = mutableListOf<HalfBracket>()
    /* 知识点：旧方法
    * O(mn) 时间复杂度，m是文本长度，n是括号类型数量
    * 这里的时间复杂度随着需要匹配的括号类型数量增加而增加，后续采用空间换时间的优化做法
    BracketType.entries.forEach { bracketType ->
        val openBracketStack = ArrayDeque<HalfBracket>() // 存储开括号的索引栈
        text.forEachIndexed { index, char ->
            when (char) {
                // 遇到开括号，记录索引到栈中
                bracketType.openChar -> {
                    openBracketStack.push(HalfBracket(char, index))
                }

                // 遇到闭括号，匹配最近的开括号
                bracketType.closeChar -> {
                    if (openBracketStack.isNotEmpty()) {
                        bracketIndices.add(openBracketStack.pop())
                        bracketIndices.add(HalfBracket(char, index))
                    }
                }
            }
        }
    }
     */

    // 为每种括号类型维护一个栈
    /* 两种方式差不多
    val stacks = mutableMapOf<BracketType, ArrayDeque<HalfBracket>>()
    BracketType.entries.forEach { type ->
        stacks[type] = ArrayDeque()
    }
     */
    val stacks = EnumMap<BracketType, ArrayDeque<HalfBracket>>(BracketType::class.java)
    BracketType.entries.forEach { type ->
        stacks[type] = ArrayDeque()
    }

    // 牺牲内存空间，只遍历一次文本
    text.forEachIndexed { index, char ->
        val bracketType = bracketMap[char] ?: return@forEachIndexed

        if (char == bracketType.openChar) {
            // 记录到type对应的栈中
            stacks[bracketType]?.push(HalfBracket(char, index))
        } else {
            val stack = stacks[bracketType] ?: return@forEachIndexed
            if (stack.isNotEmpty()) {
                bracketIndices.add(stack.pop())
                bracketIndices.add(HalfBracket(char, index))
            }
        }
    }
    val endTime = System.nanoTime()
    /*
     原方法用时：1579427 ns 反复调用经系统优化后：300052 ns
     空间换时间：558490 ns 反复调用经系统优化后：63333 ns
     初次加载时间提升64.64% 反复调用时提升78.89% 优化效果显著
     */
    Log.d("Timer", "括号分析用时: ${endTime - startTime} ns")
    return bracketIndices.sortedBy { it.index }
}

private fun buildColorRanges(
    text: String,
    matchedBrackets: List<HalfBracket>,
    normalTextStyle: TextStyle,
    specialTextStyle: TextStyle
): List<ColorRange> {
//    Log.d("StyledBracketText", "matchedBrackets:" + matchedBrackets.joinToString("\n "))
    val allRanges = mutableListOf<ColorRange>()
    val bracketStack = ArrayDeque<BracketType>()
    var currentIndex = 0
    /* 知识点：旧版本
    * 变化：用于记录的栈类型从ArrayDeque<HalfBracket> -> ArrayDeque<BracketType>
    *      直接记录HalfBracket(char,index)与记录BracketType是等效的，入栈出栈只需要判断是开括号还是闭括号即可。
    *      因为传入的matchedBrackets已经过滤了不成对的括号，而且输出时进行了排序，即matchedBrackets的括号成对且有序，
    *      所以matchedBrackets的作用仅仅是记录栈是否为空，如果是空则说明当前括号之前的其他括号都完成了匹配。
    *      在这一点上使用ArrayDeque<HalfBracket>还是ArrayDeque<BracketType>没有区别
    *      巧妙在于反正已经使用了ArrayDeque并且可以方便的找到上一个元素，那么直接记录上一个元素的类型就可以省去lastTextStyle变量了
    * 变化：ArrayDeque<HalfBracket>也直接影响了文本划分，旧方法中根据括号开闭来划分，总是想连同括号和文本一同处理，结果显得非常繁琐
    *      新方法思路：直接应用ArrayDeque的Last的样式一定是对的
    *      如果前一个元素是开括号，那么当前文本的样式就是上一个元素的样式
    *      如果前一个元素是闭括号，那么它一定已经完成闭合被弹出了，这时候ArrayDeque的Last就是再前一个未闭合的括号
    *      [ 蓝 蓝 ( 绿 绿 ) 蓝 蓝 ( 绿 绿 ) 蓝 蓝 ]
    *      1       2      3      4       5      6
    *      1，处理自己即可，然后开括号入栈，很简单
    *      2，使用1的样式，文本为蓝色，然后将自己变成绿色，开括号入栈，
    *      3，使用2的样式，文本为绿色，再处理自己为绿色，关键：3是一个闭括号，不入栈且会将2出栈，
    *      4，这里就不使用前一个括号的样式了，2,3已经完成闭合不在栈里，再去获取ArrayDeque的Last会得到1，然后应用1的样式，文本为蓝色
    *      后面的推理同理
    *
    var lastTextStyle = normalTextStyle
    val openBracketStack = ArrayDeque<HalfBracket>()
    for ((char, index) in bracketIndices) {
        // 获取char对应的括号类型 firstOrNull：从集合中查找第一个满足条件的元素
        val bracketType = bracketMap[char] ?: continue
        allRanges.add(
            ColorRange(
                startIndex = currentIndex,
                endIndex = if (char == bracketType.openChar) index - 1 else index,
                textStyle = when (char) {
                    bracketType.openChar -> if (openBracketStack.isEmpty()) normalTextStyle else lastTextStyle
                    bracketType.closeChar -> bracketType.style
                    else -> normalTextStyle
                }
            )
        )
        when (char) {
            bracketType.openChar -> openBracketStack.push(HalfBracket(char, index))
            bracketType.closeChar -> openBracketStack.pop()
        }
        currentIndex = if (char == bracketType.openChar) index else index + 1
        lastTextStyle = bracketType.style
    }
     */
    matchedBrackets.forEach { (char, index) ->
        val bracketType = bracketMap[char] ?: return@forEach
        // 新方法的思路，要处理的文本是上一个括号的下一个字符(index+1)，当前括号的上一个字符(index-1)
        // 添加括号前的文本段
        if (currentIndex < index) {
            // 栈为空意味着当前所处理的文本没有被括号所包裹，应当采用常规样式
            val textStyle = bracketStack.lastOrNull()?.let { specialTextStyle } ?: normalTextStyle
            allRanges.add(ColorRange(currentIndex, index - 1, textStyle))
        }

        // 添加括号本身
        allRanges.add(ColorRange(index, index, specialTextStyle))
        currentIndex = index + 1

        when (char) {
            bracketType.openChar -> bracketStack.addLast(bracketType)
            bracketType.closeChar -> bracketStack.removeLast()
        }
    }

    // 添加剩余文本
    if (currentIndex <= text.lastIndex) {
        val textStyle = bracketStack.lastOrNull()?.let { specialTextStyle } ?: normalTextStyle
        allRanges.add(ColorRange(currentIndex, text.lastIndex, textStyle))
    }

    return allRanges
}