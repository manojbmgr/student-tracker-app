package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.api.interceptors.AuthInterceptor
import com.bmg.studentactivity.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var apiKeyProvider: (() -> String?)? = null
    private var retrofitInstance: Retrofit? = null
    
    fun initialize(apiKeyProvider: () -> String?) {
        this.apiKeyProvider = apiKeyProvider
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = AuthInterceptor(
            apiKeyProvider = apiKeyProvider
        )
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofitInstance = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val apiService: ApiService
        get() {
            if (retrofitInstance == null) {
                throw IllegalStateException("ApiClient not initialized. Call initialize() first.")
            }
            return retrofitInstance!!.create(ApiService::class.java)
        }
}

