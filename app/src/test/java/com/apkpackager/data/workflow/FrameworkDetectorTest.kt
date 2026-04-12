package com.apkpackager.data.workflow

import com.apkpackager.data.github.GitHubApiService
import com.apkpackager.data.github.model.ContentDto
import com.apkpackager.data.github.model.FileEntryDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FrameworkDetectorTest {

    private val api = mockk<GitHubApiService>()
    private val detector = FrameworkDetector(api)

    private fun fileEntry(name: String) = FileEntryDto(name = name, type = "file", sha = "abc")

    @Test
    fun `detects Godot when project godot present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("project.godot"), fileEntry("icon.svg"), fileEntry("addons")))

        assertEquals(
            DetectionResult.Detected(AppFramework.GODOT),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `detects Flutter when pubspec yaml present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("pubspec.yaml"), fileEntry("lib")))

        assertEquals(
            DetectionResult.Detected(AppFramework.FLUTTER),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `detects Android when build gradle present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("build.gradle"), fileEntry("app")))

        assertEquals(
            DetectionResult.Detected(AppFramework.ANDROID),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `detects Android when build gradle kts present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("build.gradle.kts"), fileEntry("app")))

        assertEquals(
            DetectionResult.Detected(AppFramework.ANDROID),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `detects Android when only settings gradle kts present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("settings.gradle.kts"), fileEntry("app")))

        assertEquals(
            DetectionResult.Detected(AppFramework.ANDROID),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `detects React Native when package json has react-native dep`() = runTest {
        val packageJson = """{"dependencies":{"react-native":"0.73.0"}}"""
        val encoded = android.util.Base64.encodeToString(packageJson.toByteArray(), android.util.Base64.DEFAULT)

        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("package.json")))
        coEvery { api.getContents(any(), any(), "package.json", any()) } returns
            Response.success(ContentDto(sha = "abc", content = encoded))

        assertEquals(
            DetectionResult.Detected(AppFramework.REACT_NATIVE),
            detector.detect("owner", "repo", "main")
        )
    }

    @Test
    fun `returns Failure when package json has no react-native`() = runTest {
        val packageJson = """{"dependencies":{"express":"4.0.0"}}"""
        val encoded = android.util.Base64.encodeToString(packageJson.toByteArray(), android.util.Base64.DEFAULT)

        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("package.json")))
        coEvery { api.getContents(any(), any(), "package.json", any()) } returns
            Response.success(ContentDto(sha = "abc", content = encoded))

        val result = detector.detect("owner", "repo", "main")
        assertTrue(result is DetectionResult.Failure)
        assertTrue((result as DetectionResult.Failure).reason.contains("react-native"))
    }

    @Test
    fun `returns Failure with HTTP code when root contents call fails`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.error(403, okhttp3.ResponseBody.create(null, ""))

        val result = detector.detect("owner", "repo", "main")
        assertTrue(result is DetectionResult.Failure)
        assertTrue((result as DetectionResult.Failure).reason.contains("403"))
    }

    @Test
    fun `returns Failure listing root files when no markers matched`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("README.md"), fileEntry("src"), fileEntry("Cargo.toml")))

        val result = detector.detect("owner", "repo", "main")
        assertTrue(result is DetectionResult.Failure)
        val reason = (result as DetectionResult.Failure).reason
        assertTrue(reason.contains("Cargo.toml"))
        assertTrue(reason.contains("No supported framework markers"))
    }
}
