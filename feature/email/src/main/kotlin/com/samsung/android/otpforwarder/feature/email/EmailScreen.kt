package com.samsung.android.otpforwarder.feature.email

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun EmailScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: EmailSetupViewModel = hiltViewModel(),
) {
    val state        = viewModel.collectAsState().value
    val snackbarHost = remember { SnackbarHostState() }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            EmailSetupSideEffect.GoBack          -> onNavigateBack()
            is EmailSetupSideEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EmailSetupContent(state = state, onIntent = viewModel::onIntent)
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun EmailSetupContent(
    state: EmailSetupState,
    onIntent: (EmailSetupIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        EmailSetupTopBar(onBack = { onIntent(EmailSetupIntent.NavigateBack) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            HeroSection()

            Spacer(Modifier.height(28.dp))

            // ── Configured banner ─────────────────────────────────────────────
            AnimatedVisibility(visible = state.isConfigured, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    ConfiguredBanner(address = state.address)
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── Test-result banner ────────────────────────────────────────────
            AnimatedVisibility(visible = state.testResult != null, enter = fadeIn(), exit = fadeOut()) {
                state.testResult?.let {
                    Column {
                        TestResultBanner(result = it)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // ── Input fields ──────────────────────────────────────────────────
            OutlinedTextField(
                value           = state.address,
                onValueChange   = { onIntent(EmailSetupIntent.UpdateAddress(it)) },
                label           = { Text("Gmail address") },
                placeholder     = { Text("you@gmail.com") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                leadingIcon     = { Icon(Icons.Rounded.Email, contentDescription = null) },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value                = state.appPassword,
                onValueChange        = { onIntent(EmailSetupIntent.UpdateAppPassword(it)) },
                label                = { Text("App Password") },
                placeholder          = { Text("xxxx xxxx xxxx xxxx") },
                singleLine           = true,
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onIntent(EmailSetupIntent.TogglePasswordVisibility) }) {
                        Icon(
                            imageVector = if (state.isPasswordVisible)
                                Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (state.isPasswordVisible)
                                "Hide password" else "Show password",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(8.dp))
            AppPasswordHint()
            Spacer(Modifier.height(24.dp))

            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Button(
                onClick  = { onIntent(EmailSetupIntent.TestAndSave) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Testing connection…")
                } else {
                    Text(if (state.isConfigured) "Re-test & Save" else "Test & Save")
                }
            }

            if (state.isConfigured) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = { onIntent(EmailSetupIntent.ClearCredentials) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove Gmail account")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun EmailSetupTopBar(onBack: () -> Unit) {
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
            text  = "Gmail Setup",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection() {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.Email,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier           = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Configure outbound Gmail",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "OTPs will be forwarded from this Gmail account to your configured email destinations.",
            style     = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Configured banner ─────────────────────────────────────────────────────────

@Composable
private fun ConfiguredBanner(address: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFCEF5E6))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint               = Color(0xFF0A6B3A),
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text  = "Gmail account configured",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF0A6B3A),
            )
            Text(
                text  = address,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF0A6B3A).copy(alpha = 0.8f),
            )
        }
    }
}

// ── Test result banner ────────────────────────────────────────────────────────

@Composable
private fun TestResultBanner(result: TestResult) {
    val isSuccess = result is TestResult.Success
    val bg    = if (isSuccess) Color(0xFFCEF5E6) else MaterialTheme.colorScheme.errorContainer
    val tint  = if (isSuccess) Color(0xFF0A6B3A) else MaterialTheme.colorScheme.onErrorContainer
    val text  = if (isSuccess)
        "Connection successful! A test email was sent to your address."
    else
        (result as TestResult.Failure).reason

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector        = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = tint,
        )
    }
}

// ── App Password hint ─────────────────────────────────────────────────────────

@Composable
private fun AppPasswordHint() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector        = Icons.Rounded.Info,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "Use an App Password, not your Google account password. " +
                    "Generate one at myaccount.google.com → Security → App Passwords.",
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EmailSetupEmptyPreview() {
    EmailSetupContent(state = EmailSetupState(), onIntent = {})
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EmailSetupConfiguredPreview() {
    EmailSetupContent(
        state = EmailSetupState(
            address      = "kartik@gmail.com",
            appPassword  = "abcdabcdabcdabcd",
            isConfigured = true,
            testResult   = TestResult.Success,
        ),
        onIntent = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun EmailSetupFailurePreview() {
    EmailSetupContent(
        state = EmailSetupState(
            address     = "kartik@gmail.com",
            appPassword = "wrongpassword",
            testResult  = TestResult.Failure("Authentication failed — check your Gmail address and App Password"),
        ),
        onIntent = {},
    )
}
