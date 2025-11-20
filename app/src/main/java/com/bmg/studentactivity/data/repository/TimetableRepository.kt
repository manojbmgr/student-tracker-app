package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.TimetableRequest
import com.bmg.studentactivity.data.models.TimetableResponse

class TimetableRepository(private val apiService: ApiService) {
    suspend fun getTimetable(day: String? = null): Result<TimetableResponse> {
        return try {
            val response = apiService.getTimetable(day)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: response.message() ?: "Failed to fetch timetable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTimetableEntry(
        day: String,
        time: String,
        subject: String,
        description: String?,
        audioUrl: String?
    ): Result<TimetableResponse> {
        return try {
            val response = apiService.createTimetableEntry(
                TimetableRequest(day, time, subject, description, audioUrl)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: response.message() ?: "Failed to create timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTimetableEntry(
        id: String,
        day: String,
        time: String,
        subject: String,
        description: String?,
        audioUrl: String?
    ): Result<TimetableResponse> {
        return try {
            val response = apiService.updateTimetableEntry(
                id,
                TimetableRequest(day, time, subject, description, audioUrl)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: response.message() ?: "Failed to update timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTimetableEntry(id: String): Result<TimetableResponse> {
        return try {
            val response = apiService.deleteTimetableEntry(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: response.message() ?: "Failed to delete timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

