package com.bmg.studentactivity.data.api.interceptors

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val apiKeyProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = apiKeyProvider()
        
        val requestBuilder = originalRequest.newBuilder()
        
        if (apiKey != null) {
            requestBuilder.header("X-API-Key", apiKey)
            android.util.Log.d("AuthInterceptor", "Adding X-API-Key header for ${originalRequest.url}")
        } else {
            android.util.Log.w("AuthInterceptor", "No API key available for ${originalRequest.url}")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}

