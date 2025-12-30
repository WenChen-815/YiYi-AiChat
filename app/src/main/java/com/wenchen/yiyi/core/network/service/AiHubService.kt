package com.wenchen.yiyi.core.network.service

import com.wenchen.yiyi.core.common.entity.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import retrofit2.http.GET

/**
 * AIHub服务接口
 * 提供AI模型相关的网络数据操作
 */
interface AiHubService {
    /**
     * 获取AI模型列表
     *
     * @return 模型列表响应
     */
    @GET("/v1/models")
    suspend fun getModels(): ModelsResponse
}