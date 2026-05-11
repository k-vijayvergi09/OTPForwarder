package com.samsung.android.otpforwarder.feature.rules

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import com.samsung.android.otpforwarder.core.model.DestinationType
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// ── Filter tabs ───────────────────────────────────────────────────────────────

private enum class RulesFilter(val label: String) {
    ALL("All"),
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    RECENTLY_USED("Recently used"),
}

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun RulesScreen(
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value

    viewModel.collectSideEffect { /* snackbar in M2 */ }

    RulesContent(state = state, onIntent = viewModel::onIntent)
}

// ── Stateless content ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RulesContent(
    state: RulesState,
    onIntent: (RulesIntent) -> Unit,
) {
    var activeFilter by remember { mutableStateOf(RulesFilter.ALL) }

    val filteredRules = when (activeFilter) {
        RulesFilter.ALL           -> state.rules
        RulesFilter.ENABLED       -> state.rules.filter { it.isEnabled }
        RulesFilter.DISABLED      -> state.rules.filter { !it.isEnabled }
        RulesFilter.RECENTLY_USED -> state.rules // TODO: sort by last-used timestamp
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onIntent(RulesIntent.AddRule) },
                icon    = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text    = { Text("New rule") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            RulesTopBar()

            if (state.rules.isEmpty() && !state.isLoading) {
                EmptyRulesState(
                    onCreateFirstRule = { onIntent(RulesIntent.AddRule) },
                )
            } else {
                // Filter chips
                LazyRow(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(RulesFilter.entries) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick  = { activeFilter = filter },
                            label    = { Text(filter.label) },
                        )
                    }
                }

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    items(filteredRules, key = { it.id }) { rule ->
                        RuleCard(
                            item     = rule,
                            onToggle = { onIntent(RulesIntent.ToggleRule(rule.id)) },
                            onEdit   = { onIntent(RulesIntent.EditRule(rule.id)) },
                            onDelete = { onIntent(RulesIntent.DeleteRule(rule.id)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    item { Spacer(Modifier.height(88.dp)) } // FAB clearance
                }
            }
        }

        // ── Add / edit bottom sheet ───────────────────────────────────────────

        if (state.sheet.isVisible) {
            ModalBottomSheet(
                onDismissRequest = { onIntent(RulesIntent.SheetDismiss) },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                RuleEditorSheet(sheet = state.sheet, onIntent = onIntent)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun RulesTopBar() {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = "Rules",
            style    = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { /* TODO: search */ }) {
            Icon(Icons.Rounded.Search, contentDescription = "Search rules")
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyRulesState(onCreateFirstRule: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Illustration circle
        Box(
            modifier         = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.RuleFolder,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier           = Modifier.size(72.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text      = "No rules yet",
            style     = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "Create forwarding rules to route OTPs from specific senders to SMS or email. All other OTPs use your default rule.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onCreateFirstRule,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create first rule")
        }
    }
}

// ── Rule card ─────────────────────────────────────────────────────────────────

@Composable
private fun RuleCard(
    item: RuleUiItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val destinationLabel = item.destinations.joinToString(" · ") { dest ->
        when (dest) {
            DestinationType.SMS   -> "SMS → ••••"
            DestinationType.EMAIL -> "Email → ••••"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.RuleFolder,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier           = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (item.isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = item.matchLabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (destinationLabel.isNotEmpty()) {
                    Text(
                        text  = destinationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Switch(
                checked         = item.isEnabled,
                onCheckedChange = { onToggle() },
            )
        }

        // Edit / delete — hide for catch-all
        AnimatedVisibility(!item.isCatchAll) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick  = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete rule?") },
            text  = { Text("\"${item.name}\" will be removed and OTPs from this sender will fall through to the default rule.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Rule editor sheet ─────────────────────────────────────────────────────────

@Composable
private fun RuleEditorSheet(
    sheet: RuleSheetState,
    onIntent: (RulesIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text  = if (sheet.editingId == null) "New rule" else "Edit rule",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value          = sheet.name,
            onValueChange  = { onIntent(RulesIntent.SheetNameChanged(it)) },
            label          = { Text("Rule name") },
            placeholder    = { Text("e.g. HDFC Bank") },
            isError        = sheet.nameError != null,
            supportingText = sheet.nameError?.let { { Text(it) } },
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = sheet.senderPattern,
            onValueChange = { onIntent(RulesIntent.SheetPatternChanged(it)) },
            label         = { Text("Sender pattern") },
            placeholder   = { Text("e.g. HDFC or +91989… (leave blank to match all)") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text  = "Forward to",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected    = DestinationType.SMS in sheet.selectedDestinations,
                onClick     = { onIntent(RulesIntent.SheetToggleDestination(DestinationType.SMS)) },
                label       = { Text("SMS") },
            )
            FilterChip(
                selected    = DestinationType.EMAIL in sheet.selectedDestinations,
                onClick     = { onIntent(RulesIntent.SheetToggleDestination(DestinationType.EMAIL)) },
                label       = { Text("Email") },
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onIntent(RulesIntent.SheetDismiss) }) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onIntent(RulesIntent.SheetSave) }) {
                Text(
                    text  = if (sheet.editingId == null) "Add rule" else "Save",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RulesPreview() {
    OtpForwarderTheme {
        RulesContent(
            state = RulesState(
                isLoading = false,
                rules = listOf(
                    RuleUiItem("1", "HDFC Bank",   "Sender matches 'HDFCBK' or 'HDFC-'", listOf(DestinationType.SMS, DestinationType.EMAIL), true,  false),
                    RuleUiItem("2", "All senders", "Matches: all senders",                listOf(DestinationType.EMAIL),                       true,  true),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Empty state")
@Composable
private fun RulesEmptyPreview() {
    OtpForwarderTheme {
        RulesContent(
            state    = RulesState(isLoading = false, rules = emptyList()),
            onIntent = {},
        )
    }
}
