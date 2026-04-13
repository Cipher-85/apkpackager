package com.apkpackager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.apkpackager.ui.auth.LoginScreen
import com.apkpackager.ui.auth.LoginViewModel
import com.apkpackager.ui.branches.BranchPickerScreen
import com.apkpackager.ui.branches.BranchPickerViewModel
import com.apkpackager.ui.build.BuildDashboardScreen
import com.apkpackager.ui.build.BuildDashboardViewModel
import com.apkpackager.ui.commits.CommitHistoryScreen
import com.apkpackager.ui.commits.CommitHistoryViewModel
import com.apkpackager.ui.history.BuildHistoryScreen
import com.apkpackager.ui.history.BuildHistoryViewModel
import com.apkpackager.ui.history.BuildLogScreen
import com.apkpackager.ui.history.BuildLogViewModel
import com.apkpackager.ui.repos.RepoListScreen
import com.apkpackager.ui.repos.RepoListViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavGraph(isLoggedIn: Boolean = false) {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) "repos" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(300)) { it } +
                fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } +
                fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(300)) { -it / 3 } +
                fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(300)) { it } +
                fadeOut(animationSpec = tween(300))
        },
    ) {
        composable(
            "login",
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            val vm: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = vm,
                onLoggedIn = { navController.navigate("repos") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable(
            "repos",
            enterTransition = {
                fadeIn(animationSpec = tween(400)) +
                    scaleIn(animationSpec = tween(400), initialScale = 0.92f)
            },
        ) {
            val vm: RepoListViewModel = hiltViewModel()
            RepoListScreen(
                viewModel = vm,
                onRepoSelected = { owner, repo, defaultBranch ->
                    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
                    val encodedRepo = URLEncoder.encode(repo, "UTF-8")
                    val encodedBranch = URLEncoder.encode(defaultBranch, "UTF-8")
                    navController.navigate("branches/$encodedOwner/$encodedRepo/$encodedBranch")
                },
                onLogout = { navController.navigate("login") { popUpTo(0) { inclusive = true } } }
            )
        }
        composable(
            "branches/{owner}/{repo}/{defaultBranch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("defaultBranch") { type = NavType.StringType }
            )
        ) { backStack ->
            val owner = URLDecoder.decode(backStack.arguments?.getString("owner") ?: "", "UTF-8")
            val repo = URLDecoder.decode(backStack.arguments?.getString("repo") ?: "", "UTF-8")
            val defaultBranch = URLDecoder.decode(backStack.arguments?.getString("defaultBranch") ?: "", "UTF-8")
            val vm: BranchPickerViewModel = hiltViewModel()
            BranchPickerScreen(
                viewModel = vm,
                owner = owner,
                repo = repo,
                defaultBranch = defaultBranch,
                onBranchSelected = { branch ->
                    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
                    val encodedRepo = URLEncoder.encode(repo, "UTF-8")
                    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
                    navController.navigate("commits/$encodedOwner/$encodedRepo/$encodedBranch")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "commits/{owner}/{repo}/{branch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType }
            )
        ) { backStack ->
            val owner = URLDecoder.decode(backStack.arguments?.getString("owner") ?: "", "UTF-8")
            val repo = URLDecoder.decode(backStack.arguments?.getString("repo") ?: "", "UTF-8")
            val branch = URLDecoder.decode(backStack.arguments?.getString("branch") ?: "", "UTF-8")
            val vm: CommitHistoryViewModel = hiltViewModel()
            CommitHistoryScreen(
                viewModel = vm,
                owner = owner,
                repo = repo,
                branch = branch,
                onCommitSelected = { sha, _ ->
                    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
                    val encodedRepo = URLEncoder.encode(repo, "UTF-8")
                    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
                    navController.navigate("build/$encodedOwner/$encodedRepo/$encodedBranch")
                },
                onBack = { navController.popBackStack() },
                onHistory = {
                    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
                    val encodedRepo = URLEncoder.encode(repo, "UTF-8")
                    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
                    navController.navigate("history/$encodedOwner/$encodedRepo/$encodedBranch")
                }
            )
        }
        composable(
            "build/{owner}/{repo}/{branch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType }
            )
        ) { backStack ->
            val owner = URLDecoder.decode(backStack.arguments?.getString("owner") ?: "", "UTF-8")
            val repo = URLDecoder.decode(backStack.arguments?.getString("repo") ?: "", "UTF-8")
            val branch = URLDecoder.decode(backStack.arguments?.getString("branch") ?: "", "UTF-8")
            val vm: BuildDashboardViewModel = hiltViewModel()
            BuildDashboardScreen(
                viewModel = vm,
                owner = owner,
                repo = repo,
                branch = branch,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "history/{owner}/{repo}/{branch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType }
            )
        ) { backStack ->
            val owner = URLDecoder.decode(backStack.arguments?.getString("owner") ?: "", "UTF-8")
            val repo = URLDecoder.decode(backStack.arguments?.getString("repo") ?: "", "UTF-8")
            val branch = URLDecoder.decode(backStack.arguments?.getString("branch") ?: "", "UTF-8")
            val vm: BuildHistoryViewModel = hiltViewModel()
            BuildHistoryScreen(
                viewModel = vm,
                owner = owner,
                repo = repo,
                branch = branch,
                onBack = { navController.popBackStack() },
                onRunSelected = { runId ->
                    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
                    val encodedRepo = URLEncoder.encode(repo, "UTF-8")
                    navController.navigate("history/$encodedOwner/$encodedRepo/log/$runId")
                }
            )
        }
        composable(
            "history/{owner}/{repo}/log/{runId}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStack ->
            val owner = URLDecoder.decode(backStack.arguments?.getString("owner") ?: "", "UTF-8")
            val repo = URLDecoder.decode(backStack.arguments?.getString("repo") ?: "", "UTF-8")
            val runId = backStack.arguments?.getLong("runId") ?: 0L
            val vm: BuildLogViewModel = hiltViewModel()
            BuildLogScreen(
                viewModel = vm,
                owner = owner,
                repo = repo,
                runId = runId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
