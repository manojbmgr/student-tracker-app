package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ActivitiesResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<Activity>?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class Activity(
    @SerializedName("id", alternate = ["timetableId", "activityId"])
    val id: String,
    @SerializedName("title", alternate = ["activityName", "subject"])
    val title: String,
    @SerializedName("description", alternate = ["notes"])
    val description: String?,
    @SerializedName("subject")
    val subject: String?,
    @SerializedName("chapter")
    val chapter: String?,
    @SerializedName("due_date", alternate = ["endTime"])
    val dueDate: String?,
    @SerializedName("status")
    val _status: String?,
    @SerializedName("isCompleted")
    val isCompleted: Boolean? = false,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: String?
) {
    val status: String
        get() = _status ?: if (isCompleted == true) "Completed" else "Pending"
}
