package com.example.ui.screens.press

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.TelegramChannelEntity
import com.example.ui.components.LtrMonoText
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalYasinColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelegramAutoFeedTabContent(
    state: PressUiState,
    onToggleAutoPublish: (Boolean) -> Unit,
    onSelectHourlyLimit: (Int) -> Unit,
    onToggleAutoRewrite: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onAddChannel: (String, String) -> Unit,
    onToggleChannel: (TelegramChannelEntity) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onRestoreDefaults: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val yasinColors = LocalYasinColors.current
    var newChannelInput by rememberSaveable { mutableStateOf("") }
    var newChannelTitleInput by rememberSaveable { mutableStateOf("") }

    val hourlyPresets = listOf(10, 20, 30)
    val enabledCount = state.telegramChannels.count { it.isEnabled }
    val remaining = (state.settings.pressHourlyLimit - state.publishedLastHour).coerceAtLeast(0)
    val hasEitaaConfigured = state.settings.eitaayarToken.isNotBlank() &&
        state.settings.eitaaChannelId.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Hourly Auto-Publisher Control & Quota (20/hour)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "انتشار خودکار ساعتی یاسین پرس",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        StatusPill(
                            label = if (state.settings.pressAutoPublishEnabled) "فعال (هر ۱ ساعت)" else "خاموش",
                            containerColor = if (state.settings.pressAutoPublishEnabled) {
                                yasinColors.successContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (state.settings.pressAutoPublishEnabled) {
                                yasinColors.success
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            dotColor = if (state.settings.pressAutoPublishEnabled) yasinColors.success else null
                        )
                    }

                    Text(
                        text = "اخبار جدید را به‌صورت خودکار از کانال‌های تلگرام فعال جمع‌آوری می‌کند، از تکرار جلوگیری کرده و تا سقف ساعتی انتخاب‌شده در کانال ایتا منتشر می‌کند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasEitaaConfigured) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "برای ارسال خودکار در ایتا، کد ایتایار را ثبت کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onOpenSettings) {
                                    Text("تنظیم")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("زمان‌بند خودکار هر ساعت", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "اجرای پس‌زمینه استاندارد با WorkManager هنگام اتصال اینترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.settings.pressAutoPublishEnabled,
                            onCheckedChange = onToggleAutoPublish,
                            modifier = Modifier.testTag("press_auto_publish_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text("بازنویسی خبر با یاسین هوش", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "خلاصه‌سازی و ویراستاری خبر پیش از انتشار",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.settings.pressAutoRewriteWithAi,
                            onCheckedChange = onToggleAutoRewrite,
                            modifier = Modifier.testTag("press_auto_rewrite_switch")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "سقف انتشار ساعتی: ${state.settings.pressHourlyLimit} خبر در ساعت (باقی‌مانده این ساعت: $remaining)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(hourlyPresets) { preset ->
                                FilterChip(
                                    selected = state.settings.pressHourlyLimit == preset,
                                    onClick = { onSelectHourlyLimit(preset) },
                                    label = { Text("$preset خبر / ساعت") },
                                    modifier = Modifier.testTag("press_hourly_limit_$preset")
                                )
                            }
                        }
                    }

                    if (state.settings.pressLastAutoRunAt > 0L) {
                        val lastRunLabel = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            .format(Date(state.settings.pressLastAutoRunAt))
                        Text(
                            text = "آخرین اجرا: $lastRunLabel • کانال‌های فعال: $enabledCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRunNow,
                        enabled = !state.isRunningAutoSync,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("press_run_auto_now_button")
                    ) {
                        if (state.isRunningAutoSync) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("در حال دریافت و انتشار اخبار...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("اجرای فوری گردآوری و انتشار (${state.settings.pressHourlyLimit} خبر)")
                        }
                    }
                }
            }
        }

        // Card 2: Add Custom Telegram Channel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "افزودن کانال تلگرام دلخواه",
                        style = MaterialTheme.typography.titleSmall
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = newChannelInput,
                            onValueChange = { newChannelInput = it },
                            label = { Text("آیدی یا لینک کانال تلگرام (@channel یا t.me/...)") },
                            placeholder = { Text("@zoomit یا https://t.me/khabarfouri") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("press_add_tg_channel_input")
                        )
                    }
                    OutlinedTextField(
                        value = newChannelTitleInput,
                        onValueChange = { newChannelTitleInput = it },
                        label = { Text("نام نمایشی فارسی (اختیاری)") },
                        placeholder = { Text("مثلاً: اخبار فناوری") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            if (newChannelInput.isNotBlank()) {
                                onAddChannel(newChannelInput, newChannelTitleInput)
                                newChannelInput = ""
                                newChannelTitleInput = ""
                            }
                        },
                        enabled = newChannelInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("press_add_tg_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("افزودن به لیست کانال‌ها")
                    }
                }
            }
        }

        // Header for channels list + restore defaults
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "لیست کانال‌های تلگرام (${state.telegramChannels.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                TextButton(onClick = onRestoreDefaults) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("بازیابی پیش‌فرض‌ها")
                }
            }
        }

        items(state.telegramChannels, key = { it.username }) { channel ->
            TelegramChannelCard(
                channel = channel,
                onToggle = { onToggleChannel(channel) },
                onDelete = { onDeleteChannel(channel.username) }
            )
        }
    }
}

@Composable
private fun TelegramChannelCard(
    channel: TelegramChannelEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tg_channel_card_${channel.username}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (channel.isEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = channel.titleFa,
                        style = MaterialTheme.typography.titleSmall
                    )
                    StatusPill(
                        label = if (channel.isDefault) "پیش‌فرض" else "کاربر",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LtrMonoText(
                    text = "@${channel.username}  •  t.me/s/${channel.username}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSizeSp = 12
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = channel.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "حذف کانال",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
