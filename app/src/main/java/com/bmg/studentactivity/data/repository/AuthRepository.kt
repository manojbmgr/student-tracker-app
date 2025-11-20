package com.bmg.studentactivity.data.repository

import com.bmg.studentactivity.data.api.ApiService
import com.bmg.studentactivity.data.models.LoginRequest
import com.bmg.studentactivity.data.models.LoginResponse
import com.bmg.studentactivity.data.models.ParentLoginResponse

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun parentLogin(email: String, password: String): Result<ParentLoginResponse> {
        return try {
            val response = apiService.parentLogin(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

