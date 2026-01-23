package com.wenchen.yiyi.core.util.common

/**
 * 匹配 Html 中的 @import url('...'); 或 @import '...';
 */
val importPattern = Regex("""@import\s+(?:url\(\s*['"]?|['"])(.*?)(?:['"]?\s*\)|['"])\s*;""", RegexOption.IGNORE_CASE)

/**
 * 匹配 Html 中的 <link ...> 标签
 */
val linkTagPattern = Regex("""<link\s+[^>]*>""", RegexOption.IGNORE_CASE)

/**
 * 匹配 href 属性
 */
val hrefPattern = Regex("""href=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
