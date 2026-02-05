package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.WizardStepIndicator
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.usecase.GetLastInspectionTypeUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastInspectionTypeUseCase
import java.time.LocalDate

@Composable
fun InspectionScreen(
    getLastInspectionTypeUseCase: GetLastInspectionTypeUseCase,
    setLastInspectionTypeUseCase: SetLastInspectionTypeUseCase,
) {
    val stepLabels = listOf("Konteks", "Input", "Simpan")
    var currentStep by remember { mutableStateOf(1) }
    val inspectionOptions = listOf("Cacat", "Parameter Proses")
    val initialInspectionType =
        remember {
            getLastInspectionTypeUseCase.execute() ?: inspectionOptions.first()
        }
    var inspectionType by remember { mutableStateOf(initialInspectionType) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Input Inspeksi",
            subtitle = "Ikuti 3 langkah agar data rapi dan konsisten.",
        )
        WizardStepIndicator(currentStep = currentStep, labels = stepLabels)

        when (currentStep) {
            1 -> {
                Text(
                    text = "Default Konteks",
                    style = MaterialTheme.typography.subtitle1,
                )
                Text("Tanggal: ${LocalDate.now()}")
                Text("Shift: 1 (default)")
                Text("Line: A (terakhir dipakai)")
            }

            2 -> {
                Text(
                    text = "Input Utama",
                    style = MaterialTheme.typography.subtitle1,
                )
                AppDropdown(
                    label = "Jenis Inspeksi",
                    options = inspectionOptions,
                    selected = inspectionType,
                    onSelected = {
                        inspectionType = it
                        setLastInspectionTypeUseCase.execute(it)
                    },
                )
            }

            else -> {
                Text(
                    text = "Konfirmasi",
                    style = MaterialTheme.typography.subtitle1,
                )
                Text("Jenis inspeksi: $inspectionType")
                Text("Tekan simpan untuk data dummy.")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SecondaryButton(
                text = "Kembali",
                onClick = { if (currentStep > 1) currentStep -= 1 },
                enabled = currentStep > 1,
            )
            PrimaryButton(
                text = if (currentStep < 3) "Lanjut" else "Simpan",
                onClick = {
                    if (currentStep < 3) {
                        currentStep += 1
                    } else {
                        showConfirm = true
                    }
                },
            )
        }
    }

    ConfirmDialog(
        open = showConfirm,
        title = "Simpan Data",
        message = "Simpan data dummy untuk contoh inspeksi?",
        confirmText = "Ya, Simpan",
        dismissText = "Batal",
        onConfirm = { showConfirm = false },
        onDismiss = { showConfirm = false },
    )
}
