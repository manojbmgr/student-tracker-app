package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.models.ActivitiesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("activities")
    suspend fun getActivities(
        @Query("day") day: String? = null,
        @Query("status") status: String? = null,
        @Query("studentEmail") studentEmail: String? = null
    ): Response<ActivitiesResponse>
}
