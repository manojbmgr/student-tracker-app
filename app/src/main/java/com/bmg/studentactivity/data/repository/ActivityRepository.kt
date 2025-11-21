package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ActivitiesRequest
import com.bmg.studentactivity.data.models.ActivitiesResponse

class ActivityRepository(private val apiService: ApiService) {
    suspend fun getActivities(
        studentEmail: String? = null,
        day: String? = null,
        status: String? = null
    ): Result<ActivitiesResponse> {
        return try {
            android.util.Log.d("ActivityRepository", "=== Starting API call ===")
            android.util.Log.d("ActivityRepository", "Parameters: studentEmail=$studentEmail, day=$day, status=$status")
            android.util.Log.d("ActivityRepository", "ApiService instance: ${apiService != null}")
            android.util.Log.d("ActivityRepository", "Base URL: ${com.bmg.studentactivity.utils.Constants.BASE_URL}")
            android.util.Log.d("ActivityRepository", "Full endpoint: ${com.bmg.studentactivity.utils.Constants.BASE_URL}activities")
            
            val request = ActivitiesRequest(
                studentEmail = studentEmail,
                day = day,
                status = status
            )
            android.util.Log.d("ActivityRepository", "Request body: studentEmail=${request.studentEmail}, day=${request.day}, status=${request.status}")
            
            val response = try {
                android.util.Log.d("ActivityRepository", "Calling apiService.getActivities() with POST method...")
                android.util.Log.d("ActivityRepository", "This should trigger OkHttp logging interceptor")
                apiService.getActivities(request)
            } catch (e: Exception) {
                android.util.Log.e("ActivityRepository", "Exception during API call: ${e.javaClass.simpleName}", e)
                throw e
            }
            
            android.util.Log.d("ActivityRepository", "API call completed")
            android.util.Log.d("ActivityRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ActivityRepository", "Response body received")
                    android.util.Log.d("ActivityRepository", "Status: ${body.status}, Students count: ${body.data?.students?.size ?: 0}")
                    Result.success(body)
                } else {
                    android.util.Log.e("ActivityRepository", "Response body is null")
                    Result.failure(Exception("Activities response body is null"))
                }
            } else {
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (e: Exception) {
                    android.util.Log.e("ActivityRepository", "Error reading error body: ${e.message}")
                    null
                }
                val errorMessage = errorBody ?: "HTTP ${response.code()}: ${response.message()}"
                android.util.Log.e("ActivityRepository", "API error response: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("ActivityRepository", "JSON parsing error: ${e.message}", e)
            android.util.Log.e("ActivityRepository", "JSON error at: ${e.stackTrace.firstOrNull()}")
            Result.failure(Exception("Failed to parse response: ${e.message}", e))
        } catch (e: java.io.IOException) {
            android.util.Log.e("ActivityRepository", "Network IO error: ${e.message}", e)
            Result.failure(Exception("Network error: ${e.message}. Please check your internet connection.", e))
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("ActivityRepository", "Exception message: ${e.message}", e)
            val errorMsg = when {
                e.message?.contains("converter", ignoreCase = true) == true -> 
                    "Failed to parse server response. Please check your API key and try again."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "No internet connection. Please check your network."
                e.message?.contains("IllegalStateException", ignoreCase = true) == true ->
                    "API client not initialized. Please restart the app."
                else -> "Error: ${e.message ?: "Unknown error"}"
            }
            Result.failure(Exception(errorMsg, e))
        }
    }
}
