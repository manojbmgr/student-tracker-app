package com.bmg.studentactivity.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val subject: String?,
    val chapter: String?,
    val dueDate: String?,
    val status: String,
    val createdAt: String?
)

