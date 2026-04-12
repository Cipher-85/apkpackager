package com.apkpackager.domain

import com.apkpackager.data.github.GitHubRepository
import com.apkpackager.data.workflow.AppFramework
import com.apkpackager.data.workflow.DetectionResult
import com.apkpackager.data.workflow.FrameworkDetector
import javax.inject.Inject

sealed class BuildStep {
    object DetectingFramework : BuildStep()
    data class FrameworkDetected(val framework: AppFramework) : BuildStep()
    object CheckingWorkflow : BuildStep()
    object WorkflowReady : BuildStep()
    object Triggering : BuildStep()
    data class Queued(val runId: Long) : BuildStep()
    data class InProgress(val runId: Long, val htmlUrl: String) : BuildStep()
    data class Success(val runId: Long, val htmlUrl: String) : BuildStep()
    data class Failure(val runId: Long, val htmlUrl: String, val conclusion: String?) : BuildStep()
    data class Error(val message: String) : BuildStep()
}

class TriggerBuildUseCase @Inject constructor(
    private val frameworkDetector: FrameworkDetector,
    private val githubRepository: GitHubRepository
) {
    suspend fun execute(
        owner: String,
        repo: String,
        branch: String,
        onStep: suspend (BuildStep) -> Unit
    ) {
        onStep(BuildStep.DetectingFramework)
        val framework = when (val result = frameworkDetector.detect(owner, repo, branch)) {
            is DetectionResult.Detected -> result.framework
            is DetectionResult.Failure -> {
                onStep(BuildStep.Error(result.reason))
                return
            }
        }
        onStep(BuildStep.FrameworkDetected(framework))

        onStep(BuildStep.CheckingWorkflow)
        val workflowResult = githubRepository.ensureWorkflow(owner, repo, branch, framework)
        if (workflowResult.isFailure) {
            onStep(BuildStep.Error("Failed to set up workflow: ${workflowResult.exceptionOrNull()?.message}"))
            return
        }
        onStep(BuildStep.WorkflowReady)

        onStep(BuildStep.Triggering)
        val dispatchTime = System.currentTimeMillis() - 5_000 // small buffer for clock skew
        val triggerResult = githubRepository.triggerBuild(owner, repo, branch)
        if (triggerResult.isFailure) {
            onStep(BuildStep.Error("Failed to trigger build: ${triggerResult.exceptionOrNull()?.message}"))
            return
        }

        // Poll for the new run
        val initialRun = pollForRun(owner, repo, branch, dispatchTime)
        if (initialRun == null) {
            onStep(BuildStep.Error("Build was triggered but could not find the workflow run. Check GitHub Actions."))
            return
        }
        onStep(BuildStep.Queued(initialRun.id))

        // Poll until done
        var run: com.apkpackager.data.github.model.WorkflowRunDto = initialRun
        while (run.status != "completed") {
            kotlinx.coroutines.delay(5_000)
            run = githubRepository.getRun(owner, repo, run.id)
            when (run.status) {
                "in_progress" -> onStep(BuildStep.InProgress(run.id, run.htmlUrl))
                "queued", "waiting", "pending" -> onStep(BuildStep.Queued(run.id))
            }
        }

        if (run.conclusion == "success") {
            onStep(BuildStep.Success(run.id, run.htmlUrl))
        } else {
            onStep(BuildStep.Failure(run.id, run.htmlUrl, run.conclusion))
        }
    }

    private suspend fun pollForRun(
        owner: String, repo: String, branch: String, afterMs: Long
    ): com.apkpackager.data.github.model.WorkflowRunDto? {
        repeat(12) { // up to 60 seconds
            val found = githubRepository.findRunAfter(owner, repo, branch, afterMs)
            if (found != null) return found
            kotlinx.coroutines.delay(5_000)
        }
        return null
    }
}
