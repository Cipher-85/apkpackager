package com.apkpackager.ui.repos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkpackager.data.auth.AuthRepository
import com.apkpackager.data.github.GitHubRepository
import com.apkpackager.data.github.model.RepoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RepoListState {
    object Loading : RepoListState()
    data class Success(val repos: List<RepoDto>, val query: String = "") : RepoListState()
    data class Error(val message: String) : RepoListState()
}

@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val githubRepository: GitHubRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RepoListState>(RepoListState.Loading)
    val state: StateFlow<RepoListState> = _state

    private val allRepos = mutableListOf<RepoDto>()

    init { loadRepos() }

    fun loadRepos() {
        viewModelScope.launch {
            _state.value = RepoListState.Loading
            val repos = mutableListOf<RepoDto>()
            var page = 1
            while (true) {
                val result = githubRepository.listRepos(page)
                if (result.isFailure) {
                    _state.value = RepoListState.Error(result.exceptionOrNull()?.message ?: "Failed to load repos")
                    return@launch
                }
                val pageRepos = result.getOrDefault(emptyList())
                if (pageRepos.isEmpty()) break
                repos.addAll(pageRepos)
                if (pageRepos.size < 100) break
                page++
            }
            allRepos.clear()
            allRepos.addAll(repos.sortedBy { it.name.lowercase() })
            _state.value = RepoListState.Success(allRepos.toList())
        }
    }

    fun search(query: String) {
        val current = _state.value
        if (current is RepoListState.Success || current is RepoListState.Error) {
            val filtered = if (query.isBlank()) allRepos.toList()
            else allRepos.filter { it.name.contains(query, ignoreCase = true) || it.owner.login.contains(query, ignoreCase = true) }
            _state.value = RepoListState.Success(filtered, query)
        }
    }

    fun logout() = authRepository.logout()
}
