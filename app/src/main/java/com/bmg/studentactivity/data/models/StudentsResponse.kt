package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class StudentsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<Student>?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}
