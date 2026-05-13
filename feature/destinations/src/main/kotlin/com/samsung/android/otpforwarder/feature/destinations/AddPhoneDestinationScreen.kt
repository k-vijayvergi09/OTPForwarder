package com.samsung.android.otpforwarder.feature.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun AddPhoneDestinationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AddPhoneDestinationViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddPhoneSideEffect.GoBack -> onNavigateBack()
        }
    }

    AddPhoneContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun AddPhoneContent(
    state: AddPhoneState,
    onIntent: (AddPhoneIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onIntent(AddPhoneIntent.NavigateBack) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text  = "Add phone number",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(24.dp))

        // LABEL field
        FieldSection(label = "LABEL (OPTIONAL)") {
            OutlinedTextField(
                value         = state.label,
                onValueChange = { onIntent(AddPhoneIntent.LabelChanged(it)) },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. Personal") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
            )
        }

        Spacer(Modifier.height(20.dp))

        // PHONE NUMBER field
        FieldSection(label = "PHONE NUMBER") {
            OutlinedTextField(
                value         = state.phoneNumber,
                onValueChange = { onIntent(AddPhoneIntent.NumberChanged(it)) },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("+91 98765 43210") },
                singleLine    = true,
                isError       = state.phoneError != null,
                supportingText = state.phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape         = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Enable immediately toggle row
        ToggleRow(
            title    = "Enable immediately",
            subtitle = "Start forwarding as soon as saved",
            checked  = state.enableImmediately,
            onToggle = { onIntent(AddPhoneIntent.ToggleEnable) },
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))

        // Save button
        Button(
            onClick  = { onIntent(AddPhoneIntent.Save) },
            enabled  = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            shape    = RoundedCornerShape(50),
        ) {
            Text(
                text  = "Save",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
internal fun FieldSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddPhonePreview() {
    OtpForwarderTheme {
        AddPhoneContent(
            state    = AddPhoneState(label = "Personal", phoneNumber = "+91 98765 43210", enableImmediately = true),
            onIntent = {},
        )
    }
}
