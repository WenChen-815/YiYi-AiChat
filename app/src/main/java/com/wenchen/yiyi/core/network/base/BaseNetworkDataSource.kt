package com.wenchen.yiyi.core.network.base

import retrofit2.Retrofit

/**
 * 网络数据源基类
 * 提供所有网络数据源实现的通用功能
 */
abstract class BaseNetworkDataSource {
    /**
     * 创建API服务实例的辅助方法
     *
     * @param T API服务接口类型
     * @return API服务实例
     */
    protected inline fun <reified T> Retrofit.createService(): T {
        return this.create(T::class.java)
    }
} 