package com.wenchen.yiyi.core.network.datasource.chat

import com.wenchen.yiyi.core.common.entity.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.network.base.BaseNetworkDataSource
import com.wenchen.yiyi.core.network.service.AiHubService
import javax.inject.Inject

/**
 * AI模型网络数据源实现类
 * 提供AI模型相关的网络数据操作
 */
class AiHubNetworkDataSourceImpl @Inject constructor(
    private val aiModelsApiService: AiHubService
) : AiHubNetworkDataSource, BaseNetworkDataSource() {

    override suspend fun getModels(): NetworkResponse<ModelsResponse> {
        val response = aiModelsApiService.getModels()
        return NetworkResponse(
            data = response,
            code = 1000,
            message = null
        )
    }
}