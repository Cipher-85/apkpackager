package com.apkpackager.ui.repos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apkpackager.data.github.model.RepoDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    viewModel: RepoListViewModel,
    onRepoSelected: (owner: String, repo: String, defaultBranch: String) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repositories") },
                actions = {
                    IconButton(onClick = { viewModel.loadRepos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; viewModel.search(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search repos...") },
                singleLine = true
            )

            when (val s = state) {
                is RepoListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is RepoListState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadRepos() }) { Text("Retry") }
                    }
                }
                is RepoListState.Success -> {
                    LazyColumn {
                        items(s.repos, key = { it.id }) { repo ->
                            RepoItem(repo = repo, onClick = {
                                onRepoSelected(repo.owner.login, repo.name, repo.defaultBranch)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoItem(repo: RepoDto, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(repo.name) },
        supportingContent = { Text(repo.owner.login, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            if (repo.isPrivate) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Private",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
    HorizontalDivider()
}
