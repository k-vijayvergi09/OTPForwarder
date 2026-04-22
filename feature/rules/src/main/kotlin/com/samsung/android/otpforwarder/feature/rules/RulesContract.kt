package com.samsung.android.otpforwarder.feature.rules

import com.samsung.android.otpforwarder.core.model.DestinationType

// ── UI model ──────────────────────────────────────────────────────────────────

data class RuleUiItem(
    val id: String,
    val name: String,
    /** Human-readable match description, e.g. "Matches: HDFC" or "Matches: all senders" */
    val matchLabel: String,
    val destinations: List<DestinationType>,
    val isEnabled: Boolean,
    val isCatchAll: Boolean,
)

// ── Sheet state for add / edit ────────────────────────────────────────────────

data class RuleSheetState(
    val isVisible: Boolean = false,
    val editingId: String? = null,     // null = creating new
    val name: String = "",
    val senderPattern: String = "",
    val selectedDestinations: Set<DestinationType> = setOf(DestinationType.EMAIL),
    val nameError: String? = null,
)

// ── MVI contract ──────────────────────────────────────────────────────────────

data class RulesState(
    val rules: List<RuleUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val sheet: RuleSheetState = RuleSheetState(),
)

sealed interface RulesIntent {
    data object AddRule                                       : RulesIntent
    data class  EditRule(val id: String)                     : RulesIntent
    data class  ToggleRule(val id: String)                   : RulesIntent
    data class  DeleteRule(val id: String)                   : RulesIntent
    // Sheet intents
    data class  SheetNameChanged(val value: String)          : RulesIntent
    data class  SheetPatternChanged(val value: String)       : RulesIntent
    data class  SheetToggleDestination(val type: DestinationType) : RulesIntent
    data object SheetSave                                    : RulesIntent
    data object SheetDismiss                                 : RulesIntent
}

sealed interface RulesSideEffect {
    data class ShowSnackbar(val message: String) : RulesSideEffect
}
