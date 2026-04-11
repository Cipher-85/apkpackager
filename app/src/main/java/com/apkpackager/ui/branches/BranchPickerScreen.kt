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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is BranchState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is BranchState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadBranches(owner, repo) }) { Text("Retry") }
                }
            }
            is BranchState.Success -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(s.branches, key = { it.name }) { branch ->
                        ListItem(
                            modifier = Modifier.clickable { onBranchSelected(branch.name) },
                            headlineContent = { Text(branch.name) },
                            trailingContent = {
                                if (branch.name == defaultBranch) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Default branch",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
