package com.example.ui.screens.press

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.OperationLogEntity
import com.example.data.local.PressArticleEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.YasinRtlContentCard
import com.example.ui.components.YasinRtlContentView
import com.example.ui.theme.LocalYasinColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressScreen(
    viewModel: PressViewModel,
    onOpenEitaayarSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val yasinColors = LocalYasinColors.current
    var previewArticle by remember { mutableStateOf<PressArticleEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: Eitaayar connection status + quick shortcut
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isConfigured = state.settings.eitaayarToken.isNotBlank() &&
                        state.settings.eitaaChannelId.isNotBlank()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = if (isConfigured) yasinColors.success else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isConfigured) {
                                "ایتا متصل: ${state.settings.eitaaChannelId}"
                            } else {
                                "کد ایتایار هنوز تنظیم نشده است"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onOpenEitaayarSettings,
                        modifier = Modifier.testTag("press_open_eitaayar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("تنظیم ایتایار")
                    }
                }
            }

            // 3-way section switcher: Articles | Full Text Archive | Operation Logs
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PressSectionTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PressSectionTab.entries.size
                        ),
                        modifier = Modifier.testTag("press_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.labelFa, maxLines = 1)
                    }
                }
            }

            if (state.statusBanner != null) {
                Surface(
                    color = yasinColors.successContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircleOutline,
                            contentDescription = "موفق",
                            tint = yasinColors.success
                        )
                        Text(
                            text = state.statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = yasinColors.success,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::dismissBanners) {
                            Text("بستن")
                        }
                    }
                }
            }

            if (state.errorBanner != null) {
                ErrorBanner(
                    message = state.errorBanner.orEmpty(),
                    onRetry = onOpenEitaayarSettings
                )
            }

            when (state.selectedTab) {
                PressSectionTab.ARTICLES -> {
                    ArticlesTabContent(
                        state = state,
                        onSearchChange = viewModel::updateSearch,
                        onSelectFilter = viewModel::selectFilter,
                        onNewArticle = viewModel::openNewEditor,
                        onPreviewArticle = { previewArticle = it },
                        onEditArticle = viewModel::openEditEditor,
                        onTogglePin = viewModel::togglePin,
                        onDeleteArticle = { viewModel.deleteArticle(it.id) },
                        onPublishEitaa = viewModel::publishToEitaa,
                        onShareArticle = { article ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, article.title)
                                putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.body}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری نوشته"))
                        }
                    )
                }

                PressSectionTab.TEXT_ARCHIVE -> {
                    TextArchiveTabContent(
                        state = state,
                        onSelectSource = viewModel::selectArchiveSource,
                        onRefreshEitaa = viewModel::refreshEitaaChannelPosts,
                        onOpenSettings = onOpenEitaayarSettings
                    )
                }

                PressSectionTab.LOGS -> {
                    LogsTabContent(
                        state = state,
                        onSelectLogFilter = viewModel::selectLogFilter,
                        onClearLogs = viewModel::clearAllLogs
                    )
                }
            }
        }

        if (state.selectedTab == PressSectionTab.ARTICLES) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openNewEditor() },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.press_new_article)) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .testTag("press_fab_new_article")
            )
        }
    }

    if (state.editor.isOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEditor() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PressEditorSheet(
                editor = state.editor,
                onTitleChange = viewModel::updateEditorTitle,
                onBodyChange = viewModel::updateEditorBody,
                onCategoryChange = viewModel::updateEditorCategory,
                onTagsChange = viewModel::updateEditorTags,
                onPublishedChange = viewModel::updateEditorPublished,
                onAiAssist = viewModel::assistBodyWithAi,
                onSave = viewModel::saveEditorArticle,
                onCancel = viewModel::closeEditor
            )
        }
    }

    val activePreview = previewArticle
    if (activePreview != null) {
        val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { previewArticle = null },
            sheetState = previewSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                YasinRtlContentView(
                    content = activePreview.body.ifBlank { "متن این نوشته هنوز خالی است." },
                    title = activePreview.title,
                    categoryBadge = activePreview.category,
                    showCopyAction = true,
                    showMetadataBar = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            previewArticle = null
                            viewModel.publishToEitaa(activePreview)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("press_preview_publish_eitaa_button")
                    ) {
                        Text("انتشار در ایتا")
                    }
                    OutlinedButton(
                        onClick = {
                            previewArticle = null
                            viewModel.openEditEditor(activePreview)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("press_preview_edit_button")
                    ) {
                        Text("ویرایش")
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticlesTabContent(
    state: PressUiState,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (PressFilter) -> Unit,
    onNewArticle: () -> Unit,
    onPreviewArticle: (PressArticleEntity) -> Unit,
    onEditArticle: (PressArticleEntity) -> Unit,
    onTogglePin: (PressArticleEntity) -> Unit,
    onDeleteArticle: (PressArticleEntity) -> Unit,
    onPublishEitaa: (PressArticleEntity) -> Unit,
    onShareArticle: (PressArticleEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("جستجو در عنوان، متن یا برچسب...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "جستجو")
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("press_search_input")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PressFilter.entries) { filter ->
                FilterChip(
                    selected = state.selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter.labelFa) },
                    modifier = Modifier.testTag("press_filter_${filter.name.lowercase()}")
                )
            }
        }

        if (state.articles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Newspaper,
                title = stringResource(R.string.press_empty_title),
                description = stringResource(R.string.press_empty_desc),
                action = {
                    Button(
                        onClick = onNewArticle,
                        modifier = Modifier.testTag("press_empty_create_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.press_new_article))
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.articles, key = { it.id }) { article ->
                    PressArticleCard(
                        article = article,
                        isPublishing = state.publishingArticleId == article.id,
                        onPreview = { onPreviewArticle(article) },
                        onEdit = { onEditArticle(article) },
                        onTogglePin = { onTogglePin(article) },
                        onDelete = { onDeleteArticle(article) },
                        onPublishEitaa = { onPublishEitaa(article) },
                        onShare = { onShareArticle(article) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextArchiveTabContent(
    state: PressUiState,
    onSelectSource: (TextArchiveSource) -> Unit,
    onRefreshEitaa: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedAllArchive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TextArchiveSource.entries) { source ->
                FilterChip(
                    selected = state.archiveSource == source,
                    onClick = { onSelectSource(source) },
                    label = { Text(source.labelFa) },
                    modifier = Modifier.testTag("archive_source_${source.name.lowercase()}")
                )
            }
        }

        if (state.archiveSource == TextArchiveSource.LOCAL_PUBLICATIONS) {
            val publishedOrAll = remember(state.allArticles) {
                val published = state.allArticles.filter { it.isPublished }
                published.ifEmpty { state.allArticles }
            }

            if (publishedOrAll.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Subject,
                    title = "هنوز انتشاراتی ثبت نشده است",
                    description = "پس از ثبت یا انتشار نوشته‌ها در یاسین پرس، متن کامل همه آن‌ها به‌صورت یکجا اینجا نمایش داده می‌شود."
                )
            } else {
                val combinedPlainText = remember(publishedOrAll) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    publishedOrAll.joinToString("\n\n────────────────────────\n\n") { art ->
                        val date = sdf.format(Date(art.updatedAt))
                        val status = if (art.eitaaMessageId != null) {
                            "منتشرشده در ایتا (شناسه ${art.eitaaMessageId})"
                        } else if (art.isPublished) {
                            "منتشرشده"
                        } else {
                            "پیش‌نویس"
                        }
                        "# ${art.title}\n[$status • ${art.category} • $date]\n\n${art.body}"
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "کل انتشارات متنی (${publishedOrAll.size} مورد)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(combinedPlainText))
                            copiedAllArchive = true
                        },
                        modifier = Modifier.testTag("archive_copy_all_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (copiedAllArchive) "کپی شد" else "کپی کل متن‌ها")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    YasinRtlContentCard(
                        content = combinedPlainText,
                        showCopyAction = false,
                        showMetadataBar = true
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.settings.eitaaChannelId.isBlank()) {
                        "کانال ایتا تنظیم نشده"
                    } else {
                        "کانال: ${state.settings.eitaaChannelId}"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRefreshEitaa) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("به‌روزرسانی")
                    }
                }
            }

            when {
                state.isLoadingEitaaPosts -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.eitaaFetchError != null -> {
                    ErrorBanner(
                        message = state.eitaaFetchError,
                        onRetry = onOpenSettings
                    )
                }
                state.eitaaPosts.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Outlined.Campaign,
                        title = "پستی از کانال ایتا دریافت نشد",
                        description = "اگر کانال شما عمومی است، آیدی کانال را در تنظیمات ثبت کرده و دکمه به‌روزرسانی را بزنید.",
                        action = {
                            Button(onClick = onOpenSettings) {
                                Text("تنظیم آیدی کانال ایتا")
                            }
                        }
                    )
                }
                else -> {
                    val combinedEitaaText = remember(state.eitaaPosts) {
                        state.eitaaPosts.joinToString("\n\n────────────────────────\n\n") { post ->
                            val header = if (post.dateText.isNotBlank()) {
                                "### پست ${post.postId} (${post.dateText})"
                            } else {
                                "### پست ${post.postId}"
                            }
                            "$header\n\n${post.text}"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        YasinRtlContentCard(
                            content = combinedEitaaText,
                            title = "متن پست‌های کانال ${state.settings.eitaaChannelId}",
                            showCopyAction = true,
                            showMetadataBar = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTabContent(
    state: PressUiState,
    onSelectLogFilter: (LogLevelFilter) -> Unit,
    onClearLogs: () -> Unit
) {
    val yasinColors = LocalYasinColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(LogLevelFilter.entries) { filter ->
                    FilterChip(
                        selected = state.logFilter == filter,
                        onClick = { onSelectLogFilter(filter) },
                        label = { Text(filter.labelFa) },
                        modifier = Modifier.testTag("log_filter_${filter.name.lowercase()}")
                    )
                }
            }

            if (state.logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("logs_clear_button")
                ) {
                    Text("پاک کردن")
                }
            }
        }

        if (state.logs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.History,
                title = "هنوز لاگی ثبت نشده است",
                description = "هر عملیات ذخیره نوشته، ارسال به ایتایار، بررسی کانال، اجرای ایجنت یا تست سرور اینجا ثبت می‌شود."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.logs, key = { it.id }) { log ->
                    LogEntryCard(log = log, yasinColors = yasinColors)
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(
    log: OperationLogEntity,
    yasinColors: com.example.ui.theme.YasinExtendedColors
) {
    val dateLabel = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        .format(Date(log.timestamp))

    val (container, content, labelFa) = when (log.level) {
        "SUCCESS" -> Triple(yasinColors.successContainer, yasinColors.success, "موفق")
        "ERROR" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "خطا")
        else -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "ثبت")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("operation_log_card_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        label = labelFa,
                        containerColor = container,
                        contentColor = content,
                        dotColor = content
                    )
                    StatusPill(
                        label = log.module,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = log.title,
                style = MaterialTheme.typography.titleSmall
            )

            if (log.details.isNotBlank()) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PressArticleCard(
    article: PressArticleEntity,
    isPublishing: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onPublishEitaa: () -> Unit,
    onShare: () -> Unit
) {
    val yasinColors = LocalYasinColors.current
    val dateLabel = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        .format(Date(article.updatedAt))

    Card(
        onClick = onPreview,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("press_article_card_${article.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (article.isPinned) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = when {
                            !article.eitaaMessageId.isNullOrBlank() -> "منتشرشده در ایتا"
                            article.isPublished -> "منتشرشده"
                            else -> "پیش‌نویس"
                        },
                        containerColor = if (article.isPublished) {
                            yasinColors.successContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (article.isPublished) {
                            yasinColors.success
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                    StatusPill(
                        label = article.category,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (article.body.isNotBlank()) {
                Text(
                    text = article.body,
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
                OutlinedButton(
                    onClick = onPublishEitaa,
                    enabled = !isPublishing,
                    modifier = Modifier.testTag("press_publish_eitaa_button_${article.id}")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (article.eitaaMessageId != null) "ارسال مجدد ایتا" else "انتشار در ایتا")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "سنجاق کردن",
                            tint = if (article.isPinned) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "اشتراک‌گذاری"
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "ویرایش"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PressEditorSheet(
    editor: PressEditorState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onPublishedChange: (Boolean) -> Unit,
    onAiAssist: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val categories = listOf("فنی", "معماری", "یادداشت", "گزارش")
    var showRtlPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (editor.editingId == 0L) "ایجاد نوشته جدید در یاسین پرس" else "ویرایش نوشته",
                style = MaterialTheme.typography.titleLarge
            )
            FilterChip(
                selected = showRtlPreview,
                onClick = { showRtlPreview = !showRtlPreview },
                label = { Text(if (showRtlPreview) "حالت ویرایش" else "پیش‌نمایش RTL") },
                modifier = Modifier.testTag("press_editor_preview_toggle")
            )
        }

        if (showRtlPreview) {
            YasinRtlContentCard(
                content = editor.body.ifBlank { "هنوز متنی وارد نشده است." },
                title = editor.title.ifBlank { "پیش‌نمایش عنوان نوشته" },
                categoryBadge = editor.category,
                showCopyAction = true
            )
        } else {
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                label = { Text("عنوان نوشته") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("press_editor_title_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = editor.category == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = editor.body,
                onValueChange = onBodyChange,
                label = { Text("متن مقاله یا یادداشت") },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("press_editor_body_input")
            )
        }

        if (editor.aiAssistError != null) {
            ErrorBanner(message = editor.aiAssistError)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onAiAssist(
                        if (editor.body.isBlank()) {
                            "یک مقاله فارسی کامل و ساختاریافته بر اساس عنوان بنویس."
                        } else {
                            "متن فعلی را ویراستاری حرفه‌ای و ساختارمند به فارسی بازنویسی کن."
                        }
                    )
                },
                enabled = !editor.isAiAssisting,
                modifier = Modifier
                    .weight(1f)
                    .testTag("press_ai_assist_button")
            ) {
                if (editor.isAiAssisting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(if (editor.body.isBlank()) "نگارش با یاسین هوش" else "بازنویسی با یاسین هوش")
            }
        }

        OutlinedTextField(
            value = editor.tags,
            onValueChange = onTagsChange,
            label = { Text("برچسب‌ها (با کاما جدا کنید)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("وضعیت آماده انتشار", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = editor.isPublished,
                onCheckedChange = onPublishedChange
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = editor.title.isNotBlank() || editor.body.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("press_save_button")
            ) {
                Text("ذخیره در گوشی")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("انصراف")
            }
        }
    }
}
