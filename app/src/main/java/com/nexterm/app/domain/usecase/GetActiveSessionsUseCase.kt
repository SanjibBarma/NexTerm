package com.nexterm.app.domain.usecase

import com.nexterm.app.data.local.entity.SessionEntity
import com.nexterm.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class GetActiveSessionsUseCase(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<List<SessionEntity>> {
        return sessionRepository.getActiveSessions()
    }
}
