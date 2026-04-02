package com.nexterm.app.data.repository

import com.nexterm.app.data.local.dao.CommandHistoryDao
import com.nexterm.app.data.local.dao.SessionDao
import com.nexterm.app.data.local.entity.CommandHistoryEntity
import com.nexterm.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface SessionRepository {
    fun getAllSessions(): Flow<List<SessionEntity>>
    fun getActiveSessions(): Flow<List<SessionEntity>>
    suspend fun getSessionById(id: Long): SessionEntity?
    suspend fun createSession(name: String, workingDirectory: String): Long
    suspend fun updateSession(session: SessionEntity)
    suspend fun deleteSession(id: Long)
    suspend fun updateLastAccessed(id: Long)
    suspend fun setSessionActive(id: Long, isActive: Boolean)
    fun getCommandHistory(sessionId: Long): Flow<List<CommandHistoryEntity>>
    suspend fun addCommandToHistory(sessionId: Long, command: String, output: String, exitCode: Int, executionTimeMs: Long)
    suspend fun getUniqueCommands(limit: Int = 50): List<String>
    suspend fun clearHistory(sessionId: Long)
}

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val commandHistoryDao: CommandHistoryDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    override fun getActiveSessions(): Flow<List<SessionEntity>> = sessionDao.getActiveSessions()

    override suspend fun getSessionById(id: Long): SessionEntity? = sessionDao.getSessionById(id)

    override suspend fun createSession(name: String, workingDirectory: String): Long {
        val session = SessionEntity(
            name = name,
            workingDirectory = workingDirectory
        )
        return sessionDao.insertSession(session)
    }

    override suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
    }

    override suspend fun deleteSession(id: Long) {
        sessionDao.deleteSessionById(id)
        commandHistoryDao.clearSessionHistory(id)
    }

    override suspend fun updateLastAccessed(id: Long) {
        sessionDao.updateLastAccessed(id, Date())
    }

    override suspend fun setSessionActive(id: Long, isActive: Boolean) {
        sessionDao.setSessionActive(id, isActive)
    }

    override fun getCommandHistory(sessionId: Long): Flow<List<CommandHistoryEntity>> =
        commandHistoryDao.getCommandHistory(sessionId)

    override suspend fun addCommandToHistory(
        sessionId: Long,
        command: String,
        output: String,
        exitCode: Int,
        executionTimeMs: Long
    ) {
        val historyEntry = CommandHistoryEntity(
            sessionId = sessionId,
            command = command,
            output = output,
            exitCode = exitCode,
            executionTimeMs = executionTimeMs
        )
        commandHistoryDao.insertCommand(historyEntry)
    }

    override suspend fun getUniqueCommands(limit: Int): List<String> =
        commandHistoryDao.getUniqueCommands(limit)

    override suspend fun clearHistory(sessionId: Long) {
        commandHistoryDao.clearSessionHistory(sessionId)
    }
}