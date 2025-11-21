package com.bmg.studentactivity.data.api

import com.bmg.studentactivity.data.models.ActivitiesRequest
import com.bmg.studentactivity.data.models.ActivitiesResponse
import com.bmg.studentactivity.data.models.CompleteActivityResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("activities")
    suspend fun getActivities(
        @Body request: ActivitiesRequest
    ): Response<ActivitiesResponse>
    
    @POST("activities/complete")
    suspend fun completeActivity(
        @Body request: com.bmg.studentactivity.data.models.CompleteActivityRequest
    ): Response<CompleteActivityResponse>
    
    @Multipart
    @POST("activities/complete")
    suspend fun completeActivityWithImage(
        @Part("studentEmail") studentEmail: RequestBody,
        @Part("timetableId") timetableId: RequestBody?,
        @Part("activityId") activityId: RequestBody?,
        @Part("isCompleted") isCompleted: RequestBody,
        @Part("remark") remark: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Response<CompleteActivityResponse>
}
