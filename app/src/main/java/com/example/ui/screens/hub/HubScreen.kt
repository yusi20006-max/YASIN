package com.example.ui.screens.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.registry.ModuleExecutionMode
import com.example.core.registry.Routes
import com.example.core.registry.YasinModuleDescriptor
import com.example.ui.components.AtmosphericBanner
import com.example.ui.components.LtrMonoText
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalYasinColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    viewModel: HubViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val yasinColors = LocalYasinColors.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 640.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AtmosphericBanner(
                    badge = stringResource(R.string.hub_hero_badge),
                    title = stringResource(R.string.hub_hero_title),
                    subtitle = stringResource(R.string.hub_hero_desc),
                    trailingContent = {
                        IconButton(
                            onClick = { onNavigateToRoute(Routes.RELAY_SETTINGS) },
                            modifier = Modifier.testTag("hub_open_relay_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "تنظیمات و ایتایار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        value = "${state.nativeActiveCount}",
                        label = "ماژول نیتیو فعال",
                        accent = yasinColors.success,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        value = "${state.pressArticleCount}",
                        label = "نوشته یاسین پرس",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        value = "${state.remoteReadyCount}",
                        label = "ماژول فاز ۲ ریموت",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معماری ماژول‌های یاسین",
                            style = MaterialTheme.typography.titleLarge
                        )
                        val relayText = if (state.settings.relayBaseUrl.isBlank()) {
                            "فاز ۱: تمام‌نیتیو"
                        } else {
                            "ریلی تنظیم‌شده"
                        }
                        StatusPill(
                            label = relayText,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(HubModuleFilter.entries) { filter ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(filter.labelFa) },
                                modifier = Modifier.testTag("hub_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            val rows = if (isWide) state.modules.chunked(2) else state.modules.map { listOf(it) }
            items(rows, key = { row -> row.joinToString("-") { it.id } }) { rowModules ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowModules.forEach { module ->
                        YasinModuleCard(
                            module = module,
                            onPrimaryClick = {
                                if (module.executionMode == ModuleExecutionMode.NATIVE_ACTIVE &&
                                    module.targetRoute != null &&
                                    module.targetRoute != Routes.HUB
                                ) {
                                    onNavigateToRoute(module.targetRoute)
                                } else {
                                    viewModel.openModuleDetails(module)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (isWide && rowModules.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    val sheetModule = state.selectedModuleForSheet
    if (sheetModule != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissModuleDetails() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ModuleDetailSheetContent(
                module = sheetModule,
                relayUrl = state.settings.relayBaseUrl,
                onOpenRoute = { route ->
                    viewModel.dismissModuleDetails()
                    onNavigateToRoute(route)
                },
                onClose = { viewModel.dismissModuleDetails() }
            )
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun YasinModuleCard(
    module: YasinModuleDescriptor,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val yasinColors = LocalYasinColors.current
    val isNative = module.executionMode == ModuleExecutionMode.NATIVE_ACTIVE

    Card(
        onClick = onPrimaryClick,
        modifier = modifier.testTag("module_card_${module.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isNative) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isNative) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = module.titleFa,
                            tint = if (isNative) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                    Column {
                        Text(
                            text = module.titleFa,
                            style = MaterialTheme.typography.titleMedium
                        )
                        LtrMonoText(
                            text = module.codeName,
                            fontSizeSp = 11
                        )
                    }
                }

                StatusPill(
                    label = module.executionMode.badgeFa,
                    containerColor = if (isNative) {
                        yasinColors.successContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (isNative) {
                        yasinColors.success
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    dotColor = if (isNative) yasinColors.success else MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = module.roleFa,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = module.layer.titleFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "ورود به ${module.titleFa}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ModuleDetailSheetContent(
    module: YasinModuleDescriptor,
    relayUrl: String,
    onOpenRoute: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = module.titleFa,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Column {
                Text(text = module.titleFa, style = MaterialTheme.typography.titleLarge)
                LtrMonoText(text = module.codeName, fontSizeSp = 12)
            }
        }

        Text(
            text = module.roleFa,
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "قرارداد توسعه و اتصال فازهای بعدی",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = module.phase2ContractFa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (relayUrl.isNotBlank()) {
                    LtrMonoText(text = "Relay: $relayUrl", fontSizeSp = 11)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (module.targetRoute != null) {
                Button(
                    onClick = { onOpenRoute(module.targetRoute) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (module.executionMode == ModuleExecutionMode.NATIVE_ACTIVE) {
                            "باز کردن بخش"
                        } else {
                            "تنظیم پل ریموت"
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("بستن")
            }
        }
    }
}
