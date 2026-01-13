package com.wenchen.yiyi.core.datastore.datasource.config

import com.tencent.mmkv.MMKV
import com.wenchen.yiyi.core.model.config.UserConfig
import com.wenchen.yiyi.core.state.UserState
import com.wenchen.yiyi.core.util.storage.MMKVUtils
import com.wenchen.yiyi.core.util.storage.getObject
import com.wenchen.yiyi.core.util.storage.putObject
import timber.log.Timber
import javax.inject.Inject

class UserConfigStoreDataSourceImpl @Inject constructor(
    private val userState: UserState
) : UserConfigStoreDataSource {

    // 延迟获取 MMKV 实例，确保 userState 已初始化
    private fun getUserConfigMMKV(): MMKV {
        val userId = userState.userId.value
        require(userId.isNotEmpty()) { "用户ID不能为空，无法获取配置存储" }
        Timber.tag("UserConfig").d("userId = $userId")
        return MMKVUtils.getInstance("user_config_$userId")
    }
    override suspend fun saveUserConfig(userConfig: UserConfig) {
        getUserConfigMMKV().putObject(UserConfigStoreDataSource.USER_CONFIG_KEY, userConfig)
    }

    override suspend fun getUserConfig(): UserConfig? {
        return getUserConfigMMKV().getObject<UserConfig>(UserConfigStoreDataSource.USER_CONFIG_KEY)
    }

    override suspend fun clearUserConfig() {
        getUserConfigMMKV().remove(UserConfigStoreDataSource.USER_CONFIG_KEY)
    }

}
