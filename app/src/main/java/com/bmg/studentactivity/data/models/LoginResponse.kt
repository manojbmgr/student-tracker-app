package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: LoginData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class LoginData(
    @SerializedName("token")
    val token: String,
    @SerializedName("studentId")
    val studentId: String,
    @SerializedName("userName")
    val name: String,
    @SerializedName("email")
    val email: String
)
