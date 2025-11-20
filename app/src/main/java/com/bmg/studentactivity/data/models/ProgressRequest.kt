package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ProgressRequest(
    @SerializedName("studentEmail")
    val studentEmail: String? = null
)

