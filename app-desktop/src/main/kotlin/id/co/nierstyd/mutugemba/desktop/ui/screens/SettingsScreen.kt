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
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevDemoModeUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevDummyDataUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDemoModeUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDummyDataUseCase
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
    val getDevDemoMode: GetDevDemoModeUseCase,
    val setDevDemoMode: SetDevDemoModeUseCase,
    val getDevDummyData: GetDevDummyDataUseCase,
    val setDevDummyData: SetDevDummyDataUseCase,
    val onResetCompleted: () -> Unit,
)

@Composable
fun SettingsScreen(dependencies: SettingsScreenDependencies) {
    var showResetDialog by remember { mutableStateOf(false) }
    var allowDuplicate by remember { mutableStateOf(dependencies.getAllowDuplicateInspection.execute()) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }
    var lines by remember { mutableStateOf<List<Line>>(emptyList()) }
    var selectedDevLineId by remember { mutableStateOf<Long?>(dependencies.getDevQcLine.execute()) }
    var demoMode by remember { mutableStateOf(dependencies.getDevDemoMode.execute()) }
    var dummyData by remember { mutableStateOf(dependencies.getDevDummyData.execute()) }

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
            title = "Pengaturan",
            subtitle = "Atur aturan inspeksi dan preferensi aplikasi.",
        )
        feedback?.let { StatusBanner(feedback = it) }

        SettingsSectionCard(
            title = "Aturan Inspeksi",
            subtitle = "Aturan standar untuk checksheet harian.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppBadge(
                    text = "ATURAN",
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralTextMuted,
                )
                Text(
                    text = "Sistem akan menolak input part yang sama di tanggal yang sama.",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }
        }

        SettingsSectionCard(
            title = "Simulasi QC (Dev)",
            subtitle = "Pengaturan simulasi untuk QA dan pengembangan.",
        ) {
            val options = lines.map { DropdownOption(it.id, it.name) }
            val selectedOption =
                selectedDevLineId
                    ?.let { id -> lines.firstOrNull { it.id == id } }
                    ?.let { line -> DropdownOption(line.id, line.name) }
            AppRadioGroup(
                label = "Line QC Aktif",
                options = options,
                selectedId = selectedOption?.id,
                onSelected = { option ->
                    selectedDevLineId = option.id
                    dependencies.setDevQcLine.execute(option.id)
                    feedback = UserFeedback(FeedbackType.SUCCESS, "Line QC diubah ke ${option.label}.")
                },
                helperText = "Input inspeksi akan auto-select line ini (tetap bisa diubah).",
                maxHeight = 140.dp,
            )
            SecondaryButton(
                text = "Gunakan Pilihan Manual",
                onClick = {
                    selectedDevLineId = null
                    dependencies.setDevQcLine.execute(null)
                    feedback = UserFeedback(FeedbackType.SUCCESS, "Line QC dikembalikan ke mode manual.")
                },
                enabled = selectedDevLineId != null,
            )
            Divider(color = NeutralBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "Mode Demo", style = MaterialTheme.typography.body1)
                    Text(
                        text = "Aktifkan untuk tampilan demo dan simulasi presentasi.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Switch(
                    checked = demoMode,
                    onCheckedChange = { checked ->
                        demoMode = checked
                        dependencies.setDevDemoMode.execute(checked)
                        feedback =
                            UserFeedback(
                                FeedbackType.SUCCESS,
                                if (checked) "Mode demo diaktifkan." else "Mode demo dimatikan.",
                            )
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "Gunakan Data Dummy", style = MaterialTheme.typography.body1)
                    Text(
                        text = "Aktifkan jika ingin menguji UI dengan data contoh.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Switch(
                    checked = dummyData,
                    onCheckedChange = { checked ->
                        dummyData = checked
                        dependencies.setDevDummyData.execute(checked)
                        feedback =
                            UserFeedback(
                                FeedbackType.SUCCESS,
                                if (checked) "Data dummy diaktifkan." else "Data dummy dimatikan.",
                            )
                    },
                )
            }
            Divider(color = NeutralBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "Izinkan Input Ulang Hari yang Sama", style = MaterialTheme.typography.body1)
                    Text(
                        text = "Aktifkan jika perlu mencoba input duplikat.",
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
                                    "Input ulang hari yang sama diizinkan."
                                } else {
                                    "Input ulang hari yang sama diblokir."
                                },
                            )
                    },
                )
            }
        }

        SettingsSectionCard(
            title = "Pemeliharaan Data",
            subtitle = "Reset dan backup data lokal.",
        ) {
            Text(
                text = "Gunakan tombol di bawah untuk mengosongkan data inspeksi dan mengisi ulang data awal.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            PrimaryButton(
                text = "Reset Data",
                onClick = { showResetDialog = true },
            )
            Divider(color = NeutralBorder, thickness = 1.dp)
            Text(
                text = "Backup/restore akan tersedia pada milestone berikutnya.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(
                    text = "Backup (Nanti)",
                    onClick = {},
                    enabled = false,
                )
                SecondaryButton(
                    text = "Restore (Nanti)",
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }

    ConfirmDialog(
        open = showResetDialog,
        title = "Reset Data",
        message = "Semua data inspeksi akan dihapus dan diisi ulang dengan data awal. Lanjutkan?",
        confirmText = "Reset",
        dismissText = "Batal",
        onConfirm = {
            showResetDialog = false
            val success = dependencies.resetData.execute()
            feedback =
                if (success) {
                    dependencies.onResetCompleted()
                    UserFeedback(FeedbackType.SUCCESS, "Data berhasil direset. Master data telah dipulihkan.")
                } else {
                    UserFeedback(FeedbackType.ERROR, "Reset data gagal. Coba ulangi.")
                }
        },
        onDismiss = { showResetDialog = false },
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
