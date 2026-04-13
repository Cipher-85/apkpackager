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

    private fun fakeRun(status: String, conclusion: String? = null) = WorkflowRunDto(
        id = 42L,
        status = status,
        conclusion = conclusion,
        createdAt = "2099-01-01T00:00:00Z",
        htmlUrl = "https://github.com/owner/repo/actions/runs/42"
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
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns
            Result.failure(Exception("422 Unprocessable"))

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
    }

    @Test
    fun `emits Success for a completed successful run`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfter(any(), any(), any(), any()) } returns fakeRun("completed", "success")
        coEvery { githubRepository.getRun(any(), any(), any()) } returns fakeRun("completed", "success")

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.any { it is BuildStep.WorkflowReady })
        assertTrue(steps.last() is BuildStep.Success)
        assertEquals(42L, (steps.last() as BuildStep.Success).runId)
    }

    @Test
    fun `emits Failure when run concludes with failure`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfter(any(), any(), any(), any()) } returns fakeRun("completed", "failure")
        coEvery { githubRepository.getRun(any(), any(), any()) } returns fakeRun("completed", "failure")

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Failure)
        assertEquals("failure", (steps.last() as BuildStep.Failure).conclusion)
    }

    @Test
    fun `triggers workflow dispatch exactly once`() = runTest {
        coEvery { githubRepository.verifyWorkflow(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfter(any(), any(), any(), any()) } returns fakeRun("completed", "success")
        coEvery { githubRepository.getRun(any(), any(), any()) } returns fakeRun("completed", "success")

        useCase.execute("owner", "repo", "main") {}

        coVerify(exactly = 1) { githubRepository.triggerBuild("owner", "repo", "main") }
    }
}
