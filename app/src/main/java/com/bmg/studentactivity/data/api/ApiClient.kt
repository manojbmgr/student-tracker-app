package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.api.interceptors.AuthInterceptor
import com.bmg.studentactivity.utils.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var apiKeyProvider: (() -> String?)? = null
    private var retrofitInstance: Retrofit? = null
    
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()
    
    fun initialize(apiKeyProvider: () -> String?) {
        this.apiKeyProvider = apiKeyProvider
        
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                android.util.Log.d("OkHttp", message)
            }
        }).apply {
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
            .addConverterFactory(GsonConverterFactory.create(gson))
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

