package com.nexterm.app.data.local.dao

import androidx.room.*
import com.nexterm.app.data.local.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history WHERE sessionId = :sessionId ORDER BY executedAt DESC")
    fun getCommandHistory(sessionId: Long): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY executedAt DESC LIMIT :limit")
    fun getRecentCommands(limit: Int = 100): Flow<List<CommandHistoryEntity>>

    @Query("SELECT DISTINCT command FROM command_history ORDER BY executedAt DESC LIMIT :limit")
    suspend fun getUniqueCommands(limit: Int = 50): List<String>

    @Query("SELECT * FROM command_history WHERE command LIKE '%' || :query || '%' ORDER BY executedAt DESC")
    fun searchCommands(query: String): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandHistoryEntity): Long

    @Delete
    suspend fun deleteCommand(command: CommandHistoryEntity)

    @Query("DELETE FROM command_history WHERE sessionId = :sessionId")
    suspend fun clearSessionHistory(sessionId: Long)

    @Query("DELETE FROM command_history")
    suspend fun clearAllHistory()
}