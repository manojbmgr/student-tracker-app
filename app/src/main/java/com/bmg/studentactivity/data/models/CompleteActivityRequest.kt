package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class CompleteActivityRequest(
    @SerializedName("studentEmail")
    val studentEmail: String,
    @SerializedName("timetableId")
    val timetableId: Int? = null,
    @SerializedName("activityId")
    val activityId: Int? = null,
    @SerializedName("isCompleted")
    val isCompleted: Boolean = true,
    @SerializedName("remark")
    val remark: String? = null
)

