package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ActivitiesResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: ActivitiesData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class ActivitiesData(
    @SerializedName("students")
    val students: List<StudentActivities>? = null,
    @SerializedName("activities")
    val activities: List<Activity>? = null,
    @SerializedName("statistics")
    val statistics: ActivityStatistics? = null,
    @SerializedName("overallStatistics")
    val overallStatistics: ActivityStatistics? = null,
    @SerializedName("currentTime")
    val currentTime: String? = null,
    @SerializedName("currentDay")
    val currentDay: String? = null,
    @SerializedName("date")
    val date: String? = null
)

data class StudentActivities(
    @SerializedName("studentEmail")
    val studentEmail: String,
    @SerializedName("studentName")
    val studentName: String? = null,
    @SerializedName("profileImg")
    val profileImg: String? = null,
    @SerializedName("profileImgUrl")
    val profileImgUrl: String? = null,
    @SerializedName("activities")
    val activities: List<Activity>,
    @SerializedName("statistics")
    val statistics: ActivityStatistics
)

data class ActivityStatistics(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("completed")
    val completed: Int = 0,
    @SerializedName("pending")
    val pending: Int = 0,
    @SerializedName("overdue")
    val overdue: Int = 0,
    @SerializedName("completionPercentage")
    val completionPercentage: Double = 0.0
)

data class Activity(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("timetableId")
    val timetableId: Int? = null,
    @SerializedName("activityId")
    val activityId: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("activityName")
    val activityName: String? = null,
    @SerializedName("subject")
    val subject: String? = null,
    @SerializedName("activityType")
    val activityType: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("chapter")
    val chapter: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("startTime")
    val startTime: String? = null,
    @SerializedName("endTime")
    val endTime: String? = null,
    @SerializedName("dayOfWeek")
    val dayOfWeek: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("teacher")
    val teacher: String? = null,
    @SerializedName("status")
    val _status: String? = null,
    @SerializedName("isCompleted")
    val isCompleted: Boolean? = false,
    @SerializedName("isCompletedToday")
    val isCompletedToday: Boolean? = false,
    @SerializedName("isOverdue")
    val isOverdue: Boolean? = false,
    @SerializedName("completedAt")
    val completedAt: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("studentEmail")
    val studentEmail: String? = null,
    @SerializedName("studentName")
    val studentName: String? = null,
    @SerializedName("alarmAudio")
    val alarmAudio: String? = null,
    @SerializedName("alarmAudioUrl")
    val alarmAudioUrl: String? = null
) {
    val displayTitle: String
        get() = activityName ?: subject ?: title ?: ""
    
    val status: String
        get() {
            return when {
                isCompleted == true || isCompletedToday == true -> "Completed"
                isOverdue == true -> "Overdue"
                else -> "Pending"
            }
        }
    
    val activityIdString: String
        get() = activityId?.toString() ?: timetableId?.toString() ?: id ?: ""
}
