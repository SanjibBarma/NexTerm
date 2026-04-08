package com.nexterm.app.domain.usecase

import com.nexterm.app.data.repository.SessionRepository

class CreateSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(name: String, workingDirectory: String): Long {
        return sessionRepository.createSession(name, workingDirectory)
    }
}
