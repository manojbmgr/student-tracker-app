package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("auth/parent-login")
    suspend fun parentLogin(@Body request: LoginRequest): Response<ParentLoginResponse>
    
    @POST("activities")
    suspend fun getActivities(@Body request: ActivitiesRequest): Response<ActivitiesResponse>
    
    @POST("dashboard")
    suspend fun getParentDashboard(@Body request: DashboardRequest): Response<ParentDashboardResponse>
    
    @POST("activities/complete")
    suspend fun markActivityComplete(@Body request: CompleteRequest): Response<CompleteResponse>
    
    @GET("timetable")
    suspend fun getTimetable(
        @Query("day") day: String? = null
    ): Response<TimetableResponse>
    
    @POST("timetable")
    suspend fun createTimetableEntry(@Body request: TimetableRequest): Response<TimetableResponse>
    
    @PUT("timetable/{id}")
    suspend fun updateTimetableEntry(
        @Path("id") id: String,
        @Body request: TimetableRequest
    ): Response<TimetableResponse>
    
    @DELETE("timetable/{id}")
    suspend fun deleteTimetableEntry(@Path("id") id: String): Response<TimetableResponse>
    
    @POST("progress")
    suspend fun getProgress(@Body request: ProgressRequest): Response<ProgressResponse>
    
    @GET("students")
    suspend fun getStudents(): Response<StudentsResponse>
}
