package com.apkpackager.data.workflow

import com.apkpackager.data.github.GitHubApiService
import com.apkpackager.data.github.model.ContentDto
import com.apkpackager.data.github.model.FileEntryDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class FrameworkDetectorTest {

    private val api = mockk<GitHubApiService>()
    private val detector = FrameworkDetector(api)

    private fun fileEntry(name: String) = FileEntryDto(name = name, type = "file", sha = "abc")

    @Test
    fun `detects Flutter when pubspec yaml present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("pubspec.yaml"), fileEntry("lib")))

        assertEquals(AppFramework.FLUTTER, detector.detect("owner", "repo", "main"))
    }

    @Test
    fun `detects Android when build gradle present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("build.gradle"), fileEntry("app")))

        assertEquals(AppFramework.ANDROID, detector.detect("owner", "repo", "main"))
    }

    @Test
    fun `detects Android when build gradle kts present`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("build.gradle.kts"), fileEntry("app")))

        assertEquals(AppFramework.ANDROID, detector.detect("owner", "repo", "main"))
    }

    @Test
    fun `detects React Native when package json has react-native dep`() = runTest {
        val packageJson = """{"dependencies":{"react-native":"0.73.0"}}"""
        val encoded = android.util.Base64.encodeToString(packageJson.toByteArray(), android.util.Base64.DEFAULT)

        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("package.json")))
        coEvery { api.getContents(any(), any(), "package.json", any()) } returns
            Response.success(ContentDto(sha = "abc", content = encoded))

        assertEquals(AppFramework.REACT_NATIVE, detector.detect("owner", "repo", "main"))
    }

    @Test
    fun `returns UNKNOWN when package json has no react-native`() = runTest {
        val packageJson = """{"dependencies":{"express":"4.0.0"}}"""
        val encoded = android.util.Base64.encodeToString(packageJson.toByteArray(), android.util.Base64.DEFAULT)

        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.success(listOf(fileEntry("package.json")))
        coEvery { api.getContents(any(), any(), "package.json", any()) } returns
            Response.success(ContentDto(sha = "abc", content = encoded))

        assertEquals(AppFramework.UNKNOWN, detector.detect("owner", "repo", "main"))
    }

    @Test
    fun `returns UNKNOWN when root contents call fails`() = runTest {
        coEvery { api.getRootContents(any(), any(), any()) } returns
            Response.error(403, okhttp3.ResponseBody.create(null, ""))

        assertEquals(AppFramework.UNKNOWN, detector.detect("owner", "repo", "main"))
    }
}
