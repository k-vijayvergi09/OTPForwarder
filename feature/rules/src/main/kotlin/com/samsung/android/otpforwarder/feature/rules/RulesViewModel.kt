package com.samsung.android.otpforwarder.feature.rules

import androidx.lifecycle.ViewModel
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRule
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    // RulesRepository will replace in-memory list in M2 when Room is wired.
) : ViewModel(), ContainerHost<RulesState, RulesSideEffect> {

    override val container = container<RulesState, RulesSideEffect>(
        initialState = RulesState(
            isLoading = false,
            rules = listOf(
                ForwardingRule(
                    id             = "default",
                    name           = "All senders",
                    senderPattern  = "",
                    destinations   = listOf(DestinationType.EMAIL),
                    isEnabled      = true,
                    priority       = 999,
                ).toUiItem(),
            ),
        )
    )

    fun onIntent(intent: RulesIntent) = when (intent) {
        RulesIntent.AddRule -> intent {
            reduce {
                state.copy(
                    sheet = RuleSheetState(
                        isVisible            = true,
                        editingId            = null,
                        name                 = "",
                        senderPattern        = "",
                        selectedDestinations = setOf(DestinationType.EMAIL),
                    )
                )
            }
        }

        is RulesIntent.EditRule -> intent {
            val rule = state.rules.firstOrNull { it.id == intent.id } ?: return@intent
            reduce {
                state.copy(
                    sheet = RuleSheetState(
                        isVisible            = true,
                        editingId            = rule.id,
                        name                 = rule.name,
                        senderPattern        = if (rule.isCatchAll) "" else rule.matchLabel.removePrefix("Matches: "),
                        selectedDestinations = rule.destinations.toSet(),
                    )
                )
            }
        }

        is RulesIntent.ToggleRule -> intent {
            reduce {
                state.copy(
                    rules = state.rules.map { r ->
                        if (r.id == intent.id) r.copy(isEnabled = !r.isEnabled) else r
                    }
                )
            }
        }

        is RulesIntent.DeleteRule -> intent {
            reduce { state.copy(rules = state.rules.filterNot { it.id == intent.id }) }
            postSideEffect(RulesSideEffect.ShowSnackbar("Rule deleted"))
        }

        is RulesIntent.SheetNameChanged -> intent {
            reduce { state.copy(sheet = state.sheet.copy(name = intent.value, nameError = null)) }
        }

        is RulesIntent.SheetPatternChanged -> intent {
            reduce { state.copy(sheet = state.sheet.copy(senderPattern = intent.value)) }
        }

        is RulesIntent.SheetToggleDestination -> intent {
            reduce {
                val current = state.sheet.selectedDestinations.toMutableSet()
                if (intent.type in current) {
                    if (current.size > 1) current.remove(intent.type)
                } else {
                    current.add(intent.type)
                }
                state.copy(sheet = state.sheet.copy(selectedDestinations = current))
            }
        }

        RulesIntent.SheetSave -> intent {
            val sheet = state.sheet
            if (sheet.name.isBlank()) {
                reduce { state.copy(sheet = sheet.copy(nameError = "Name is required")) }
                return@intent
            }
            val newRule = ForwardingRule(
                id            = sheet.editingId ?: UUID.randomUUID().toString(),
                name          = sheet.name.trim(),
                senderPattern = sheet.senderPattern.trim(),
                destinations  = sheet.selectedDestinations.toList(),
                isEnabled     = true,
            ).toUiItem()

            reduce {
                val updatedRules = if (sheet.editingId != null) {
                    state.rules.map { if (it.id == sheet.editingId) newRule else it }
                } else {
                    state.rules + newRule
                }
                state.copy(
                    rules = updatedRules,
                    sheet = RuleSheetState(),
                )
            }
        }

        RulesIntent.SheetDismiss -> intent {
            reduce { state.copy(sheet = RuleSheetState()) }
        }
    }
}

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun ForwardingRule.toUiItem() = RuleUiItem(
    id          = id,
    name        = name,
    matchLabel  = if (isCatchAll) "Matches: all senders" else "Matches: $senderPattern",
    destinations = destinations,
    isEnabled   = isEnabled,
    isCatchAll  = isCatchAll,
)
