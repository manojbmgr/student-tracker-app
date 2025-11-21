package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ActivitiesResponse

class ActivityRepository(private val apiService: ApiService) {
    suspend fun getActivities(
        studentEmail: String? = null,
        day: String? = null,
        status: String? = null
    ): Result<ActivitiesResponse> {
        return try {
            android.util.Log.d("ActivityRepository", "Calling activities API: studentEmail=$studentEmail, day=$day, status=$status")
            
            val response = apiService.getActivities(day, status, studentEmail)
            
            android.util.Log.d("ActivityRepository", "Activities API response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ActivityRepository", "Activities response received, students count: ${body.data?.students?.size ?: 0}")
                    Result.success(body)
                } else {
                    android.util.Log.e("ActivityRepository", "Activities response body is null")
                    Result.failure(Exception("Activities response body is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "HTTP ${response.code()}: ${response.message()}"
                android.util.Log.e("ActivityRepository", "Activities API error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Activities API exception: ${e.message}", e)
            Result.failure(Exception("Network error: ${e.message}", e))
        }
    }
}
