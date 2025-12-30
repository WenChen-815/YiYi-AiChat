package com.wenchen.yiyi.core.state.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 应用状态模块，提供应用级协程作用域
 */
@Module
@InstallIn(SingletonComponent::class)
object AppStateModule {

    /**
     * 提供应用级别的协程作用域
     * SupervisorJob 确保子协程失败不会终止整个作用域
     *
     * @return 应用级 CoroutineScope
     */
    @ApplicationScope
    @Singleton
    @Provides
    fun providesApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/**
 * 应用级协程作用域限定符
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
