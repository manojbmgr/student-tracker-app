package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ActivitiesResponse
import com.bmg.studentactivity.data.models.CompleteRequest
import com.bmg.studentactivity.data.models.CompleteResponse
import com.bmg.studentactivity.data.models.ParentDashboardResponse

class ActivityRepository(private val apiService: ApiService) {
    suspend fun getActivities(token: String, status: String? = null): Result<ActivitiesResponse> {
        return try {
            val response = apiService.getActivities("Bearer $token", status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch activities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getParentDashboard(token: String): Result<ParentDashboardResponse> {
        return try {
            val response = apiService.getParentDashboard("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markActivityComplete(token: String, activityId: String): Result<CompleteResponse> {
        return try {
            val response = apiService.markActivityComplete("Bearer $token", CompleteRequest(activityId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark activity complete"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
