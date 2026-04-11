package com.apkpackager.ui.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkpackager.data.github.GitHubRepository
import com.apkpackager.data.github.model.BranchDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BranchState {
    object Loading : BranchState()
    data class Success(val branches: List<BranchDto>) : BranchState()
    data class Error(val message: String) : BranchState()
}

@HiltViewModel
class BranchPickerViewModel @Inject constructor(
    private val githubRepository: GitHubRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BranchState>(BranchState.Loading)
    val state: StateFlow<BranchState> = _state

    fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch {
            _state.value = BranchState.Loading
            val result = githubRepository.listBranches(owner, repo)
            _state.value = if (result.isSuccess) {
                BranchState.Success(result.getOrDefault(emptyList()))
            } else {
                BranchState.Error(result.exceptionOrNull()?.message ?: "Failed to load branches")
            }
        }
    }
}
