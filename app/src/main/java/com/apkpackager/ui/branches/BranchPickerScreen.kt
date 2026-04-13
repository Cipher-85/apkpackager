package com.apkpackager.ui.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apkpackager.ui.components.ErrorState
import com.apkpackager.ui.components.ShimmerLoadingList
import com.apkpackager.ui.components.YoinkinsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchPickerScreen(
    viewModel: BranchPickerViewModel,
    owner: String,
    repo: String,
    defaultBranch: String,
    onBranchSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(owner, repo) { viewModel.loadBranches(owner, repo) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is BranchState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                ShimmerLoadingList(itemCount = 5)
            }
            is BranchState.Error -> ErrorState(
                message = s.message,
                onRetry = { viewModel.loadBranches(owner, repo) },
                modifier = Modifier.padding(padding),
            )
            is BranchState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(s.branches, key = { it.name }) { branch ->
                        YoinkinsCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBranchSelected(branch.name) }
                                .animateItem(),
                        ) {
                            ListItem(
                                headlineContent = { Text(branch.name) },
                                trailingContent = {
                                    if (branch.name == defaultBranch) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Default branch",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            )
                        }
                    }
                }
            }
        }
    }
}
