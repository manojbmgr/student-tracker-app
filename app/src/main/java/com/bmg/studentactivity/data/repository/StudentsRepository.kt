package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.StudentsResponse

class StudentsRepository(private val apiService: ApiService) {
    suspend fun getStudents(): Result<StudentsResponse> {
        return try {
            val response = apiService.getStudents()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch students"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

