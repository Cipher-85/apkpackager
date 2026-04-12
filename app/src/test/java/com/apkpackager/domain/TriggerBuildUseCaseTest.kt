package com.apkpackager.domain

import com.apkpackager.data.github.GitHubRepository
import com.apkpackager.data.github.model.WorkflowRunDto
import com.apkpackager.data.workflow.AppFramework
import com.apkpackager.data.workflow.DetectionResult
import com.apkpackager.data.workflow.FrameworkDetector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerBuildUseCaseTest {

    private val frameworkDetector = mockk<FrameworkDetector>()
    private val githubRepository = mockk<GitHubRepository>()
    private val useCase = TriggerBuildUseCase(frameworkDetector, githubRepository)

    private fun fakeRun(status: String, conclusion: String? = null) = WorkflowRunDto(
        id = 42L,
        status = status,
        conclusion = conclusion,
        createdAt = "2099-01-01T00:00:00Z",
        htmlUrl = "https://github.com/owner/repo/actions/runs/42"
    )

    @Test
    fun `emits Error when detection fails`() = runTest {
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns
            DetectionResult.Failure("No supported framework markers found. Saw: README.md")

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
        val error = steps.last() as BuildStep.Error
        assertTrue(error.message.contains("README.md"))
    }

    @Test
    fun `emits Error when workflow setup fails`() = runTest {
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns DetectionResult.Detected(AppFramework.ANDROID)
        coEvery { githubRepository.ensureWorkflow(any(), any(), any(), any()) } returns
            Result.failure(Exception("403 Forbidden"))

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
        assertTrue((steps.last() as BuildStep.Error).message.contains("403"))
    }

    @Test
    fun `emits Error when trigger fails`() = runTest {
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns DetectionResult.Detected(AppFramework.FLUTTER)
        coEvery { githubRepository.ensureWorkflow(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns
            Result.failure(Exception("422 Unprocessable"))

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.last() is BuildStep.Error)
    }

    @Test
    fun `emits Success for a completed successful run`() = runTest {
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns DetectionResult.Detected(AppFramework.ANDROID)
        coEvery { githubRepository.ensureWorkflow(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfter(any(), any(), any(), any()) } returns fakeRun("completed", "success")
        coEvery { githubRepository.getRun(any(), any(), any()) } returns fakeRun("completed", "success")

        val steps = mutableListOf<BuildStep>()
        useCase.execute("owner", "repo", "main") { steps.add(it) }

        assertTrue(steps.any { it is BuildStep.FrameworkDetected && it.framework == AppFramework.ANDROID })
        assertTrue(steps.last() is BuildStep.Success)
        assertEquals(42L, (steps.last() as BuildStep.Success).runId)
    }

    @Test
    fun `emits Failure when run concludes with failure`() = runTest {
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns DetectionResult.Detected(AppFramework.ANDROID)
        coEvery { githubRepository.ensureWorkflow(any(), any(), any(), any()) } returns Result.success(Unit)
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
        coEvery { frameworkDetector.detect(any(), any(), any()) } returns DetectionResult.Detected(AppFramework.ANDROID)
        coEvery { githubRepository.ensureWorkflow(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.triggerBuild(any(), any(), any()) } returns Result.success(Unit)
        coEvery { githubRepository.findRunAfter(any(), any(), any(), any()) } returns fakeRun("completed", "success")
        coEvery { githubRepository.getRun(any(), any(), any()) } returns fakeRun("completed", "success")

        useCase.execute("owner", "repo", "main") {}

        coVerify(exactly = 1) { githubRepository.triggerBuild("owner", "repo", "main") }
    }
}
