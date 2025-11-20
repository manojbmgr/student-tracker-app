package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ParentLoginResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: ParentLoginData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class ParentLoginData(
    @SerializedName("token")
    val token: String,
    @SerializedName("expiresIn")
    val expiresIn: Int? = null,
    @SerializedName("apiKey")
    val apiKey: String? = null,
    @SerializedName("parentId")
    val parentId: Int,
    @SerializedName("userName")
    val userName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("userType")
    val userType: String? = "parent",
    @SerializedName("students")
    val students: List<Student>?,
    @SerializedName("totalStudents")
    val totalStudents: Int? = null,
    @SerializedName("authentication")
    val authentication: AuthenticationInfo? = null
)

data class AuthenticationInfo(
    @SerializedName("jwtToken")
    val jwtToken: String? = null,
    @SerializedName("apiKey")
    val apiKey: String? = null,
    @SerializedName("note")
    val note: String? = null
)

data class Student(
    @SerializedName("studentId", alternate = ["id"])
    val studentId: Int,
    @SerializedName("studentName", alternate = ["name"])
    val studentName: String,
    @SerializedName("class", alternate = ["className"])
    val className: Int? = null
)
