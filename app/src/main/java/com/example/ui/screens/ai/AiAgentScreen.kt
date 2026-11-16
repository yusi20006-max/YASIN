package com.example.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.registry.Routes
import com.example.data.local.ChatMessageEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.YasinRtlContentView
import com.example.ui.theme.LocalYasinColors

@Composable
fun AiAgentScreen(
    viewModel: AiAgentViewModel,
    onNavigateToRoute: (String) -> Unit,
    initialPrompt: String? = null,
    onConsumedInitialPrompt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.switchMode(AiWorkspaceMode.CHAT)
            inputText = initialPrompt
            onConsumedInitialPrompt()
        }
    }

    LaunchedEffect(state.messages.size, state.operationState) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.weight(1f)
            ) {
                AiWorkspaceMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.selectedMode == mode,
                        onClick = { viewModel.switchMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AiWorkspaceMode.entries.size
                        ),
                        modifier = Modifier.testTag("ai_mode_${mode.dbKey}")
                    ) {
                        Text(mode.labelFa, maxLines = 1)
                    }
                }
            }

            IconButton(
                onClick = { viewModel.clearCurrentModeHistory() },
                modifier = Modifier.testTag("ai_clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.clear_chat_cd)
                )
            }
        }

        if (state.operationState is AiOperationState.Error) {
            val err = (state.operationState as AiOperationState.Error).message
            ErrorBanner(
                message = err,
                onRetry = { viewModel.dismissError() }
            )
        }

        val starterPrompts = if (state.selectedMode == AiWorkspaceMode.CHAT) {
            listOf(
                "نقش ماژول‌های اکوسیستم یاسین را خلاصه توضیح بده",
                "یک ایده ساختاریافته برای فاز ۲ سرور ریلی بده",
                "مزایای معماری بومی ماژولار در اندروید چیست؟"
            )
        } else {
            listOf(
                "یک پیش‌نویس مقاله درباره معماری Yasin-MCP در یاسین پرس بساز",
                "مخزن Openfeed را از گیت‌هاب بررسی و خلاصه کن",
                "وضعیت آماده‌بودن ماژول‌های فعلی اکوسیستم را تحلیل کن"
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.messages.isEmpty() && state.operationState !is AiOperationState.Streaming) {
                item {
                    EmptyStateView(
                        icon = if (state.selectedMode == AiWorkspaceMode.CHAT) {
                            Icons.Outlined.AutoAwesome
                        } else {
                            Icons.Outlined.SmartToy
                        },
                        title = if (state.selectedMode == AiWorkspaceMode.CHAT) {
                            "گفتگو با یاسین هوش (Gemini)"
                        } else {
                            "دستیار خودکار یاسین ایجنت"
                        },
                        description = if (state.selectedMode == AiWorkspaceMode.CHAT) {
                            "متصل به Firebase AI Logic. سؤال فنی بپرسید یا متن‌های خود را تحلیل کنید."
                        } else {
                            "هدف خود را بنویسید؛ ایجنت می‌تواند مستقیماً پیش‌نویس در یاسین پرس بسازد یا مخازن گیت‌هاب را بازرسی کند."
                        }
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(starterPrompts) { prompt ->
                            AssistChip(
                                onClick = { viewModel.sendInput(prompt) },
                                label = { Text(prompt, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onCopy = { clipboard.setText(AnnotatedString(msg.content)) },
                    onActionNavigate = { actionType ->
                        when (actionType) {
                            "CREATE_PRESS_DRAFT" -> onNavigateToRoute(Routes.PRESS)
                            "INSPECT_REPO" -> onNavigateToRoute(Routes.DOCS_MCP)
                        }
                    }
                )
            }

            if (state.operationState is AiOperationState.Streaming) {
                val streamingText = (state.operationState as AiOperationState.Streaming).partialText
                item(key = "streaming_bubble") {
                    ChatBubbleItem(
                        message = ChatMessageEntity(
                            role = "model",
                            mode = state.selectedMode.dbKey,
                            content = streamingText
                        ),
                        onCopy = {},
                        onActionNavigate = {}
                    )
                }
            } else if (state.operationState is AiOperationState.Loading) {
                item(key = "loading_indicator") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (state.selectedMode == AiWorkspaceMode.CHAT) {
                                "در حال دریافت پاسخ استریم از Gemini..."
                            } else {
                                "ایجنت در حال برنامه‌ریزی و اجرای عملیات..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (state.selectedMode == AiWorkspaceMode.CHAT) {
                            stringResource(R.string.ai_input_hint)
                        } else {
                            stringResource(R.string.agent_input_hint)
                        }
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                maxLines = 4,
                shape = MaterialTheme.shapes.medium
            )

            FilledIconButton(
                onClick = {
                    val toSend = inputText
                    inputText = ""
                    viewModel.sendInput(toSend)
                },
                enabled = inputText.isNotBlank() &&
                    state.operationState !is AiOperationState.Loading &&
                    state.operationState !is AiOperationState.Streaming,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_cd)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onActionNavigate: (String) -> Unit
) {
    val isUser = message.role == "user"
    val isAgent = message.role == "agent"
    val yasinColors = LocalYasinColors.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUser -> MaterialTheme.colorScheme.primaryContainer
                    isAgent -> yasinColors.agentContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            border = BorderStroke(
                1.dp,
                when {
                    isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    isAgent -> yasinColors.agentAccent.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                }
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isUser -> "شما"
                            isAgent -> "یاسین ایجنت"
                            else -> "یاسین هوش"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isUser -> MaterialTheme.colorScheme.primary
                            isAgent -> yasinColors.agentAccent
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )

                    if (!isUser && message.content.isNotBlank()) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "کپی متن",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isAgent && !message.actionType.isNullOrBlank() && message.actionType != "ANSWER_ONLY") {
                    val actionLabel = when (message.actionType) {
                        "CREATE_PRESS_DRAFT" -> "عملیات انجام‌شده: ثبت پیش‌نویس در یاسین پرس"
                        "INSPECT_REPO" -> "عملیات انجام‌شده: بازرسی مخزن ${message.actionTarget.orEmpty()}"
                        else -> message.actionType
                    }
                    StatusPill(
                        label = actionLabel,
                        containerColor = yasinColors.successContainer,
                        contentColor = yasinColors.success,
                        dotColor = yasinColors.success
                    )
                }

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    YasinRtlContentView(
                        content = message.content,
                        showCopyAction = false,
                        showMetadataBar = false,
                        selectable = false,
                        compact = true
                    )
                }

                if (isAgent && (message.actionType == "CREATE_PRESS_DRAFT" || message.actionType == "INSPECT_REPO")) {
                    OutlinedButton(
                        onClick = { onActionNavigate(message.actionType) }
                    ) {
                        Icon(
                            imageVector = if (message.actionType == "CREATE_PRESS_DRAFT") {
                                Icons.Outlined.Newspaper
                            } else {
                                Icons.Outlined.Description
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (message.actionType == "CREATE_PRESS_DRAFT") {
                                "مشاهده در یاسین پرس"
                            } else {
                                "مشاهده در اسناد و MCP"
                            }
                        )
                    }
                }
            }
        }
    }
}
