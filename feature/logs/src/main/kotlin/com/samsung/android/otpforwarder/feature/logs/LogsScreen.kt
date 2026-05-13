package com.samsung.android.otpforwarder.feature.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpTheme
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { /* navigation handled by NavGraph */ }

    LogsContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun LogsContent(
    state: LogsState,
    onIntent: (LogsIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        LogsTopBar(state = state, onIntent = onIntent)

        state.selectedLog?.let { log ->
            LogDetailDialog(
                log = log,
                onDismiss = { onIntent(LogsIntent.CloseDetail) }
            )
        }

        val displayGroups = state.filteredGroups
        val isSearchWithNoResults = state.searchQuery.isNotBlank() && displayGroups.isEmpty()

        when {
            isSearchWithNoResults -> NoSearchResults(query = state.searchQuery)
            displayGroups.isEmpty() && !state.isLoading -> EmptyLogsState()
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        TodaySummaryCard(
                            forwarded = state.todayForwarded,
                            failed    = state.todayFailed,
                            pending   = state.todayPending,
                            modifier  = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    displayGroups.forEach { group ->
                        item {
                            Text(
                                text     = group.label,
                                style    = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight    = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(group.items, key = { it.id }) { item ->
                            LogRowItem(
                                item     = item,
                                onIntent = onIntent,
                                onRetry  = { onIntent(LogsIntent.RetryForwarding(it)) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@Composable
private fun LogsTopBar(
    state: LogsState,
    onIntent: (LogsIntent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "Logs",
                style    = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // Search icon — toggles the search bar
            IconButton(onClick = { onIntent(LogsIntent.ToggleSearch) }) {
                Icon(
                    imageVector        = if (state.isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (state.isSearchActive) "Close search" else "Search",
                )
            }
            IconButton(onClick = { /* TODO: filter */ }) {
                Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
            }
        }

        // Animated search bar that expands below the title row
        AnimatedVisibility(
            visible = state.isSearchActive,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = { onIntent(LogsIntent.SetSearchQuery(it)) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .focusRequester(focusRequester),
                placeholder   = { Text("Search sender, OTP, message…") },
                singleLine    = true,
                leadingIcon   = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon  = if (state.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onIntent(LogsIntent.SetSearchQuery("")) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape  = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            // Auto-focus when the bar appears
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}

// ── Today summary card ────────────────────────────────────────────────────────

@Composable
private fun TodaySummaryCard(
    forwarded: Int,
    failed: Int,
    pending: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column {
            Text(
                text  = "Today",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text  = "$forwarded forwarded",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (failed > 0 || pending > 0) {
                val parts = mutableListOf<String>()
                if (failed > 0)  parts += "$failed failed"
                if (pending > 0) parts += "$pending pending"
                Text(
                    text  = parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

// ── Log row item ──────────────────────────────────────────────────────────────

@Composable
private fun LogRowItem(
    item: LogRowUiItem,
    onIntent: (LogsIntent) -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val ext = OtpTheme.extendedColors

    val (iconBg, iconTint, icon) = when (item.status) {
        ForwardingStatus.FORWARDED    -> Triple(ext.successContainer, ext.onSuccessContainer, Icons.Rounded.Check)
        ForwardingStatus.PENDING,
        ForwardingStatus.RETRY_QUEUED -> Triple(ext.warningContainer, ext.onWarningContainer, Icons.Rounded.Schedule)
        ForwardingStatus.FAILED       -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Rounded.Close,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onIntent(LogsIntent.OpenDetail(item.id)) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIconCircle(iconBg = iconBg, iconTint = iconTint, icon = icon)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = item.sender,
                style    = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.15.sp,
                ),
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = item.otp,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick  = { clipboardManager.setText(AnnotatedString(item.otp)) },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy OTP",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text  = item.deliveryLine,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = item.timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.status != ForwardingStatus.FORWARDED) {
                IconButton(onClick = { onRetry(item.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry sending SMS",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIconCircle(
    iconBg: Color,
    iconTint: Color,
    icon: ImageVector,
) {
    Box(
        modifier         = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(iconBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(20.dp),
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyLogsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "No OTPs yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "When an OTP arrives it will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "No results for \"$query\"",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Try a different sender name, OTP, or message text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

// ── Detail Dialog ─────────────────────────────────────────────────────────────

@Composable
private fun LogDetailDialog(
    log: LogRowUiItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Message Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sender",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = log.sender,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = log.timeLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Full Message",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = log.fullBody,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LogsPreview() {
    OtpForwarderTheme {
        LogsContent(
            state = LogsState(
                todayForwarded = 12,
                todayFailed    = 2,
                todayPending   = 1,
                groups = listOf(
                    LogGroup(
                        label = "TODAY",
                        items = listOf(
                            LogRowUiItem("1", "HDFC Bank", "842913",   "This is the full SMS body from HDFC Bank.", "SMS delivered · Email delivered", ForwardingStatus.FORWARDED,    "2m ago"),
                            LogRowUiItem("2", "Amazon",    "4127",    "This is the full SMS body from Amazon.",    "Email delivered",                ForwardingStatus.FORWARDED,    "18m ago"),
                            LogRowUiItem("3", "Uber",      "57382",   "This is the full SMS body from Uber.",      "Retrying",                       ForwardingStatus.RETRY_QUEUED, "42m ago"),
                            LogRowUiItem("4", "Google",    "21539478","This is the full SMS body from Google.",    "SMS send failed",                ForwardingStatus.FAILED,       "1h ago"),
                        ),
                    ),
                    LogGroup(
                        label = "YESTERDAY",
                        items = listOf(
                            LogRowUiItem("5", "ICICI Bank", "7385", "This is the full SMS body from ICICI Bank.", "SMS delivered", ForwardingStatus.FORWARDED, "yday 22:14"),
                        ),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
