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
    @SerializedName("parentId")
    val parentId: String,
    @SerializedName("userName")
    val userName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("students")
    val students: List<Student>?
)

data class Student(
    @SerializedName("studentId", alternate = ["id"])
    val studentId: String,
    @SerializedName("studentName", alternate = ["name"])
    val studentName: String,
    @SerializedName("class", alternate = ["className", "email"])
    val className: String
)
