package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class CompleteRequest(
    @SerializedName("activity_id")
    val activityId: String
)

data class CompleteResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

