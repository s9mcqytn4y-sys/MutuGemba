package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.UserPreferenceKeys
import id.co.nierstyd.mutugemba.domain.UserPreferencesRepository

data class UserPreferencesState(
    val defaultInspectionKind: InspectionKind,
    val compactMode: Boolean,
    val showGuidance: Boolean,
)

class GetUserPreferencesUseCase(
    private val repository: UserPreferencesRepository,
) {
    fun execute(userId: Long): UserPreferencesState {
        val prefs = repository.getAll(userId)
        val kind =
            prefs[UserPreferenceKeys.DEFAULT_INSPECTION_KIND]
                ?.let { runCatching { InspectionKind.valueOf(it) }.getOrNull() }
                ?: InspectionKind.DEFECT
        val compactMode = prefs[UserPreferenceKeys.COMPACT_MODE]?.toBooleanStrictOrNull() ?: false
        val showGuidance = prefs[UserPreferenceKeys.SHOW_GUIDANCE]?.toBooleanStrictOrNull() ?: true

        return UserPreferencesState(
            defaultInspectionKind = kind,
            compactMode = compactMode,
            showGuidance = showGuidance,
        )
    }
}

class SaveUserPreferencesUseCase(
    private val repository: UserPreferencesRepository,
) {
    fun execute(
        userId: Long,
        preferences: UserPreferencesState,
    ) {
        repository.setPreference(
            userId = userId,
            key = UserPreferenceKeys.DEFAULT_INSPECTION_KIND,
            value = preferences.defaultInspectionKind.name,
        )
        repository.setPreference(
            userId = userId,
            key = UserPreferenceKeys.COMPACT_MODE,
            value = preferences.compactMode.toString(),
        )
        repository.setPreference(
            userId = userId,
            key = UserPreferenceKeys.SHOW_GUIDANCE,
            value = preferences.showGuidance.toString(),
        )
    }
}
