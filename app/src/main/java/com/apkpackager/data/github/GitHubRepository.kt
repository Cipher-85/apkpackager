package com.apkpackager.data.github

import com.apkpackager.data.github.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val api: GitHubApiService
) {
    suspend fun listRepos(page: Int = 1): Result<List<RepoDto>> {
        return try {
            val response = api.listRepos(page = page)
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else if (response.code() == 401) Result.failure(Exception("Session expired — please log in again"))
            else Result.failure(Exception("Failed to load repos: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBranches(owner: String, repo: String): Result<List<BranchDto>> {
        return try {
            val response = api.listBranches(owner, repo)
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else if (response.code() == 401) Result.failure(Exception("Session expired — please log in again"))
            else Result.failure(Exception("Failed to load branches: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyWorkflow(owner: String, repo: String, branch: String): Result<Unit> {
        return try {
            val response = api.getContents(owner, repo, ".github/workflows/build-apk.yml", branch)
            when {
                response.isSuccessful && response.body()?.sha != null -> Result.success(Unit)
                response.code() == 404 -> Result.failure(Exception(WORKFLOW_MISSING_MESSAGE))
                response.code() == 401 -> Result.failure(Exception("Session expired — please log in again"))
                else -> Result.failure(Exception("Could not verify workflow file: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not verify workflow file: ${e.message ?: e.javaClass.simpleName}", e))
        }
    }

    suspend fun triggerBuild(owner: String, repo: String, branch: String): Result<Unit> {
        return try {
            val response = api.triggerWorkflow(
                owner, repo, "build-apk.yml",
                WorkflowDispatchRequest(ref = branch)
            )
            if (response.isSuccessful || response.code() == 204) Result.success(Unit)
            else Result.failure(Exception("Trigger failed: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestRunId(owner: String, repo: String, branch: String): Long {
        val runs = api.listWorkflowRuns(owner, repo, branch = branch).workflowRuns
        return runs.maxOfOrNull { it.id } ?: 0L
    }

    suspend fun findRunAfterId(owner: String, repo: String, branch: String, afterRunId: Long): WorkflowRunDto? {
        val runs = api.listWorkflowRuns(owner, repo, branch = branch).workflowRuns
        return runs.filter { it.id > afterRunId }.minByOrNull { it.id }
    }

    suspend fun getRun(owner: String, repo: String, runId: Long): WorkflowRunDto =
        api.getWorkflowRun(owner, repo, runId)

    suspend fun getArtifacts(owner: String, repo: String, runId: Long): List<ArtifactDto> =
        api.listArtifacts(owner, repo, runId).artifacts

    suspend fun listBuildHistory(owner: String, repo: String, branch: String? = null, page: Int = 1): Result<List<WorkflowRunDto>> {
        return try {
            val response = api.listAllWorkflowRuns(owner, repo, branch = branch, page = page)
            Result.success(response.workflowRuns)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) Result.failure(Exception("Session expired — please log in again"))
            else Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobsForRun(owner: String, repo: String, runId: Long): Result<List<WorkflowJobDto>> {
        return try {
            val response = api.listWorkflowJobs(owner, repo, runId)
            Result.success(response.jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobLog(owner: String, repo: String, jobId: Long): Result<String> {
        return try {
            val response = api.downloadJobLogs(owner, repo, jobId)
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: ""
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch logs: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listCommits(owner: String, repo: String, branch: String, page: Int = 1): Result<List<CommitDto>> {
        return try {
            val response = api.listCommits(owner, repo, branch, page = page)
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else if (response.code() == 401) Result.failure(Exception("Session expired — please log in again"))
            else Result.failure(Exception("Failed to load commits: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getArtifactDownloadUrl(owner: String, repo: String, artifactId: Long): String =
        "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"

    companion object {
        const val WORKFLOW_MISSING_MESSAGE =
            "This repo has no .github/workflows/build-apk.yml. " +
                "Yoinkins does not create build configs — commit a workflow with a workflow_dispatch trigger " +
                "that uploads an artifact named \"app-debug\" containing the debug APK. " +
                "See docs/sample-workflows/ for reference workflows."
    }
}
