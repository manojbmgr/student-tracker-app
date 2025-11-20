package com.bmg.studentactivity.data.api.interceptors

import com.bmg.studentactivity.utils.Constants
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val apiKeyProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()
        val apiKey = apiKeyProvider()
        
        val requestBuilder = originalRequest.newBuilder()
        
        // Prefer JWT token over API key
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
            android.util.Log.d("AuthInterceptor", "Adding Authorization header for ${originalRequest.url}")
        } else if (apiKey != null) {
            requestBuilder.header("X-API-Key", apiKey)
            android.util.Log.d("AuthInterceptor", "Adding X-API-Key header for ${originalRequest.url}")
        } else {
            android.util.Log.w("AuthInterceptor", "No token or API key available for ${originalRequest.url}")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}

