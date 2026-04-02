package com.nexterm.app.data.local.dao

import androidx.room.*
import com.nexterm.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY lastAccessedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY lastAccessedAt DESC")
    fun getActiveSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("UPDATE sessions SET lastAccessedAt = :date WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, date: java.util.Date)

    @Query("UPDATE sessions SET isActive = :isActive WHERE id = :id")
    suspend fun setSessionActive(id: Long, isActive: Boolean)
}