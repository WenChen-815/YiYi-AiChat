package com.wenchen.yiyi.core.util

import com.wenchen.yiyi.core.util.business.ChatUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    @Singleton
    fun provideChatUtils(): ChatUtils {
        return ChatUtils()
    }
}