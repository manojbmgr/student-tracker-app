package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ProgressResponse

class ProgressRepository(private val apiService: ApiService) {
    suspend fun getProgress(token: String): Result<ProgressResponse> {
        return try {
            val response = apiService.getProgress("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch progress"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

