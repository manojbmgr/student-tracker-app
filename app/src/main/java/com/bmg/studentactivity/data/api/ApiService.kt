package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.models.ActivitiesRequest
import com.bmg.studentactivity.data.models.ActivitiesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("activities")
    suspend fun getActivities(
        @Body request: ActivitiesRequest
    ): Response<ActivitiesResponse>
}
