package com.bmg.studentactivity.di

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.repository.ActivityRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideActivityRepository(apiService: ApiService): ActivityRepository {
        return ActivityRepository(apiService)
    }
}

