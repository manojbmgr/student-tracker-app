package com.bmg.studentactivity.data.repository

import android.content.Context
import android.net.Uri
import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.ActivitiesRequest
import com.bmg.studentactivity.data.models.ActivitiesResponse
import com.bmg.studentactivity.data.models.CompleteActivityRequest
import com.bmg.studentactivity.data.models.CompleteActivityResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ActivityRepository(
    private val apiService: ApiService,
    private val context: Context
) {
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
    
    suspend fun completeActivity(
        studentEmail: String,
        timetableId: Int? = null,
        activityId: Int? = null,
        isCompleted: Boolean = true,
        remark: String? = null
    ): Result<CompleteActivityResponse> {
        return try {
            android.util.Log.d("ActivityRepository", "=== Starting complete activity API call ===")
            android.util.Log.d("ActivityRepository", "studentEmail=$studentEmail, timetableId=$timetableId, activityId=$activityId, isCompleted=$isCompleted")
            
            val request = CompleteActivityRequest(
                studentEmail = studentEmail,
                timetableId = timetableId,
                activityId = activityId,
                isCompleted = isCompleted,
                remark = remark
            )
            
            val response = apiService.completeActivity(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ActivityRepository", "Activity completion successful")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Complete activity response body is null"))
                }
            } else {
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorBody ?: "HTTP ${response.code()}: ${response.message()}"
                android.util.Log.e("ActivityRepository", "Complete activity error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Exception in completeActivity: ${e.message}", e)
            Result.failure(Exception("Failed to update activity: ${e.message ?: "Unknown error"}", e))
        }
    }
    
    suspend fun completeActivityWithImage(
        studentEmail: String,
        timetableId: Int? = null,
        activityId: Int? = null,
        isCompleted: Boolean = true,
        remark: String? = null,
        imageUri: Uri? = null
    ): Result<CompleteActivityResponse> {
        return try {
            android.util.Log.d("ActivityRepository", "=== Starting complete activity with image API call ===")
            
            val studentEmailBody = studentEmail.toRequestBody("text/plain".toMediaTypeOrNull())
            val timetableIdBody = timetableId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val activityIdBody = activityId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val isCompletedBody = isCompleted.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val remarkBody = remark?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            var imagePart: MultipartBody.Part? = null
            if (imageUri != null) {
                try {
                    val imageFile = getFileFromUri(imageUri)
                    if (imageFile != null && imageFile.exists()) {
                        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("completionImage", imageFile.name, requestFile)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ActivityRepository", "Error processing image: ${e.message}", e)
                }
            }
            
            val response = apiService.completeActivityWithImage(
                studentEmail = studentEmailBody,
                timetableId = timetableIdBody,
                activityId = activityIdBody,
                isCompleted = isCompletedBody,
                remark = remarkBody,
                image = imagePart
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ActivityRepository", "Activity completion with image successful")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Complete activity response body is null"))
                }
            } else {
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorBody ?: "HTTP ${response.code()}: ${response.message()}"
                android.util.Log.e("ActivityRepository", "Complete activity with image error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Exception in completeActivityWithImage: ${e.message}", e)
            Result.failure(Exception("Failed to update activity with image: ${e.message ?: "Unknown error"}", e))
        }
    }
    
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
                tempFile
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error getting file from URI: ${e.message}", e)
            null
        }
    }
}
