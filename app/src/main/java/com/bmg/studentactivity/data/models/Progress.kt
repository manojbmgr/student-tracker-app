package com.bmg.studentactivity.data.models

import com.google.gson.annotations.SerializedName

data class ProgressResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: ProgressData?
) {
    val success: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class ProgressData(
    @SerializedName("overall_progress")
    val overallProgress: Float?,
    @SerializedName("subjects")
    val subjects: List<SubjectProgress>?
)

data class SubjectProgress(
    @SerializedName("subject")
    val subject: String,
    @SerializedName("progress")
    val progress: Float,
    @SerializedName("chapters")
    val chapters: List<ChapterProgress>?
)

data class ChapterProgress(
    @SerializedName("chapter")
    val chapter: String,
    @SerializedName("progress")
    val progress: Float
)
