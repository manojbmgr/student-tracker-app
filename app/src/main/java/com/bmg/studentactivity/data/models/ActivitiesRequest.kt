package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ActivitiesRequest(
    @SerializedName("studentEmail")
    val studentEmail: String? = null,
    @SerializedName("day")
    val day: String? = null,
    @SerializedName("status")
    val status: String? = null
)


