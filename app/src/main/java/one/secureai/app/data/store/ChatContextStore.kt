package one.secureai.app.data.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot handoff for "tap a Note/Library photo, land in a fresh chat with
 * it as context" — mirrors iOS's AppCoordinator.pendingNote/pendingLibraryItem,
 * but as a StateFlow since Android has no coordinator singleton, following the
 * same pattern ProjectStore.activeProject already uses for cross-screen state.
 */
object ChatContextStore {
    private val _pendingPrompt = MutableStateFlow<String?>(null)
    val pendingPrompt: StateFlow<String?> = _pendingPrompt.asStateFlow()

    fun set(systemPrompt: String) {
        _pendingPrompt.value = systemPrompt
    }

    /** Reads and clears the pending context — call once when Chat appears. */
    fun consume(): String? {
        val value = _pendingPrompt.value
        _pendingPrompt.value = null
        return value
    }
}
