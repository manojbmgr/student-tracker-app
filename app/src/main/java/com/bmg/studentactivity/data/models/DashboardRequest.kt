package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class DashboardRequest(
    @SerializedName("studentEmail")
    val studentEmail: String? = null,
    @SerializedName("day")
    val day: String? = null
)

