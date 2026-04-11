package com.apkpackager.data.workflow

import android.util.Base64
import com.apkpackager.data.github.GitHubApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class FrameworkDetector @Inject constructor(
    private val api: GitHubApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detect(owner: String, repo: String, branch: String): AppFramework {
        val response = api.getRootContents(owner, repo, branch)
        if (!response.isSuccessful) return AppFramework.UNKNOWN
        val files = response.body()?.map { it.name } ?: return AppFramework.UNKNOWN

        return when {
            files.any { it == "pubspec.yaml" } -> AppFramework.FLUTTER
            files.any { it == "package.json" } -> detectNodeFramework(owner, repo, branch)
            files.any { it == "build.gradle" || it == "build.gradle.kts" } -> AppFramework.ANDROID
            else -> AppFramework.UNKNOWN
        }
    }

    private suspend fun detectNodeFramework(owner: String, repo: String, branch: String): AppFramework {
        val response = api.getContents(owner, repo, "package.json", branch)
        if (!response.isSuccessful) return AppFramework.UNKNOWN
        val encoded = response.body()?.content ?: return AppFramework.UNKNOWN
        return try {
            val decoded = String(Base64.decode(encoded.replace("\n", ""), Base64.DEFAULT))
            val obj = json.parseToJsonElement(decoded).jsonObject
            val deps = obj["dependencies"]?.jsonObject?.keys ?: emptySet()
            val devDeps = obj["devDependencies"]?.jsonObject?.keys ?: emptySet()
            if ("react-native" in deps || "react-native" in devDeps) AppFramework.REACT_NATIVE
            else AppFramework.UNKNOWN
        } catch (e: Exception) {
            AppFramework.UNKNOWN
        }
    }
}
