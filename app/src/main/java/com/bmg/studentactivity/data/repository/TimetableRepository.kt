package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.TimetableRequest
import com.bmg.studentactivity.data.models.TimetableResponse

class TimetableRepository(private val apiService: ApiService) {
    suspend fun getTimetable(token: String, day: String? = null): Result<TimetableResponse> {
        return try {
            val response = apiService.getTimetable("Bearer $token", day)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch timetable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTimetableEntry(
        token: String,
        day: String,
        time: String,
        subject: String,
        description: String?,
        audioUrl: String?
    ): Result<TimetableResponse> {
        return try {
            val response = apiService.createTimetableEntry(
                "Bearer $token",
                TimetableRequest(day, time, subject, description, audioUrl)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTimetableEntry(
        token: String,
        id: String,
        day: String,
        time: String,
        subject: String,
        description: String?,
        audioUrl: String?
    ): Result<TimetableResponse> {
        return try {
            val response = apiService.updateTimetableEntry(
                "Bearer $token",
                id,
                TimetableRequest(day, time, subject, description, audioUrl)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTimetableEntry(token: String, id: String): Result<TimetableResponse> {
        return try {
            val response = apiService.deleteTimetableEntry("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete timetable entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

