package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val organization: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val language: String, // "English", "Kannada", "Kannada-English"
    val category: String, // "Spam", "Delivery", "Personal", "Work", "Urgent", "Unknown"
    val summary: String,
    val importantDetails: String,
    val transcriptJson: String, // serialized JSON or pipe-delimited
    val hasRecording: Boolean = true,
    val recordingDuration: Int = 0,
    val isTrusted: Boolean = false,
    val isSpam: Boolean = false,
    val screeningStatus: String = "Screened by AI"
)
