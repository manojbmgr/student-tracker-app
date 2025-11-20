package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ActivitiesRequest
import com.bmg.studentactivity.data.models.ActivitiesResponse
import com.bmg.studentactivity.data.models.CompleteRequest
import com.bmg.studentactivity.data.models.CompleteResponse
import com.bmg.studentactivity.data.models.DashboardRequest
import com.bmg.studentactivity.data.models.ParentDashboardResponse

class ActivityRepository(private val apiService: ApiService) {
    suspend fun getActivities(
        studentEmail: String? = null,
        day: String? = null,
        status: String? = null
    ): Result<ActivitiesResponse> {
        return try {
            val request = ActivitiesRequest(studentEmail, day, status)
            val response = apiService.getActivities(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch activities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getParentDashboard(
        studentEmail: String? = null,
        day: String? = null
    ): Result<ParentDashboardResponse> {
        return try {
            // Always create a request object, even if all fields are null
            // This ensures the POST request body is sent
            val request = DashboardRequest(
                studentEmail = studentEmail,
                day = day
            )
            val response = apiService.getParentDashboard(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: response.message() ?: "Failed to fetch dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markActivityComplete(activityId: String): Result<CompleteResponse> {
        return try {
            val response = apiService.markActivityComplete(CompleteRequest(activityId))
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
