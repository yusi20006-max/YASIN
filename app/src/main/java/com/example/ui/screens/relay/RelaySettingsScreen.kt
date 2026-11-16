package com.example.ui.screens.relay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrMonoText
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalYasinColors

@Composable
fun RelaySettingsScreen(
    viewModel: RelaySettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val yasinColors = LocalYasinColors.current

    var eitaayarTokenInput by rememberSaveable { mutableStateOf("") }
    var eitaaChannelInput by rememberSaveable { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }

    var relayUrlInput by rememberSaveable { mutableStateOf("") }
    var githubOwnerInput by rememberSaveable { mutableStateOf("yusi20006-max") }
    var initializedFromPrefs by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isSettingsLoaded, state.settings) {
        if (state.isSettingsLoaded && !initializedFromPrefs) {
            eitaayarTokenInput = state.settings.eitaayarToken
            eitaaChannelInput = state.settings.eitaaChannelId
            relayUrlInput = state.settings.relayBaseUrl
            githubOwnerInput = state.settings.githubOwner
            initializedFromPrefs = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.savedBanner != null) {
                StatusPill(
                    label = state.savedBanner.orEmpty(),
                    containerColor = yasinColors.successContainer,
                    contentColor = yasinColors.success,
                    dotColor = yasinColors.success
                )
            }

            // Eitaayar & Eitaa Channel Configuration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = "تنظیمات کلید ایتایار",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "کد ایتایار (Eitaa-yar API Key)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        val hasSavedKey = state.settings.eitaayarToken.isNotBlank()
                        StatusPill(
                            label = if (hasSavedKey) "ذخیره در DataStore" else "ثبت‌نشده",
                            containerColor = if (hasSavedKey) {
                                yasinColors.successContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (hasSavedKey) {
                                yasinColors.success
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            dotColor = if (hasSavedKey) yasinColors.success else null
                        )
                    }

                    Text(
                        text = "کلید API دریافت‌شده از پنل eitaayar.ir و آیدی کانال مقصد را اینجا وارد کنید. اطلاعات به‌صورت امن و دائمی در Preferences DataStore گوشی ذخیره می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = eitaayarTokenInput,
                            onValueChange = {
                                eitaayarTokenInput = it
                                viewModel.dismissSavedBanner()
                            },
                            label = { Text("Eitaa-yar API Key / کد ایتایار") },
                            placeholder = { Text("bot123456:xxxxxxxxxxxxxxxx") },
                            singleLine = true,
                            visualTransformation = if (showToken) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                Row {
                                    if (eitaayarTokenInput.isNotEmpty()) {
                                        IconButton(onClick = {
                                            eitaayarTokenInput = ""
                                            viewModel.clearEitaayarToken()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "پاک کردن کد ایتایار"
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showToken = !showToken }) {
                                        Icon(
                                            imageVector = if (showToken) {
                                                Icons.Outlined.VisibilityOff
                                            } else {
                                                Icons.Outlined.Visibility
                                            },
                                            contentDescription = if (showToken) "مخفی کردن کلید" else "نمایش کلید"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("eitaayar_token_input")
                        )

                        OutlinedTextField(
                            value = eitaaChannelInput,
                            onValueChange = {
                                eitaaChannelInput = it
                                viewModel.dismissSavedBanner()
                            },
                            label = { Text("آیدی یا شناسه کانال ایتا (Channel / Chat ID)") },
                            placeholder = { Text("yasin_press یا 12345678") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("eitaa_channel_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveEitaayarSettings(eitaayarTokenInput, eitaaChannelInput)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eitaayar_save_button")
                        ) {
                            Text("ذخیره در DataStore")
                        }

                        OutlinedButton(
                            onClick = { viewModel.testEitaaChannel(eitaaChannelInput) },
                            enabled = !state.isTestingEitaa && eitaaChannelInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eitaa_test_channel_button")
                        ) {
                            if (state.isTestingEitaa) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("بررسی کانال ایتا")
                        }
                    }

                    val eitaaRes = state.eitaaTestResult
                    if (eitaaRes != null) {
                        if (eitaaRes.success) {
                            Surface(
                                color = yasinColors.successContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                        contentDescription = "موفق",
                                        tint = yasinColors.success
                                    )
                                    Text(
                                        text = eitaaRes.messageFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = yasinColors.success
                                    )
                                }
                            }
                        } else {
                            ErrorBanner(message = eitaaRes.messageFa)
                        }
                    }
                }
            }

        // Phase 2 Remote Relay & GitHub Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "پل ارتباطی فاز ۲ (Yasin Relay & Remote MCP)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "آدرس سرور ریموت (Render، Hugging Face یا Cloudflare Tunnel) و نام کاربری گیت‌هاب اکوسیستم را اینجا تنظیم کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = relayUrlInput,
                        onValueChange = {
                            relayUrlInput = it
                            viewModel.dismissSavedBanner()
                        },
                        label = { Text("Yasin Relay Base URL (Optional)") },
                        placeholder = { Text("https://your-relay.onrender.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("relay_url_input")
                    )

                    OutlinedTextField(
                        value = githubOwnerInput,
                        onValueChange = {
                            githubOwnerInput = it
                            viewModel.dismissSavedBanner()
                        },
                        label = { Text("GitHub Ecosystem Username") },
                        placeholder = { Text("yusi20006-max") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("relay_github_owner_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveSettings(relayUrlInput, githubOwnerInput) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("relay_save_button")
                    ) {
                        Text(stringResource(R.string.save_settings))
                    }

                    OutlinedButton(
                        onClick = { viewModel.testConnection(relayUrlInput) },
                        enabled = !state.isPinging,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("relay_ping_button")
                    ) {
                        if (state.isPinging) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(stringResource(R.string.relay_test_btn))
                    }
                }

                val ping = state.pingResult
                if (ping != null) {
                    if (ping.reachable) {
                        Surface(
                            color = yasinColors.successContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = "متصل",
                                    tint = yasinColors.success
                                )
                                Text(
                                    text = ping.detailFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = yasinColors.success
                                )
                            }
                        }
                    } else {
                        ErrorBanner(message = ping.detailFa)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تنظیمات ظاهری و هوش مصنوعی", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تم تیره اکوسیستم", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "استفاده از پالت سرمه‌ای عمیق با کنتراست بالا",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.settings.darkTheme,
                        onCheckedChange = viewModel::toggleDarkTheme,
                        modifier = Modifier.testTag("settings_dark_theme_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ارسال خودکار کانتکست به ایجنت", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "همراه‌کردن وضعیت ماژول‌ها و تعداد نوشته‌های پرس با دستورات ایجنت",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.settings.autoAttachContext,
                        onCheckedChange = viewModel::toggleAutoContext
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "واسط‌های آماده توسعه برای فاز ۲ و ۳",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                ContractRow("AiGateway", "فاز ۱: Firebase AI Logic (Gemini 3.8 Flash) → فاز ۲: Yasin Relay AI")
                ContractRow("PressRepository & EitaaRepository", "فاز ۱: Room SQLite + ایتایار → فاز ۲: همگام‌سازی ابری")
                ContractRow("LogRepository", "ثبت دائمی وقایع انتشار و عملیات در Room")
                ContractRow("EcosystemInspectorRepository", "فاز ۱: GitHub REST API → فاز ۲: Yasin-MCP HTTP")
            }
        }

        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ContractRow(contractName: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LtrMonoText(
            text = contractName,
            color = MaterialTheme.colorScheme.primary,
            fontSizeSp = 12
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
