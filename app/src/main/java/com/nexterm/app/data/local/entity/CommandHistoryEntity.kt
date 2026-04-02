package com.nexterm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val command: String,
    val output: String = "",
    val exitCode: Int = 0,
    val executedAt: Date = Date(),
    val executionTimeMs: Long = 0
)