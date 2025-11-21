package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class CompleteActivityResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: CompleteActivityData? = null
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class CompleteActivityData(
    @SerializedName("timetableId")
    val timetableId: Int? = null,
    @SerializedName("activityId")
    val activityId: Int? = null,
    @SerializedName("isCompleted")
    val isCompleted: Boolean,
    @SerializedName("completedAt")
    val completedAt: String? = null,
    @SerializedName("subject")
    val subject: String? = null,
    @SerializedName("activityName")
    val activityName: String? = null,
    @SerializedName("activityType")
    val activityType: String? = null,
    @SerializedName("completionImage")
    val completionImage: String? = null,
    @SerializedName("completionImageUrl")
    val completionImageUrl: String? = null,
    @SerializedName("remark")
    val remark: String? = null
)

