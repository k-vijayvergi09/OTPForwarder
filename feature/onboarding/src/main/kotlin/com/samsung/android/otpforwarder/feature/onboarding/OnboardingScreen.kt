package com.samsung.android.otpforwarder.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    // Permission launchers
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val receiveSms = grants[Manifest.permission.RECEIVE_SMS] ?: false
        if (receiveSms) viewModel.onIntent(OnboardingIntent.SmsGranted)
        else            viewModel.onIntent(OnboardingIntent.SmsDenied(permanently = false))
    }

    val notificationLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) viewModel.onIntent(OnboardingIntent.NotificationGranted)
            else         viewModel.onIntent(OnboardingIntent.NotificationDenied)
        }
    } else null

    viewModel.collectSideEffect { effect ->
        when (effect) {
            OnboardingSideEffect.LaunchSmsPermission -> {
                smsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                    )
                )
            }
            OnboardingSideEffect.LaunchNotificationPermission -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.onIntent(OnboardingIntent.NotificationGranted)
                }
            }
            OnboardingSideEffect.LaunchBatteryOptSettings -> {
                // Handled by the UI below via ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            }
            OnboardingSideEffect.NavigateToHome -> onNavigateToHome()
        }
    }

    OnboardingContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun OnboardingContent(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Step indicator dots
        StepIndicator(
            totalSteps   = OnboardingStep.entries.size,
            currentIndex = state.currentStep.index,
            modifier     = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )

        // Animated step content
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                val forward = targetState.index > initialState.index
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
            label = "onboarding_step",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.SMS_PERMISSION -> SmsPermissionStep(denied = state.smsDenied)
                OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep()
                OnboardingStep.BATTERY_OPTIMIZATION -> BatteryOptStep()
                OnboardingStep.DONE -> DoneStep()
            }
        }

        // Bottom actions
        OnboardingActions(
            step     = state.currentStep,
            smsDenied = state.smsDenied,
            onIntent = onIntent,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
    }
}

// ── Step indicator ────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(
    totalSteps: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val active = index == currentIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                    .size(if (active) 24.dp else 8.dp, 8.dp)
            )
            if (index < totalSteps - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

// ── Step composables ──────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep() {
    StepScaffold(
        icon       = Icons.Rounded.Forward,
        iconBg     = Color(0xFFDCE5FF),
        iconTint   = Color(0xFF0A1F6B),
        title      = "Welcome to OTP Forwarder",
        body       = "Automatically forward OTPs from your main phone to any number or email — perfect for multi-device users and frequent travelers.",
    )
}

@Composable
private fun SmsPermissionStep(denied: Boolean) {
    StepScaffold(
        icon     = Icons.Rounded.Sms,
        iconBg   = MaterialTheme.colorScheme.secondaryContainer,
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        title    = "Allow SMS access",
        body     = if (denied)
            "SMS permission was denied. The app cannot detect OTPs without it. Please grant it to continue."
        else
            "OTP Forwarder needs permission to read incoming SMS messages so it can detect and forward OTPs.",
    )
}

@Composable
private fun NotificationPermissionStep() {
    StepScaffold(
        icon     = Icons.Rounded.Notifications,
        iconBg   = MaterialTheme.colorScheme.tertiaryContainer,
        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        title    = "Stay in the loop",
        body     = "Allow notifications so you can see a quick summary every time an OTP is forwarded. You can turn this off any time in Settings.",
    )
}

@Composable
private fun BatteryOptStep() {
    StepScaffold(
        icon     = Icons.Rounded.Battery5Bar,
        iconBg   = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.onErrorContainer,
        title    = "Disable battery optimisation",
        body     = "Some Android manufacturers aggressively kill background apps. Exempting OTP Forwarder ensures your OTPs are always forwarded, even when the screen is off.",
    )
}

@Composable
private fun DoneStep() {
    StepScaffold(
        icon     = Icons.Rounded.CheckCircle,
        iconBg   = Color(0xFFCEF5E6),
        iconTint = Color(0xFF0A6B3A),
        title    = "You're all set!",
        body     = "OTP Forwarder is ready. Your OTPs will be detected and forwarded automatically. Configure forwarding rules any time from the Rules tab.",
    )
}

@Composable
private fun StepScaffold(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    body: String,
) {
    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center,
    ) {
        Box(
            modifier         = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text      = title,
            style     = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = body,
            style     = MaterialTheme.typography.bodyLarge.copy(
                lineHeight    = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun OnboardingActions(
    step: OnboardingStep,
    smsDenied: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (step) {
            OnboardingStep.WELCOME -> {
                Button(
                    onClick  = { onIntent(OnboardingIntent.Next) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Get started") }
            }

            OnboardingStep.SMS_PERMISSION -> {
                Button(
                    onClick  = { onIntent(OnboardingIntent.RequestSmsPermission) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (smsDenied) "Retry permission" else "Grant SMS access") }
                if (smsDenied) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = { onIntent(OnboardingIntent.Next) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Skip (app won't work correctly)") }
                }
            }

            OnboardingStep.NOTIFICATION_PERMISSION -> {
                Button(
                    onClick  = { onIntent(OnboardingIntent.RequestNotificationPermission) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Allow notifications") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = { onIntent(OnboardingIntent.NotificationDenied) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip") }
            }

            OnboardingStep.BATTERY_OPTIMIZATION -> {
                Button(
                    onClick  = { onIntent(OnboardingIntent.RequestBatteryOptExemption) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Disable battery optimisation") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = { onIntent(OnboardingIntent.Next) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip") }
            }

            OnboardingStep.DONE -> {
                Button(
                    onClick  = { onIntent(OnboardingIntent.Finish) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Go to app") }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingWelcomePreview() {
    OtpForwarderTheme {
        OnboardingContent(
            state    = OnboardingState(currentStep = OnboardingStep.WELCOME),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingDonePreview() {
    OtpForwarderTheme {
        OnboardingContent(
            state    = OnboardingState(currentStep = OnboardingStep.DONE),
            onIntent = {},
        )
    }
}
