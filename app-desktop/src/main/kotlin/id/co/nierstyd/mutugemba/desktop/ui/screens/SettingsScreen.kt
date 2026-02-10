package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppRadioGroup
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.usecase.BackupDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.RestoreDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import kotlinx.coroutines.delay

data class SettingsScreenDependencies(
    val getAllowDuplicateInspection: GetAllowDuplicateInspectionUseCase,
    val setAllowDuplicateInspection: SetAllowDuplicateInspectionUseCase,
    val resetData: ResetDataUseCase,
    val getLines: GetLinesUseCase,
    val getDevQcLine: GetDevQcLineUseCase,
    val setDevQcLine: SetDevQcLineUseCase,
    val backupDatabase: BackupDatabaseUseCase,
    val restoreDatabase: RestoreDatabaseUseCase,
    val onResetCompleted: () -> Unit,
    val onRestoreCompleted: () -> Unit,
)

@Composable
fun SettingsScreen(dependencies: SettingsScreenDependencies) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var allowDuplicate by remember { mutableStateOf(dependencies.getAllowDuplicateInspection.execute()) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }
    var lines by remember { mutableStateOf<List<Line>>(emptyList()) }
    var selectedDevLineId by remember { mutableStateOf<Long?>(dependencies.getDevQcLine.execute()) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(3000)
            feedback = null
        }
    }

    LaunchedEffect(Unit) {
        lines = dependencies.getLines.execute()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.Settings.Title,
            subtitle = AppStrings.Settings.Subtitle,
        )
        feedback?.let { StatusBanner(feedback = it) }

        SettingsSectionCard(
            title = AppStrings.Settings.RulesTitle,
            subtitle = AppStrings.Settings.RulesSubtitle,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppBadge(
                    text = AppStrings.Settings.RulesBadge,
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralTextMuted,
                )
                Text(
                    text = AppStrings.Settings.RulesDescription,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }
        }

        SettingsSectionCard(
            title = AppStrings.Settings.SimTitle,
            subtitle = AppStrings.Settings.SimSubtitle,
        ) {
            val options = lines.map { DropdownOption(it.id, it.name) }
            val selectedOption =
                selectedDevLineId
                    ?.let { id -> lines.firstOrNull { it.id == id } }
                    ?.let { line -> DropdownOption(line.id, line.name) }
            AppRadioGroup(
                label = AppStrings.Settings.LineQcLabel,
                options = options,
                selectedId = selectedOption?.id,
                onSelected = { option ->
                    selectedDevLineId = option.id
                    dependencies.setDevQcLine.execute(option.id)
                    feedback =
                        UserFeedback(
                            FeedbackType.SUCCESS,
                            AppStrings.Feedback.lineQcUpdated(option.label),
                        )
                },
                helperText = AppStrings.Settings.LineQcHint,
                maxHeight = 140.dp,
            )
            SecondaryButton(
                text = AppStrings.Actions.UseManualSelection,
                onClick = {
                    selectedDevLineId = null
                    dependencies.setDevQcLine.execute(null)
                    feedback =
                        UserFeedback(
                            FeedbackType.SUCCESS,
                            AppStrings.Feedback.lineQcManual,
                        )
                },
                enabled = selectedDevLineId != null,
            )
            Divider(color = NeutralBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = AppStrings.Settings.DuplicateTitle, style = MaterialTheme.typography.body1)
                    Text(
                        text = AppStrings.Settings.DuplicateSubtitle,
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Switch(
                    checked = allowDuplicate,
                    onCheckedChange = { checked ->
                        allowDuplicate = checked
                        dependencies.setAllowDuplicateInspection.execute(checked)
                        feedback =
                            UserFeedback(
                                FeedbackType.SUCCESS,
                                if (checked) {
                                    AppStrings.Feedback.DuplicateAllowed
                                } else {
                                    AppStrings.Feedback.DuplicateBlocked
                                },
                            )
                    },
                )
            }
        }

        SettingsSectionCard(
            title = AppStrings.Settings.MaintenanceTitle,
            subtitle = AppStrings.Settings.MaintenanceSubtitle,
        ) {
            Text(
                text = AppStrings.Settings.MaintenanceBody,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            PrimaryButton(
                text = AppStrings.Actions.ResetData,
                onClick = { showResetDialog = true },
            )
            Divider(color = NeutralBorder, thickness = 1.dp)
            Text(
                text = AppStrings.Settings.BackupHint,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Text(
                text = AppStrings.Settings.RestoreHint,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(
                    text = AppStrings.Actions.Backup,
                    onClick = {
                        val result = dependencies.backupDatabase.execute()
                        feedback =
                            if (result.isSuccess) {
                                UserFeedback(FeedbackType.SUCCESS, AppStrings.Feedback.BackupSuccess)
                            } else {
                                UserFeedback(FeedbackType.ERROR, AppStrings.Feedback.BackupFailed)
                            }
                    },
                )
                SecondaryButton(
                    text = AppStrings.Actions.Restore,
                    onClick = { showRestoreDialog = true },
                )
            }
        }
    }

    ConfirmDialog(
        open = showResetDialog,
        title = AppStrings.Settings.ResetDialogTitle,
        message = AppStrings.Settings.ResetDialogMessage,
        confirmText = AppStrings.Actions.Reset,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = {
            showResetDialog = false
            val success = dependencies.resetData.execute()
            feedback =
                if (success) {
                    dependencies.onResetCompleted()
                    UserFeedback(FeedbackType.SUCCESS, AppStrings.Feedback.ResetSuccess)
                } else {
                    UserFeedback(FeedbackType.ERROR, AppStrings.Feedback.ResetFailed)
                }
        },
        onDismiss = { showResetDialog = false },
    )

    ConfirmDialog(
        open = showRestoreDialog,
        title = AppStrings.Actions.Restore,
        message = AppStrings.Settings.RestoreHint,
        confirmText = AppStrings.Actions.Restore,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = {
            showRestoreDialog = false
            val result = dependencies.restoreDatabase.execute()
            feedback =
                if (result.isSuccess) {
                    dependencies.onRestoreCompleted()
                    UserFeedback(FeedbackType.SUCCESS, AppStrings.Settings.RestoreSuccess)
                } else {
                    UserFeedback(FeedbackType.ERROR, AppStrings.Feedback.RestoreFailed)
                }
        },
        onDismiss = { showRestoreDialog = false },
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = title, style = MaterialTheme.typography.subtitle1)
                Text(text = subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
            }
            content()
        }
    }
}
