package com.wenchen.yiyi.core.data.repository

import com.wenchen.yiyi.core.common.entity.ModelsResponse
import com.wenchen.yiyi.core.model.network.NetworkResponse
import com.wenchen.yiyi.core.network.datasource.chat.AiHubNetworkDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHubRepository @Inject constructor(
    private val aiHubNetworkDataSource: AiHubNetworkDataSource
) {
    /**
     * 获取AI模型列表
     *
     * @return 包含AI模型信息的响应对象
     */
    fun getModels(baseUrl: String? =  null, apiKey: String? =  null) : Flow<NetworkResponse<ModelsResponse>> = flow {
        emit(aiHubNetworkDataSource.getModels(baseUrl, apiKey))
    }.flowOn(Dispatchers.IO)
}