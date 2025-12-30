import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    // Android 应用构建插件
    alias(libs.plugins.android.application)
    // Kotlin Android 支持
    alias(libs.plugins.kotlin.android)
    // Compose 编译插件
    alias(libs.plugins.kotlin.compose)
    // Kotlin 序列化插件
    alias(libs.plugins.kotlin.serialization)
    // Hilt 依赖注入插件
    alias(libs.plugins.hilt)
    // KSP 注解处理插件
    alias(libs.plugins.ksp)
}

// 加载签名文件
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.wenchen.yiyi"
    // 编译期使用的 SDK 版本
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.wenchen.yiyi"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "1.3.1"
        // Instrumentation 测试入口
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    // ABI 分包配置 - 一次性打包多个架构版本
//    splits {
//        abi {
//            // 启用 ABI 分包
//            isEnable = true
//            // 重置默认列表
//            reset()
//            // 包含的架构：32位和64位 ARM
//            include("armeabi-v7a", "arm64-v8a")
//            // 是否生成通用 APK（包含所有架构）
//            // 设置为 true 会额外生成一个包含所有架构的 APK
//            isUniversalApk = true
//        }
//    }

    // 自定义打包名称
    applicationVariants.all {
        outputs.all {
            // 修正类型转换问题
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "YiYi-AIChat_${buildType.name}_v${versionName}.apk"
        }
    }
    // 配置签名文件
    signingConfigs {
        create("config") {
            storeFile = file(keystoreProperties["KEY_STORE_FILE"] as String)
            storePassword = keystoreProperties["KEY_STORE_PASSWORD"] as String
            keyAlias = keystoreProperties["KEY_ALIAS"] as String
            keyPassword = keystoreProperties["KEY_PASSWORD"] as String

            // 启用所有签名方案以确保最大兼容性
            // JAR 签名 (Android 1.0+)
            enableV1Signing = true
            // APK 签名 v2 (Android 7.0+)
            enableV2Signing = true
            // APK 签名 v3 (Android 9.0+)
            enableV3Signing = true
            // APK 签名 v4 (Android 11.0+)
            enableV4Signing = true
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            // 是否启用资源压缩
            isShrinkResources = true
            buildConfigField("Boolean", "DEBUG", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("config")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("config")
            buildConfigField("Boolean", "DEBUG", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // 开启 Compose 支持
        compose = true
        // 开启 BuildConfig 支持
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        // Kotlin 编译生成的 JVM 字节码版本
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
// AndroidX Core 基础
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.core.splashscreen)

// Jetpack Compose UI
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.ui.graphics)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.compose.material.icons.core)
implementation(libs.androidx.compose.material.icons.extended)
implementation(libs.androidx.compose.material3)
implementation(libs.coil3.compose) // 图片加载
implementation(libs.coil3.gif) // 图片加载 GIF 支持
implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
implementation(libs.haze) // 模糊工具库
// ConstraintLayout
implementation(libs.androidx.constraintlayout)
implementation(libs.androidx.constraintlayout.compose)
// Material Design 组件
implementation(libs.material)

// 导航组件
implementation(libs.navigation.compose)

// 序列化
implementation(libs.kotlinx.serialization.json)

// 网络请求 (Retrofit + OkHttp)
implementation(libs.retrofit)
implementation(libs.retrofit2.kotlinx.serialization.converter)
implementation(libs.okhttp)
implementation(libs.logging.interceptor)
debugImplementation(libs.chucker)
releaseImplementation(libs.chucker.no.op)

// 权限
implementation(libs.xxpermissions)

// toast
implementation(libs.toaster)

// 数据存储
implementation(libs.mmkv)

// 日志
implementation(libs.timber)

// 依赖注入 (Hilt + Navigation)
implementation(libs.hilt.android)
ksp(libs.hilt.android.compiler)
implementation(libs.hilt.navigation.compose)
androidTestImplementation(libs.hilt.android.testing)
kspAndroidTest(libs.hilt.android.compiler)
compileOnly(libs.ksp.gradlePlugin)

// 数据库 (Room)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
implementation(libs.androidx.room.paging)
ksp(libs.androidx.room.compiler)
kspTest(libs.androidx.room.compiler)
kspAndroidTest(libs.androidx.room.compiler)
testImplementation(libs.androidx.room.testing)
androidTestImplementation(libs.androidx.room.testing)

// JSON 解析 (Moshi)
implementation(libs.moshi)
implementation(libs.moshi.kotlin)
ksp(libs.moshi.kotlin.codegen)
implementation(libs.gson)

// 调试工具
// debugImplementation(libs.leakcanary.android)

// 单元 / UI 测试
testImplementation(libs.junit)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
debugImplementation(libs.androidx.compose.ui.tooling)
debugImplementation(libs.androidx.compose.ui.test.manifest)
}