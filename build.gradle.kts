// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android应用程序插件，用于构建Android应用
    alias(libs.plugins.android.application) apply false
    // Kotlin Android插件，支持Kotlin语言开发
    alias(libs.plugins.kotlin.android) apply false
    // Kotlin Compose插件，用于Jetpack Compose UI开发
    alias(libs.plugins.kotlin.compose) apply false
    // Kotlin Serialization插件
    alias(libs.plugins.kotlin.serialization) apply false
    // 依赖注入相关插件
    // Hilt插件，用于依赖注入框架的支持
    alias(libs.plugins.hilt) apply false
    // KSP (Kotlin Symbol Processing)插件，用于注解处理
    alias(libs.plugins.ksp) apply false
}