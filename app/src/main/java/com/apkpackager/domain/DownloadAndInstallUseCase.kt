package com.apkpackager.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.apkpackager.data.github.GitHubApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int) : DownloadState()
    data class Ready(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class DownloadAndInstallUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GitHubApiService
) {
    suspend fun download(
        owner: String,
        repo: String,
        artifactId: Long,
        onProgress: (DownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(context.cacheDir, "apk_downloads").also { it.mkdirs() }
            val zipFile = File(downloadDir, "artifact.zip")
            val apkFile = File(downloadDir, "app.apk")

            onProgress(DownloadState.Downloading(0))

            val response = api.downloadArtifact(owner, repo, artifactId)
            if (!response.isSuccessful) {
                onProgress(DownloadState.Error("Download failed: HTTP ${response.code()}"))
                return@withContext null
            }
            val body = response.body() ?: run {
                onProgress(DownloadState.Error("Empty response"))
                return@withContext null
            }

            val totalBytes = body.contentLength().takeIf { it > 0 }
            var bytesRead = 0L

            FileOutputStream(zipFile).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        out.write(buffer, 0, n)
                        bytesRead += n
                        if (totalBytes != null) {
                            val pct = ((bytesRead * 100) / totalBytes).toInt()
                            onProgress(DownloadState.Downloading(pct))
                        }
                    }
                }
            }

            // Extract APK from zip
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                        FileOutputStream(apkFile).use { out -> zip.copyTo(out) }
                        break
                    }
                    entry = zip.nextEntry
                }
            }
            zipFile.delete()

            if (!apkFile.exists()) {
                onProgress(DownloadState.Error("No APK found in artifact archive"))
                return@withContext null
            }

            onProgress(DownloadState.Ready(apkFile))
            apkFile
        } catch (e: Exception) {
            onProgress(DownloadState.Error(e.message ?: "Unknown error"))
            null
        }
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun install(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
