package com.bmg.studentactivity.di

import com.bmg.studentactivity.data.api.ApiClient
import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiService(tokenManager: TokenManager): ApiService {
        ApiClient.initialize(
            tokenProvider = { tokenManager.getToken() },
            apiKeyProvider = { tokenManager.getApiKey() }
        )
        return ApiClient.apiService
    }
}

