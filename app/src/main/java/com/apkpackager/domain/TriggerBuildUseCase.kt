package com.apkpackager.domain

import com.apkpackager.data.github.GitHubRepository
import javax.inject.Inject

sealed class BuildStep {
    object VerifyingWorkflow : BuildStep()
    object WorkflowReady : BuildStep()
    object Triggering : BuildStep()
    data class Queued(val runId: Long) : BuildStep()
    data class InProgress(val runId: Long, val htmlUrl: String) : BuildStep()
    data class Success(val runId: Long, val htmlUrl: String) : BuildStep()
    data class Failure(val runId: Long, val htmlUrl: String, val conclusion: String?) : BuildStep()
    data class Error(val message: String) : BuildStep()
}

class TriggerBuildUseCase @Inject constructor(
    private val githubRepository: GitHubRepository
) {
    suspend fun execute(
        owner: String,
        repo: String,
        branch: String,
        onStep: suspend (BuildStep) -> Unit
    ) {
        onStep(BuildStep.VerifyingWorkflow)
        val workflowResult = githubRepository.verifyWorkflow(owner, repo, branch)
        if (workflowResult.isFailure) {
            onStep(BuildStep.Error(workflowResult.exceptionOrNull()?.message ?: "Workflow verification failed"))
            return
        }
        onStep(BuildStep.WorkflowReady)

        onStep(BuildStep.Triggering)
        val baselineRunId = try {
            githubRepository.getLatestRunId(owner, repo, branch)
        } catch (e: Exception) {
            onStep(BuildStep.Error("Could not read current build state: ${e.message ?: e.javaClass.simpleName}"))
            return
        }
        val triggerResult = githubRepository.triggerBuild(owner, repo, branch)
        if (triggerResult.isFailure) {
            onStep(BuildStep.Error("Failed to trigger build: ${triggerResult.exceptionOrNull()?.message}"))
            return
        }

        // Poll for the run we just dispatched (any run with id > baseline is newer than anything pre-dispatch)
        val initialRun = pollForRun(owner, repo, branch, baselineRunId)
        if (initialRun == null) {
            onStep(BuildStep.Error("Build was triggered but could not find the workflow run. Check GitHub Actions."))
            return
        }
        onStep(BuildStep.Queued(initialRun.id))

        // Poll until done
        var run: com.apkpackager.data.github.model.WorkflowRunDto = initialRun
        var consecutiveErrors = 0
        while (run.status != "completed") {
            kotlinx.coroutines.delay(5_000)
            try {
                run = githubRepository.getRun(owner, repo, run.id)
                consecutiveErrors = 0
                when (run.status) {
                    "in_progress" -> onStep(BuildStep.InProgress(run.id, run.htmlUrl))
                    "queued", "waiting", "pending" -> onStep(BuildStep.Queued(run.id))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                consecutiveErrors++
                if (consecutiveErrors >= 5) {
                    onStep(BuildStep.Error("Lost connection while monitoring build: ${e.message}"))
                    return
                }
                // Back off on repeated failures
                kotlinx.coroutines.delay(consecutiveErrors * 5_000L)
            }
        }

        if (run.conclusion == "success") {
            onStep(BuildStep.Success(run.id, run.htmlUrl))
        } else {
            onStep(BuildStep.Failure(run.id, run.htmlUrl, run.conclusion))
        }
    }

    private suspend fun pollForRun(
        owner: String, repo: String, branch: String, afterRunId: Long
    ): com.apkpackager.data.github.model.WorkflowRunDto? {
        repeat(12) { // up to 60 seconds
            try {
                val found = githubRepository.findRunAfterId(owner, repo, branch, afterRunId)
                if (found != null) return found
            } catch (_: kotlinx.coroutines.CancellationException) {
                throw kotlinx.coroutines.CancellationException()
            } catch (_: Exception) {
                // Network error — keep retrying
            }
            kotlinx.coroutines.delay(5_000)
        }
        return null
    }
}
