package com.samsung.android.otpforwarder.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import com.samsung.android.otpforwarder.core.model.DestinationType
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { effect ->
        when (effect) {
            SettingsSideEffect.GoBack                 -> onNavigateBack()
            SettingsSideEffect.LaunchExportFilePicker -> { /* TODO M5: file picker */ }
            SettingsSideEffect.LaunchImportFilePicker -> { /* TODO M5: file picker */ }
            is SettingsSideEffect.ShowSnackbar        -> { /* TODO M5: snackbar host */ }
        }
    }

    SettingsContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        SettingsTopBar(onBack = { onIntent(SettingsIntent.NavigateBack) })

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Forwarding ────────────────────────────────────────────────────
            item {
                SectionHeader("Forwarding")
                SettingsGroup {
                    SwitchRow(
                        icon    = Icons.Rounded.ToggleOn,
                        title   = "Forwarding",
                        subtitle = if (state.isForwardingEnabled) "All OTPs are being forwarded" else "All forwarding paused",
                        checked = state.isForwardingEnabled,
                        onToggle = { onIntent(SettingsIntent.ToggleForwarding) },
                    )
                    ActionRow(
                        icon     = Icons.Rounded.Sms,
                        title    = "Default SMS destination",
                        subtitle = state.defaultPhoneNumber.ifBlank { "Not set" },
                        onClick  = { onIntent(SettingsIntent.ShowPhoneNumberDialog) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    ActionRow(
                        icon     = Icons.Rounded.Email,
                        title    = "Default email destination",
                        subtitle = state.defaultEmailAddress.ifBlank { "Not set" },
                        onClick  = { onIntent(SettingsIntent.ShowEmailDialog) },
                    )
                }
            }

            // ── Security ──────────────────────────────────────────────────────
            item {
                SectionHeader("Security")
                SettingsGroup {
                    SwitchRow(
                        icon     = Icons.Rounded.Fingerprint,
                        title    = "Biometric lock",
                        subtitle = "Require fingerprint or PIN to open app",
                        checked  = state.isBiometricLockEnabled,
                        onToggle = { onIntent(SettingsIntent.ToggleBiometricLock) },
                    )
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            item {
                SectionHeader("Notifications")
                SettingsGroup {
                    SwitchRow(
                        icon     = Icons.Rounded.Notifications,
                        title    = "Forwarding notifications",
                        subtitle = "Show a notification when an OTP is forwarded",
                        checked  = state.notificationsEnabled,
                        onToggle = { onIntent(SettingsIntent.ToggleNotifications) },
                    )
                }
            }

            // ── Data ──────────────────────────────────────────────────────────
            item {
                SectionHeader("Data")
                SettingsGroup {
                    ActionRow(
                        icon     = Icons.Rounded.FileUpload,
                        title    = "Export config",
                        subtitle = "Save rules & settings as encrypted JSON",
                        onClick  = { onIntent(SettingsIntent.ExportConfig) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    ActionRow(
                        icon     = Icons.Rounded.FileDownload,
                        title    = "Import config",
                        subtitle = "Restore from a previously exported file",
                        onClick  = { onIntent(SettingsIntent.ImportConfig) },
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (state.showPhoneNumberDialog) {
        EditPhoneNumberDialog(
            initialValue = state.defaultPhoneNumber,
            onDismiss = { onIntent(SettingsIntent.HidePhoneNumberDialog) },
            onSave = { onIntent(SettingsIntent.SavePhoneNumber(it)) }
        )
    }

    if (state.showEmailDialog) {
        EditEmailDialog(
            initialValue = state.defaultEmailAddress,
            onDismiss = { onIntent(SettingsIntent.HideEmailDialog) },
            onSave = { onIntent(SettingsIntent.SaveEmail(it)) }
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text  = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp),
    )
}

// ── Settings group card ───────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
}

// ── Row types ─────────────────────────────────────────────────────────────────

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector        = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DelayRow(
    delay: Int,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Rounded.Timer,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Forwarding delay",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = if (delay == 0) "Immediate" else "${delay}s before forwarding",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = if (delay == 0) "0s" else "${delay}s",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Slider(
            value         = delay.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = 0f..30f,
            steps         = 29,
            modifier      = Modifier.padding(start = 40.dp, top = 4.dp),
        )
    }
}

@Composable
private fun DestinationRow(
    selectedDestinations: Set<DestinationType>,
    defaultPhoneNumber: String,
    defaultEmailAddress: String,
    onToggle: (DestinationType) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailAddressChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text  = "Forward OTPs to",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Applied when no specific rule matches",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected  = DestinationType.SMS in selectedDestinations,
                onClick   = { onToggle(DestinationType.SMS) },
                label     = { Text("SMS") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            FilterChip(
                selected  = DestinationType.EMAIL in selectedDestinations,
                onClick   = { onToggle(DestinationType.EMAIL) },
                label     = { Text("Email") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        if (DestinationType.SMS in selectedDestinations) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = defaultPhoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Default Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (DestinationType.EMAIL in selectedDestinations) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = defaultEmailAddress,
                onValueChange = onEmailAddressChange,
                label = { Text("Default Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsPreview() {
    OtpForwarderTheme {
        SettingsContent(
            state = SettingsState(
                isForwardingEnabled    = true,
                forwardingDelaySeconds = 5,
                defaultDestinations    = setOf(DestinationType.SMS, DestinationType.EMAIL),
                defaultPhoneNumber     = "1234567890",
                defaultEmailAddress    = "test@example.com",
                showPhoneNumberDialog  = false,
                showEmailDialog        = false,
                isBiometricLockEnabled = false,
                notificationsEnabled   = true,
                isLoading              = false,
            ),
            onIntent = {},
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun EditPhoneNumberDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialValue) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default SMS destination") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditEmailDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialValue) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default email destination") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
