package com.samsung.android.otpforwarder.feature.home

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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun HomeScreen(
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { effect ->
        when (effect) {
            HomeSideEffect.GoToLogs     -> onNavigateToLogs()
            HomeSideEffect.GoToSettings -> onNavigateToSettings()
        }
    }

    HomeContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
internal fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        HomeTopBar(
            onNotifications = { /* TODO M5 */ },
            onSettings      = { onIntent(HomeIntent.NavigateToSettings) },
        )

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                StatusCard(
                    isEnabled        = state.isForwardingEnabled,
                    activeRulesCount = state.activeRulesCount,
                    todayCount       = state.todayCount,
                    onToggle         = { onIntent(HomeIntent.ToggleForwarding) },
                    modifier         = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                TodaySectionHeader(
                    onSeeAll = { onIntent(HomeIntent.NavigateToLogs) },
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(4.dp))
            }

            if (state.recentItems.isEmpty() && !state.isLoading) {
                item { EmptyTodayState(Modifier.padding(horizontal = 24.dp)) }
            } else {
                items(state.recentItems, key = { it.id }) { item ->
                    OtpRow(item = item, modifier = Modifier.padding(horizontal = 24.dp))
                    if (state.recentItems.last() != item) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 24.dp),
                            thickness = 1.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(onNotifications: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = "Home",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNotifications) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

// ── Status card ───────────────────────────────────────────────────────────────

private val StatusCardGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFFDCE5FF),
        0.73f to Color(0xFFCEF5E6),
    ),
)
private val BrandBlue = Color(0xFF0A1F6B)

@Composable
private fun StatusCard(
    isEnabled: Boolean,
    activeRulesCount: Int,
    todayCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StatusCardGradient)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column {
            Text(
                text  = if (isEnabled) "ACTIVE" else "PAUSED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                ),
                color = BrandBlue,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = if (isEnabled) "Forwarding is on" else "Forwarding is off",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = BrandBlue,
            )
            Spacer(Modifier.height(4.dp))
            val subtitle = buildString {
                if (activeRulesCount > 0) append("$activeRulesCount rules watching · ")
                append("$todayCount OTPs forwarded today")
            }
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandBlue.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "Global forwarding",
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color    = BrandBlue,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked         = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor   = Color(0xFF2E5BFF),
                        checkedThumbColor   = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                    ),
                )
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun TodaySectionHeader(onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text  = "TODAY",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSeeAll) {
            Text(
                text  = "See all",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── OTP row item ──────────────────────────────────────────────────────────────

@Composable
private fun OtpRow(item: OtpRowUiItem, modifier: Modifier = Modifier) {
    val ext = OtpTheme.extendedColors
    val colors = when (item.status) {
        ForwardingStatus.FORWARDED    -> StatusColors(
            iconBg    = Color(0xFFDCE5FF), iconTint = BrandBlue,
            badgeBg   = ext.successContainer, badgeText = ext.onSuccessContainer,
        )
        ForwardingStatus.PENDING,
        ForwardingStatus.RETRY_QUEUED -> StatusColors(
            iconBg    = ext.warningContainer, iconTint = ext.onWarningContainer,
            badgeBg   = ext.warningContainer, badgeText = ext.onWarningContainer,
        )
        ForwardingStatus.FAILED       -> StatusColors(
            iconBg    = MaterialTheme.colorScheme.errorContainer,
            iconTint  = MaterialTheme.colorScheme.onErrorContainer,
            badgeBg   = MaterialTheme.colorScheme.errorContainer,
            badgeText = MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Row(
        modifier          = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.Sms,
                contentDescription = null,
                tint               = colors.iconTint,
                modifier           = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text     = item.sender,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = item.maskedOtp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 3.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier         = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(colors.badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = item.status.badgeLabel(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = colors.badgeText,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyTodayState(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "No OTPs forwarded today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class StatusColors(
    val iconBg: Color, val iconTint: Color,
    val badgeBg: Color, val badgeText: Color,
)

private fun ForwardingStatus.badgeLabel() = when (this) {
    ForwardingStatus.FORWARDED    -> "Forwarded"
    ForwardingStatus.PENDING      -> "Pending"
    ForwardingStatus.RETRY_QUEUED -> "Pending"
    ForwardingStatus.FAILED       -> "Failed"
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    OtpForwarderTheme {
        HomeContent(
            state = HomeState(
                isForwardingEnabled = true,
                activeRulesCount    = 3,
                todayCount          = 24,
                recentItems = listOf(
                    OtpRowUiItem("1", "HDFC Bank",  "••• •••", "Forwarded · SMS + Email", ForwardingStatus.FORWARDED,    "2m ago"),
                    OtpRowUiItem("2", "Amazon",     "••• •••", "Retry queued",            ForwardingStatus.RETRY_QUEUED, "18m ago"),
                    OtpRowUiItem("3", "MakeMyTrip", "••• •••", "Network error",           ForwardingStatus.FAILED,       "1h ago"),
                ),
            ),
            onIntent = {},
        )
    }
}
