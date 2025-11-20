package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class TimetableResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<TimetableEntry>?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class TimetableEntry(
    @SerializedName("id")
    val id: String,
    @SerializedName("day")
    val day: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("audio_url")
    val audioUrl: String?
)

data class TimetableRequest(
    @SerializedName("day")
    val day: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("audio_url")
    val audioUrl: String?
)
