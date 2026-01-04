package com.wenchen.yiyi.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionUtils {

    // 检查并请求悬浮窗权限
    fun checkAndRequestOverlayPermission(activity: Activity, launcher: ActivityResultLauncher<Intent>): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(activity)) {
                true // 已有权限
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${activity.packageName}".toUri()
                )
                launcher.launch(intent)
                false // 需要请求权限
            }
        } else {
            true // 低版本默认有权限
        }
    }

    // 检查是否有悬浮窗权限
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    // 请求悬浮窗权限
    fun requestOverlayPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${activity.packageName}".toUri()
            )
            activity.startActivityForResult(intent, requestCode)
        }
    }

    // 检查并请求存储权限
    fun checkAndRequestStoragePermission(activity: Activity, requestPermissionLauncher: ActivityResultLauncher<String>) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14及以上版本 - 使用新的精选照片API，无需提前请求权限
                Log.d("PermissionUtils", "Android 14+，使用精选照片API无需提前请求权限")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13 (API 33) - 使用READ_MEDIA_IMAGES权限
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_MEDIA_IMAGES,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else -> {
                // Android 12及以下版本 - 使用READ_EXTERNAL_STORAGE权限
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
}