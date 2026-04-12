package com.apkpackager.data.workflow

import android.util.Base64
import com.apkpackager.data.github.GitHubApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

sealed class DetectionResult {
    data class Detected(val framework: AppFramework) : DetectionResult()
    data class Failure(val reason: String) : DetectionResult()
}

class FrameworkDetector @Inject constructor(
    private val api: GitHubApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detect(owner: String, repo: String, branch: String): DetectionResult {
        val response = try {
            api.getRootContents(owner, repo, branch)
        } catch (e: Exception) {
            return DetectionResult.Failure("Could not list repository contents: ${e.message ?: e.javaClass.simpleName}")
        }
        if (!response.isSuccessful) {
            return DetectionResult.Failure(
                "GitHub returned HTTP ${response.code()} when listing repository contents. " +
                    "Check that the app has access to this repo and branch."
            )
        }
        val files = response.body()?.map { it.name }.orEmpty()
        if (files.isEmpty()) {
            return DetectionResult.Failure("The branch '$branch' appears to be empty.")
        }

        val hasGodot = "project.godot" in files
        val hasPubspec = "pubspec.yaml" in files
        val hasPackageJson = "package.json" in files
        val hasGradleBuild = files.any { it == "build.gradle" || it == "build.gradle.kts" }
        val hasGradleSettings = files.any { it == "settings.gradle" || it == "settings.gradle.kts" }

        return when {
            hasGodot -> DetectionResult.Detected(AppFramework.GODOT)
            hasPubspec -> DetectionResult.Detected(AppFramework.FLUTTER)
            hasPackageJson -> detectNodeFramework(owner, repo, branch, files)
            hasGradleBuild || hasGradleSettings -> DetectionResult.Detected(AppFramework.ANDROID)
            else -> DetectionResult.Failure(unsupportedMessage(files))
        }
    }

    private suspend fun detectNodeFramework(
        owner: String,
        repo: String,
        branch: String,
        rootFiles: List<String>
    ): DetectionResult {
        val response = try {
            api.getContents(owner, repo, "package.json", branch)
        } catch (e: Exception) {
            return DetectionResult.Failure("Could not read package.json: ${e.message ?: e.javaClass.simpleName}")
        }
        if (!response.isSuccessful) {
            return DetectionResult.Failure("GitHub returned HTTP ${response.code()} when reading package.json.")
        }
        val encoded = response.body()?.content
            ?: return DetectionResult.Failure("package.json had no content.")
        return try {
            val decoded = String(Base64.decode(encoded.replace("\n", ""), Base64.DEFAULT))
            val obj = json.parseToJsonElement(decoded).jsonObject
            val deps = obj["dependencies"]?.jsonObject?.keys ?: emptySet()
            val devDeps = obj["devDependencies"]?.jsonObject?.keys ?: emptySet()
            val allDeps = deps + devDeps
            if ("react-native" in allDeps) {
                DetectionResult.Detected(AppFramework.REACT_NATIVE)
            } else {
                DetectionResult.Failure(
                    "Found package.json but no 'react-native' dependency. " +
                        "Supported frameworks: React Native, Flutter, Android."
                )
            }
        } catch (e: Exception) {
            DetectionResult.Failure("Could not parse package.json: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun unsupportedMessage(files: List<String>): String {
        val preview = files.take(10).joinToString(", ")
        val suffix = if (files.size > 10) ", …" else ""
        return "No supported framework markers found at the repo root. " +
            "Expected one of: project.godot (Godot), pubspec.yaml (Flutter), " +
            "package.json with react-native (React Native), " +
            "or build.gradle / settings.gradle (Android). Saw: $preview$suffix"
    }
}
