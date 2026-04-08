package com.nexterm.app.domain.usecase

import com.nexterm.app.data.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateSessionUseCaseTest {

    private val sessionRepository = mockk<SessionRepository>()
    private val useCase = CreateSessionUseCase(sessionRepository)

    @Test
    fun `invoke should return session id from repository`() = runTest {
        // Given
        val name = "Test Session"
        val workingDirectory = "/home"
        val expectedId = 1L
        coEvery { sessionRepository.createSession(name, workingDirectory) } returns expectedId

        // When
        val result = useCase(name, workingDirectory)

        // Then
        assertEquals(expectedId, result)
    }
}
