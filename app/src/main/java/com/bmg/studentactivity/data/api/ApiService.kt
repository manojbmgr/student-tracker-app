package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("auth/parent-login")
    suspend fun parentLogin(@Body request: LoginRequest): Response<ParentLoginResponse>
    
    @GET("activities")
    suspend fun getActivities(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<ActivitiesResponse>
    
    @GET("dashboard")
    suspend fun getParentDashboard(
        @Header("Authorization") token: String
    ): Response<ParentDashboardResponse>
    
    @POST("activities/complete")
    suspend fun markActivityComplete(
        @Header("Authorization") token: String,
        @Body request: CompleteRequest
    ): Response<CompleteResponse>
    
    @GET("timetable")
    suspend fun getTimetable(
        @Header("Authorization") token: String,
        @Query("day") day: String? = null
    ): Response<TimetableResponse>
    
    @POST("timetable")
    suspend fun createTimetableEntry(
        @Header("Authorization") token: String,
        @Body request: TimetableRequest
    ): Response<TimetableResponse>
    
    @PUT("timetable/{id}")
    suspend fun updateTimetableEntry(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: TimetableRequest
    ): Response<TimetableResponse>
    
    @DELETE("timetable/{id}")
    suspend fun deleteTimetableEntry(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<TimetableResponse>
    
    @GET("progress")
    suspend fun getProgress(
        @Header("Authorization") token: String
    ): Response<ProgressResponse>
    
    @GET("students")
    suspend fun getStudents(
        @Header("Authorization") token: String
    ): Response<StudentsResponse>
}
