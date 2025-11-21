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
        // Initialize ApiClient with API key provider
        // The lambda ensures we always get the latest API key
        ApiClient.initialize(
            apiKeyProvider = { 
                val apiKey = tokenManager.getApiKey()
                android.util.Log.d("NetworkModule", "API key provider called, apiKey exists: ${apiKey != null}")
                apiKey
            }
        )
        android.util.Log.d("NetworkModule", "ApiService provided and initialized")
        return ApiClient.apiService
    }
}

