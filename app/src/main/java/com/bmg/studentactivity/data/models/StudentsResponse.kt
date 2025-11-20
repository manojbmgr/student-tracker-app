package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class StudentsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: StudentsData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class StudentsData(
    @SerializedName("students")
    val students: List<Student>?,
    @SerializedName("total")
    val total: Int? = null
)
