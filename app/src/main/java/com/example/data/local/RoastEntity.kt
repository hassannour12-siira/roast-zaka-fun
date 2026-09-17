package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roast_history")
data class RoastEntity(
    @PrimaryKey
    val id: String,
    val candidateName: String,
    val candidateHeadline: String,
    val openingLine: String,
    val overallScore: Int,
    val previousScore: Int? = null,
    val biggestRedFlag: String,
    val intensity: String,
    val targetRole: String,
    val cvSnippet: String,
    val timestamp: Long
)
