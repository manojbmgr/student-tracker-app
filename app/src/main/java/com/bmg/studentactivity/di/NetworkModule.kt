package com.bmg.studentactivity.di

import com.bmg.studentactivity.data.api.ApiClient
import com.bmg.studentactivity.data.api.ApiService
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
    fun provideApiService(): ApiService {
        return ApiClient.apiService
    }
}

