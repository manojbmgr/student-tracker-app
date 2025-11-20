package com.bmg.studentactivity.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey
    val id: String,
    val day: String,
    val time: String,
    val subject: String,
    val description: String?,
    val audioUrl: String?
)

