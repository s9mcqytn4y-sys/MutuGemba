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
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetLastInspectionTypeUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastInspectionTypeUseCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun InspectionScreen(
    getLastInspectionTypeUseCase: GetLastInspectionTypeUseCase,
    setLastInspectionTypeUseCase: SetLastInspectionTypeUseCase,
    createInspectionUseCase: CreateInspectionRecordUseCase,
    onRecordSaved: (InspectionRecord) -> Unit,
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
        InspectionStepContent(
            step = currentStep,
            inspectionType = inspectionType,
            inspectionOptions = inspectionOptions,
            onInspectionTypeChanged = {
                inspectionType = it
                setLastInspectionTypeUseCase.execute(it)
            },
        )
        InspectionActions(
            currentStep = currentStep,
            onStepChange = { currentStep = it },
            onSaveRequest = { showConfirm = true },
        )
    }

    SaveInspectionDialog(
        open = showConfirm,
        inspectionType = inspectionType,
        createInspectionUseCase = createInspectionUseCase,
        onRecordSaved = onRecordSaved,
        onClose = {
            showConfirm = false
            currentStep = 1
        },
    )
}

@Composable
private fun InspectionStepContent(
    step: Int,
    inspectionType: String,
    inspectionOptions: List<String>,
    onInspectionTypeChanged: (String) -> Unit,
) {
    when (step) {
        1 -> ContextStepContent()
        2 ->
            InputStepContent(
                inspectionType = inspectionType,
                options = inspectionOptions,
                onInspectionTypeChanged = onInspectionTypeChanged,
            )

        else -> ConfirmStepContent(inspectionType = inspectionType)
    }
}

@Composable
private fun ContextStepContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Default Konteks",
            style = MaterialTheme.typography.subtitle1,
        )
        Text("Tanggal: ${LocalDate.now()}")
        Text("Shift: 1 (default)")
        Text("Line: A (terakhir dipakai)")
    }
}

@Composable
private fun InputStepContent(
    inspectionType: String,
    options: List<String>,
    onInspectionTypeChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Input Utama",
            style = MaterialTheme.typography.subtitle1,
        )
        AppDropdown(
            label = "Jenis Inspeksi",
            options = options,
            selected = inspectionType,
            onSelected = onInspectionTypeChanged,
        )
    }
}

@Composable
private fun ConfirmStepContent(inspectionType: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Konfirmasi",
            style = MaterialTheme.typography.subtitle1,
        )
        Text("Jenis inspeksi: $inspectionType")
        Text("Tekan simpan untuk data dummy.")
    }
}

@Composable
private fun InspectionActions(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onSaveRequest: () -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SecondaryButton(
            text = "Kembali",
            onClick = { if (currentStep > 1) onStepChange(currentStep - 1) },
            enabled = currentStep > 1,
        )
        PrimaryButton(
            text = if (currentStep < 3) "Lanjut" else "Simpan",
            onClick = {
                if (currentStep < 3) {
                    onStepChange(currentStep + 1)
                } else {
                    onSaveRequest()
                }
            },
        )
    }
}

@Composable
private fun SaveInspectionDialog(
    open: Boolean,
    inspectionType: String,
    createInspectionUseCase: CreateInspectionRecordUseCase,
    onRecordSaved: (InspectionRecord) -> Unit,
    onClose: () -> Unit,
) {
    ConfirmDialog(
        open = open,
        title = "Simpan Data",
        message = "Simpan data dummy untuk contoh inspeksi?",
        confirmText = "Ya, Simpan",
        dismissText = "Batal",
        onConfirm = {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val record =
                createInspectionUseCase.execute(
                    type = inspectionType,
                    lineName = "Line A",
                    createdAt = timestamp,
                )
            onRecordSaved(record)
            onClose()
        },
        onDismiss = onClose,
    )
}
