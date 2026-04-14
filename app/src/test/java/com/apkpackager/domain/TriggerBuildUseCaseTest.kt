package com.apkpackager.domain

import com.apkpackager.data.github.GitHubRepository
import com.apkpackager.data.github.model.WorkflowRunDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerBuildUseCaseTest {

    private val githubRepository = mockk<GitHubRepository>()
    private val useCase = TriggerBuildUseCase(githubRepository)

    private fun fakeRun(
        id: Long = 42L,
        status: String = "completed",
        conclusion: String? = "success",
        createdAt: String = "2099-01-01T00:00:00Z",
    ) = WorkflowRunDto(
        id = id,
        status = status,
        conclusion = conclusion,
        createdAt = createdAt,
        htmlUrl = "https://github.com/owner/repo/actions/runs/$id",
    )

    @Test
    fun `emits Error when workflow is missing`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns
            Result.failure(Exception(GitHubRepository.WORKFLOW_MISSING_MESSAGE))

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
        val error = steps.last() as BuildStep.Error
        assertTrue(error.message.contains("build-apk.yml"))
    }

    @Test
    fun `emits Error when trigger fails`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.getLatestRunId(any(), any(), any()) } returns 100L
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns
            Result.failure(Exception("422 Unprocessable"))

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
    }

    @Test
    fun `emits Success for a completed successful run`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.getLatestRunId(any(), any(), any()) } returns 100L
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfterId(any(), any(), any(), 100L) } returns fakeRun(id = 101L)
        coEvery { githubRepository.getRun(any(), any(), 101L) } returns fakeRun(id = 101L)

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.any { it is BuildStep.WorkflowReady })
        assertTrue(steps.last() is BuildStep.Success)
        assertEquals(101L, (steps.last() as BuildStep.Success).runId)
    }

    @Test
    fun `emits Failure when run concludes with failure`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.getLatestRunId(any(), any(), any()) } returns 100L
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfterId(any(), any(), any(), 100L) } returns
            fakeRun(id = 101L, conclusion = "failure")
        coEvery { githubRepository.getRun(any(), any(), 101L) } returns
            fakeRun(id = 101L, conclusion = "failure")

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Failure)
        assertEquals("failure", (steps.last() as BuildStep.Failure).conclusion)
    }

    @Test
    fun `triggers workflow dispatch exactly once`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.getLatestRunId(any(), any(), any()) } returns 100L
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfterId(any(), any(), any(), 100L) } returns fakeRun(id = 101L)
        coEvery { githubRepository.getRun(any(), any(), 101L) } returns fakeRun(id = 101L)

        useCase.execute("owner", "repo", "main") {}

        coVerify(exactly = 1) { githubRepository.triggerBuild("owner", "repo", "main") }
    }

    @Test
    fun `never returns a run that existed before dispatch`() = runTest {
        // Regression for: pollForRun used to match by createdAt >= (now - 5s) and could
        // latch onto the previous build if it completed within the 5s clock-skew buffer,
        // causing the download to point at the old run's artifact.
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.getLatestRunId(any(), any(), any()) } returns 500L
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        // Mock returns only the new run (id 501), never the previous run (id 500) —
        // which demonstrates findRunAfterId's contract: filter by id > baseline.
        coEvery { githubRepository.findRunAfterId(any(), any(), any(), 500L) } returns fakeRun(id = 501L)
        coEvery { githubRepository.getRun(any(), any(), 501L) } returns fakeRun(id = 501L)

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        val success = steps.last() as BuildStep.Success
        assertEquals(501L, success.runId)
    }
}
