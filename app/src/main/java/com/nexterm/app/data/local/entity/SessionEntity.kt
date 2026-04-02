package com.nexterm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shellPath: String = "/system/bin/sh",
    val workingDirectory: String,
    val environmentVariables: String = "",
    val createdAt: Date = Date(),
    val lastAccessedAt: Date = Date(),
    val isActive: Boolean = true
)