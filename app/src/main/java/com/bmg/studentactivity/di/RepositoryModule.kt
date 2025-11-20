package com.bmg.studentactivity.di

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.repository.*
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
    fun provideAuthRepository(apiService: ApiService): AuthRepository {
        return AuthRepository(apiService)
    }
    
    @Provides
    @Singleton
    fun provideActivityRepository(apiService: ApiService): ActivityRepository {
        return ActivityRepository(apiService)
    }
    
    @Provides
    @Singleton
    fun provideTimetableRepository(apiService: ApiService): TimetableRepository {
        return TimetableRepository(apiService)
    }
    
    @Provides
    @Singleton
    fun provideProgressRepository(apiService: ApiService): ProgressRepository {
        return ProgressRepository(apiService)
    }
    
    @Provides
    @Singleton
    fun provideStudentsRepository(apiService: ApiService): StudentsRepository {
        return StudentsRepository(apiService)
    }
}

