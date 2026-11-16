package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.delay

sealed interface RtlContentBlock {
    data class Heading(val level: Int, val text: String) : RtlContentBlock
    data class Paragraph(val text: String) : RtlContentBlock
    data class BulletItem(val text: String) : RtlContentBlock
    data class NumberedItem(val number: String, val text: String) : RtlContentBlock
    data class Quote(val text: String) : RtlContentBlock
    data class CodeBlock(val language: String, val code: String) : RtlContentBlock
    data object Divider : RtlContentBlock
}

/**
 * کامپوننت پایه نمایش محتوای فارسی (RTL) با پشتیبانی از متون ترکیبی (BiDi)،
 * فونت استاندارد وزیرمتن، بلاک‌های کد چپ‌چین (JetBrains Mono) و انتخاب متن.
 */
@Composable
fun YasinRtlContentView(
    content: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    categoryBadge: String? = null,
    showCopyAction: Boolean = true,
    showMetadataBar: Boolean = true,
    selectable: Boolean = true,
    compact: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    var copiedAll by remember { mutableStateOf(false) }

    LaunchedEffect(copiedAll) {
        if (copiedAll) {
            delay(1800)
            copiedAll = false
        }
    }

    val blocks = remember(content) { parseRtlContentBlocks(content) }
    val wordCount = remember(content) {
        content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    val bodyTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = VazirmatnFontFamily,
        fontSize = if (compact) 14.sp else 15.sp,
        lineHeight = if (compact) 24.sp else 27.sp,
        textDirection = TextDirection.ContentOrRtl,
        textAlign = TextAlign.Start
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("yasin_rtl_content_view"),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            if (!title.isNullOrBlank() || showMetadataBar || showCopyAction) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (showMetadataBar || showCopyAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!categoryBadge.isNullOrBlank()) {
                                StatusPill(
                                    label = categoryBadge,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (showMetadataBar && wordCount > 0) {
                                Text(
                                    text = "$wordCount کلمه",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showCopyAction && content.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val fullText = if (!title.isNullOrBlank()) {
                                        "$title\n\n$content"
                                    } else {
                                        content
                                    }
                                    clipboard.setText(AnnotatedString(fullText))
                                    copiedAll = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rtl_content_copy_all_button")
                            ) {
                                Icon(
                                    imageVector = if (copiedAll) {
                                        Icons.Outlined.Check
                                    } else {
                                        Icons.Outlined.ContentCopy
                                    },
                                    contentDescription = if (copiedAll) "کپی شد" else "کپی کل محتوا",
                                    tint = if (copiedAll) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            val bodyContent: @Composable () -> Unit = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    blocks.forEach { block ->
                        RtlBlockRenderer(
                            block = block,
                            bodyStyle = bodyTextStyle,
                            compact = compact
                        )
                    }
                }
            }

            if (selectable) {
                SelectionContainer {
                    bodyContent()
                }
            } else {
                bodyContent()
            }
        }
    }
}

/**
 * کارت آماده بر پایه YasinRtlContentView برای نمایش مقاله‌ها، پاسخ‌ها و مستندات.
 */
@Composable
fun YasinRtlContentCard(
    content: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    categoryBadge: String? = null,
    showCopyAction: Boolean = true,
    showMetadataBar: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        YasinRtlContentView(
            content = content,
            title = title,
            categoryBadge = categoryBadge,
            showCopyAction = showCopyAction,
            showMetadataBar = showMetadataBar,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun RtlBlockRenderer(
    block: RtlContentBlock,
    bodyStyle: TextStyle,
    compact: Boolean
) {
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val inlineCodeFg = MaterialTheme.colorScheme.primary

    when (block) {
        is RtlContentBlock.Heading -> {
            val headingStyle = when (block.level) {
                1 -> MaterialTheme.typography.titleLarge
                2 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }.copy(
                fontFamily = VazirmatnFontFamily,
                fontWeight = FontWeight.Bold,
                textDirection = TextDirection.ContentOrRtl,
                textAlign = TextAlign.Start
            )
            Text(
                text = formatInlinePersianText(block.text, inlineCodeBg, inlineCodeFg),
                style = headingStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = if (compact) 2.dp else 6.dp)
            )
        }

        is RtlContentBlock.Paragraph -> {
            Text(
                text = formatInlinePersianText(block.text, inlineCodeBg, inlineCodeFg),
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        is RtlContentBlock.BulletItem -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 9.dp)
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = formatInlinePersianText(block.text, inlineCodeBg, inlineCodeFg),
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        is RtlContentBlock.NumberedItem -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${block.number}.",
                    style = bodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatInlinePersianText(block.text, inlineCodeBg, inlineCodeFg),
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        is RtlContentBlock.Quote -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = formatInlinePersianText(block.text, inlineCodeBg, inlineCodeFg),
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }

        is RtlContentBlock.CodeBlock -> {
            RtlAwareCodeBlock(
                language = block.language,
                code = block.code
            )
        }

        RtlContentBlock.Divider -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun RtlAwareCodeBlock(
    language: String,
    code: String
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrMonoText(
                    text = language.ifBlank { "code" },
                    color = MaterialTheme.colorScheme.primary,
                    fontSizeSp = 11
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("rtl_code_block_copy_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = if (copied) "کد کپی شد" else "کپی کد",
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Code blocks are explicitly rendered LTR with horizontal scroll
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = code,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        }
    }
}

private fun formatInlinePersianText(
    raw: String,
    codeBg: Color,
    codeFg: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            when {
                raw.startsWith("**", i) -> {
                    val end = raw.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(raw.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                raw[i] == '`' -> {
                    val end = raw.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                background = codeBg,
                                color = codeFg,
                                fontSize = 13.sp
                            )
                        ) {
                            append(" ${raw.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                else -> {
                    append(raw[i])
                    i++
                }
            }
        }
    }
}

fun parseRtlContentBlocks(rawContent: String): List<RtlContentBlock> {
    if (rawContent.isBlank()) return emptyList()

    val lines = rawContent.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<RtlContentBlock>()
    val paragraphBuffer = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks.add(RtlContentBlock.Paragraph(paragraphBuffer.joinToString("\n")))
            paragraphBuffer.clear()
        }
    }

    var idx = 0
    while (idx < lines.size) {
        val line = lines[idx]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            idx++
            while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                codeLines.add(lines[idx])
                idx++
            }
            blocks.add(RtlContentBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
            idx++
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            idx++
            continue
        }

        if (trimmed == "---" || trimmed == "***") {
            flushParagraph()
            blocks.add(RtlContentBlock.Divider)
            idx++
            continue
        }

        val headingMatch = Regex("^(#{1,3})\\s+(.+)$").find(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            blocks.add(
                RtlContentBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text = headingMatch.groupValues[2].trim()
                )
            )
            idx++
            continue
        }

        if (trimmed.startsWith("> ")) {
            flushParagraph()
            blocks.add(RtlContentBlock.Quote(trimmed.removePrefix("> ").trim()))
            idx++
            continue
        }

        val bulletMatch = Regex("^[-*•]\\s+(.+)$").find(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            blocks.add(RtlContentBlock.BulletItem(bulletMatch.groupValues[1].trim()))
            idx++
            continue
        }

        val numberMatch = Regex("^(\\d+|[۰-۹]+)[.)]\\s+(.+)$").find(trimmed)
        if (numberMatch != null) {
            flushParagraph()
            blocks.add(
                RtlContentBlock.NumberedItem(
                    number = numberMatch.groupValues[1],
                    text = numberMatch.groupValues[2].trim()
                )
            )
            idx++
            continue
        }

        paragraphBuffer.add(line.trimEnd())
        idx++
    }

    flushParagraph()
    return blocks
}
