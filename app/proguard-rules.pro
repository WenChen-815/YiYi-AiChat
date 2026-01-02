# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# xxp权限混淆规则 地址: https://github.com/getActivity/XXPermissions/blob/master/library/proguard-permissions.pro
-keepclassmembers interface com.hjq.permissions.start.IStartActivityDelegate {
    <methods>;
}
-keepclassmembers interface com.hjq.permissions.fragment.IFragmentMethodNative {
    <methods>;
}
-keepclassmembers class androidx.fragment.app.Fragment {
    androidx.fragment.app.FragmentActivity getActivity();
}

-keep class com.wenchen.yiyi.core.network.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }
# Gson
-keep class com.wenchen.yiyi.core.common.entity.** { *; }
-keep class com.wenchen.yiyi.feature.aiChat.entity.** { *; }
-keep class com.wenchen.yiyi.feature.worldBook.entity.** { *; }
-keep class com.wenchen.yiyi.core.common.ApiService$* { *; }
