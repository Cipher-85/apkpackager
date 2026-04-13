package com.apkpackager.ui.repos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apkpackager.data.github.model.RepoDto
import com.apkpackager.ui.components.ErrorState
import com.apkpackager.ui.components.ShimmerLoadingList
import com.apkpackager.ui.components.YoinkinsCard

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search repos...") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            )

            when (val s = state) {
                is RepoListState.Loading -> ShimmerLoadingList(itemCount = 6)
                is RepoListState.Error -> ErrorState(
                    message = s.message,
                    onRetry = { viewModel.loadRepos() },
                )
                is RepoListState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.repos, key = { it.id }) { repo ->
                            RepoItem(
                                repo = repo,
                                onClick = {
                                    onRepoSelected(repo.owner.login, repo.name, repo.defaultBranch)
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoItem(repo: RepoDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    YoinkinsCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(repo.name) },
            supportingContent = { Text(repo.owner.login, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                if (repo.isPrivate) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}
