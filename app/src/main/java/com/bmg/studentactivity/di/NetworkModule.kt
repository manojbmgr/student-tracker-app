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
        // Initialize ApiClient with token providers
        // The lambdas ensure we always get the latest token/API key
        ApiClient.initialize(
            tokenProvider = { 
                val token = tokenManager.getToken()
                android.util.Log.d("NetworkModule", "Token provider called, token exists: ${token != null}")
                token
            },
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

