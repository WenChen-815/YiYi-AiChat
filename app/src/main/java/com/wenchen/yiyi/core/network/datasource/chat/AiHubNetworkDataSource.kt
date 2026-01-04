package com.wenchen.yiyi.core.network.datasource.chat

import com.wenchen.yiyi.core.database.entity.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse

/**
 * AI模型网络数据源接口
 * 提供AI模型相关的网络数据操作
 */
interface AiHubNetworkDataSource {
    /**
     * 获取AI模型列表
     *
     * @return 模型列表响应
     */
    suspend fun getModels(baseUrl: String? =  null, apiKey: String? =  null): NetworkResponse<ModelsResponse>
}