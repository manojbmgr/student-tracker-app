package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ParentDashboardResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: ParentDashboardData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class ParentDashboardData(
    @SerializedName("students")
    val students: List<StudentDashboardData>?
)

data class StudentDashboardData(
    @SerializedName("activities")
    val activities: ActivitiesInfo?
)

data class ActivitiesInfo(
    @SerializedName("list")
    val list: List<Activity>?
)
