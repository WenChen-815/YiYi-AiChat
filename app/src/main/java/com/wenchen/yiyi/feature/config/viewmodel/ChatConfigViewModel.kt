package com.wenchen.yiyi.feature.config.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.wenchen.yiyi.core.base.viewmodel.BaseNetWorkViewModel
import com.wenchen.yiyi.core.datastore.storage.ImageManager
import com.wenchen.yiyi.core.model.network.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.state.UserConfigState
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatConfigViewModel @Inject constructor(
    navigator: AppNavigator,
    userState: UserState,
    userConfigState: UserConfigState,
) : BaseNetWorkViewModel<ModelsResponse>(
    navigator = navigator,
    userState = userState,
    userConfigState = userConfigState
) {
    val userConfig = userConfigState.userConfig.value

    override fun requestApiFlow(): Flow<NetworkResponse<ModelsResponse>> {
        return emptyFlow()
    }

    fun updateUserConfig(
        userId: String,
        userName: String,
        userAvatarPath: String?,
        maxContextCount: String,
        summarizeCount: String,
        maxSummarizeCount: String,
        enableSeparator: Boolean,
        enableTimePrefix: Boolean,
        enableStreamOutput: Boolean,
        showLogcatView: Boolean
    ) {
        viewModelScope.launch {
            val newUserConfig = userConfig?.copy(
                userId = userId,
                userName = userName,
                userAvatarPath = userAvatarPath,
                maxContextMessageSize = maxContextCount.toIntOrNull() ?: 15,
                summarizeTriggerCount = summarizeCount.toIntOrNull() ?: 20,
                maxSummarizeCount = maxSummarizeCount.toIntOrNull() ?: 20,
                enableSeparator = enableSeparator,
                enableTimePrefix = enableTimePrefix,
                enableStreamOutput = enableStreamOutput,
                showLogcatView = showLogcatView
            ) ?: return@launch
            userConfigState.updateUserConfig(newUserConfig)
            navigator.navigateBack()
        }
    }

    fun saveUserAvatar(hasNewUserAvatar: Boolean, userAvatarBitmap: Bitmap?): String? {
        if (hasNewUserAvatar && userAvatarBitmap != null) {
            // 删除旧头像文件
            val oldAvatarPath = userConfig?.userAvatarPath
            if (!oldAvatarPath.isNullOrEmpty()) {
                try {
                    val oldFile = File(oldAvatarPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 保存新头像
            val userId = userConfig?.userId ?: "123123"
            val savedFile = ImageManager().saveAvatarImage("user_$userId", userAvatarBitmap)
            return savedFile?.absolutePath
        }
        return userConfig?.userAvatarPath
    }
}
