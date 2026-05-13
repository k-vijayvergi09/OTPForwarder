package com.samsung.android.otpforwarder.feature.destinations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun DestinationsScreen(
    onAddPhone: () -> Unit = {},
    onAddEmail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DestinationsViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { effect ->
        when (effect) {
            DestinationsSideEffect.NavigateToAddPhone -> onAddPhone()
            DestinationsSideEffect.NavigateToAddEmail -> onAddEmail()
            DestinationsSideEffect.GoToSettings       -> onNavigateToSettings()
        }
    }

    DestinationsContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun DestinationsContent(
    state: DestinationsState,
    onIntent: (DestinationsIntent) -> Unit,
) {
    val isEmpty = !state.isLoading &&
        state.smsDestinations.isEmpty() &&
        state.emailDestinations.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        DestinationsTopBar(onSettings = { onIntent(DestinationsIntent.NavigateToSettings) })

        if (isEmpty) {
            EmptyDestinationsContent(onIntent = onIntent)
        } else {
            DestinationsList(state = state, onIntent = onIntent)
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun DestinationsTopBar(onSettings: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = "Destinations",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

// ── Populated list ────────────────────────────────────────────────────────────

@Composable
private fun DestinationsList(
    state: DestinationsState,
    onIntent: (DestinationsIntent) -> Unit,
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── PHONE NUMBERS section ──────────────────────────────────────────────
        item {
            SectionHeader(
                title    = "PHONE NUMBERS",
                onAdd    = { onIntent(DestinationsIntent.AddPhone) },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }

        if (state.smsDestinations.isEmpty()) {
            item {
                EmptySectionHint(
                    hint     = "No phone numbers added yet",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        } else {
            items(state.smsDestinations, key = { it.id }) { item ->
                SwipeToDeleteRow(
                    onDelete = { onIntent(DestinationsIntent.DeleteSms(item.id)) },
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    DestinationRow(
                        icon      = if (item.label.lowercase().contains("office") ||
                                       item.label.lowercase().contains("work"))
                                       Icons.Rounded.Business else Icons.Rounded.Call,
                        label     = item.label.ifBlank { item.phoneNumber },
                        value     = item.phoneNumber,
                        isEnabled = item.isEnabled,
                        onToggle  = { onIntent(DestinationsIntent.ToggleSms(item.id)) },
                    )
                }
                if (state.smsDestinations.last() != item) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 24.dp),
                        thickness = 1.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }

        // ── EMAIL ADDRESSES section ────────────────────────────────────────────
        item {
            SectionHeader(
                title    = "EMAIL ADDRESSES",
                onAdd    = { onIntent(DestinationsIntent.AddEmail) },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }

        if (state.emailDestinations.isEmpty()) {
            item {
                EmptySectionHint(
                    hint     = "No email addresses added yet",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        } else {
            items(state.emailDestinations, key = { it.id }) { item ->
                SwipeToDeleteRow(
                    onDelete = { onIntent(DestinationsIntent.DeleteEmail(item.id)) },
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    DestinationRow(
                        icon      = if (item.label.lowercase().contains("work"))
                                       Icons.Rounded.Work else Icons.Rounded.Email,
                        label     = item.label.ifBlank { item.emailAddress },
                        value     = item.emailAddress,
                        isEnabled = item.isEnabled,
                        onToggle  = { onIntent(DestinationsIntent.ToggleEmail(item.id)) },
                    )
                }
                if (state.emailDestinations.last() != item) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 24.dp),
                        thickness = 1.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyDestinationsContent(
    onIntent: (DestinationsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier          = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Illustration circle
        Box(
            modifier         = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Rounded.ForwardToInbox,
                contentDescription = null,
                modifier           = Modifier.size(44.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text  = "No destinations yet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text  = "Add a phone number or email to start forwarding OTPs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Primary CTA — Add phone number
        androidx.compose.material3.Button(
            onClick  = { onIntent(DestinationsIntent.AddPhone) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape    = RoundedCornerShape(50),
        ) {
            Text(text = "Add phone number", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(12.dp))

        // Secondary CTA — Add email address
        OutlinedButton(
            onClick  = { onIntent(DestinationsIntent.AddEmail) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape    = RoundedCornerShape(50),
        ) {
            Text(text = "Add email address", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAdd) {
            Text(
                text  = "+ Add",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Empty section hint ────────────────────────────────────────────────────────

@Composable
private fun EmptySectionHint(hint: String, modifier: Modifier = Modifier) {
    Text(
        text     = hint,
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier,
    )
}

// ── Destination row ───────────────────────────────────────────────────────────

@Composable
private fun DestinationRow(
    icon: ImageVector,
    label: String,
    value: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar circle
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier           = Modifier.size(22.dp),
            )
        }

        // Label + value
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Toggle
        Switch(
            checked         = isEnabled,
            onCheckedChange = { onToggle() },
        )
    }
}

// ── Swipe-to-delete wrapper ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
    )

    // Reset the state if the item wasn't actually deleted (shouldn't happen, but safe)
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) return@LaunchedEffect
    }

    SwipeToDismissBox(
        state            = dismissState,
        modifier         = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label         = "swipe_bg_color",
            )
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DestinationsPreview() {
    OtpForwarderTheme {
        DestinationsContent(
            state = DestinationsState(
                isLoading = false,
                smsDestinations = listOf(
                    SmsDestinationUiItem("1", "Personal", "+91 98765 43210", true),
                    SmsDestinationUiItem("2", "Office",   "+91 99887 76655", false),
                ),
                emailDestinations = listOf(
                    EmailDestinationUiItem("3", "Personal", "kartik@gmail.com",   true),
                    EmailDestinationUiItem("4", "Work",     "kartik@company.com", true),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DestinationsEmptyPreview() {
    OtpForwarderTheme {
        DestinationsContent(
            state    = DestinationsState(isLoading = false),
            onIntent = {},
        )
    }
}
