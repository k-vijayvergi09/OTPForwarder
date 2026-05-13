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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun AddEmailDestinationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AddEmailDestinationViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddEmailSideEffect.GoBack -> onNavigateBack()
        }
    }

    AddEmailContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun AddEmailContent(
    state: AddEmailState,
    onIntent: (AddEmailIntent) -> Unit,
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
            IconButton(onClick = { onIntent(AddEmailIntent.NavigateBack) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text  = "Add email address",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(24.dp))

        // LABEL field
        FieldSection(label = "LABEL (OPTIONAL)") {
            OutlinedTextField(
                value         = state.label,
                onValueChange = { onIntent(AddEmailIntent.LabelChanged(it)) },
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

        // EMAIL ADDRESS field
        FieldSection(label = "EMAIL ADDRESS") {
            OutlinedTextField(
                value         = state.emailAddress,
                onValueChange = { onIntent(AddEmailIntent.AddressChanged(it)) },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("kartik@gmail.com") },
                singleLine    = true,
                isError       = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape         = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Enable immediately toggle row
        ToggleRow(
            title    = "Enable immediately",
            subtitle = "Start forwarding as soon as saved",
            checked  = state.enableImmediately,
            onToggle = { onIntent(AddEmailIntent.ToggleEnable) },
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))

        // Save button
        Button(
            onClick  = { onIntent(AddEmailIntent.Save) },
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

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddEmailPreview() {
    OtpForwarderTheme {
        AddEmailContent(
            state    = AddEmailState(label = "Personal", emailAddress = "kartik@gmail.com", enableImmediately = true),
            onIntent = {},
        )
    }
}
