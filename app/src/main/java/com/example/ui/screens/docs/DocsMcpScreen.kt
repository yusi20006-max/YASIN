package com.example.ui.screens.docs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.RepoSummary
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrMonoText
import com.example.ui.components.StatusPill
import com.example.ui.components.YasinRtlContentView
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsMcpScreen(
    viewModel: DocsMcpViewModel,
    onAnalyzeWithAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "بازرس زنده مخازن و مستندات (Yasin MCP)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    LtrMonoText(text = "github.com/${state.owner}", fontSizeSp = 12)
                }
                IconButton(
                    onClick = { viewModel.refreshRepos() },
                    modifier = Modifier.testTag("docs_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.docs_refresh_cd)
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearch,
            placeholder = { Text("جستجوی مخزن یا توضیح...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "جستجو") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("docs_search_input")
        )

        when (val load = state.loadState) {
            DocsLoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "در حال دریافت مخازن زنده از گیت‌هاب...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is DocsLoadState.Error -> {
                ErrorBanner(
                    message = load.message,
                    onRetry = { viewModel.refreshRepos() }
                )
            }
            is DocsLoadState.Success -> {
                val query = state.searchQuery.trim()
                val filtered = load.repos.filter { repo ->
                    query.isEmpty() ||
                        repo.name.contains(query, ignoreCase = true) ||
                        repo.description.contains(query, ignoreCase = true) ||
                        repo.language.contains(query, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.docs_empty_title),
                        description = "مخزنی با این عبارت پیدا نشد یا اکانت گیت‌هاب مخزن عمومی ندارد."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { repo ->
                            RepoCard(
                                repo = repo,
                                onInspect = { viewModel.openRepoReadme(repo) },
                                onOpenBrowser = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl)))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val sheet = state.readmeSheet
    if (sheet.repo != null) {
        val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeReadmeSheet() },
            sheetState = modalState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LtrMonoText(
                    text = sheet.repo.fullName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSizeSp = 15
                )

                when {
                    sheet.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    sheet.error != null -> {
                        ErrorBanner(message = sheet.error)
                    }
                    else -> {
                        var formattedMode by remember { mutableStateOf(true) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = formattedMode,
                                onClick = { formattedMode = true },
                                label = { Text("نمای ساختاریافته") }
                            )
                            FilterChip(
                                selected = !formattedMode,
                                onClick = { formattedMode = false },
                                label = { Text("متن خام Markdown") }
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            if (formattedMode) {
                                Box(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(14.dp)
                                ) {
                                    YasinRtlContentView(
                                        content = sheet.content,
                                        showCopyAction = true,
                                        showMetadataBar = true,
                                        compact = true
                                    )
                                }
                            } else {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = sheet.content,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .verticalScroll(rememberScrollState())
                                            .padding(14.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val snippet = sheet.content.take(1800)
                                val repoName = sheet.repo.name
                                viewModel.closeReadmeSheet()
                                onAnalyzeWithAi(
                                    "این مستندات مخزن $repoName از اکوسیستم یاسین را به فارسی تحلیل کن و قابلیت‌های کلیدی آن را توضیح بده:\n\n$snippet"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("docs_analyze_with_ai_button")
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تحلیل این مخزن با یاسین هوش")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.closeReadmeSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("بستن")
                }
            }
        }
    }
}

@Composable
private fun RepoCard(
    repo: RepoSummary,
    onInspect: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Card(
        onClick = onInspect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repo_card_${repo.name}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrMonoText(
                    text = repo.name,
                    color = MaterialTheme.colorScheme.primary,
                    fontSizeSp = 15,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenBrowser) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = "باز کردن در مرورگر"
                    )
                }
            }

            if (repo.description.isNotBlank()) {
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        label = repo.language,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatusPill(
                        label = "★ ${repo.stars}",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LtrMonoText(text = repo.updatedAt, fontSizeSp = 11)
                }
            }
        }
    }
}
