package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.AppNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.InfoCard
import id.co.nierstyd.mutugemba.desktop.ui.components.MilestoneItem
import id.co.nierstyd.mutugemba.desktop.ui.components.MilestonePanel
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.components.WizardStepIndicator
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.desktop.ui.util.toDisplayLabel
import id.co.nierstyd.mutugemba.domain.CtqParameter
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionDefectSlot
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.InspectionDefaults
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun InspectionScreen(
    dependencies: InspectionScreenDependencies,
    onRecordSaved: (InspectionRecord) -> Unit,
) {
    val formState = remember { InspectionFormState(dependencies) }

    LaunchedEffect(Unit) {
        formState.loadMasterData()
    }

    InspectionScreenContent(
        state = formState.uiState,
        onStepChange = formState::onStepChange,
        onSaveRequest = { formState.onSave(onRecordSaved) },
    )
}

@Composable
private fun InspectionScreenContent(
    state: InspectionUiState,
    onStepChange: (Int) -> Unit,
    onSaveRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Input Inspeksi",
            subtitle = "Ikuti 3 langkah agar data rapi dan konsisten.",
        )
        InfoCard(title = "Prosedur Singkat") {
            Text("1. Pilih Line (Press / Sewing).")
            Text("2. Isi tabel cacat sesuai jam produksi.")
            Text("3. Konfirmasi lalu simpan.")
        }
        WizardStepIndicator(currentStep = state.currentStep, labels = state.stepLabels)
        MilestonePanel(title = "Proses Input", items = state.milestoneItems)

        when (state.currentStep) {
            1 ->
                ContextStepContent(
                    state = state.context.state,
                    options = state.context.options,
                    actions = state.context.actions,
                )

            2 ->
                InputStepContent(
                    state = state.input.state,
                    options = state.input.options,
                    actions = state.input.actions,
                )

            else -> ConfirmStepContent(state = state.confirm)
        }

        state.feedback?.let { StatusBanner(feedback = it) }
            ?: state.ctqWarning?.let { StatusBanner(feedback = it) }

        InspectionActions(
            currentStep = state.currentStep,
            onStepChange = onStepChange,
            onSaveRequest = onSaveRequest,
        )
    }
}

@Suppress("TooManyFunctions")
private class InspectionFormState(
    private val dependencies: InspectionScreenDependencies,
) {
    private val defaults: InspectionDefaults = dependencies.defaults.getDefaults.execute()
    private val stepLabels = listOf("Konteks", "Input", "Simpan")
    private val timeSlots = InspectionTimeSlot.standardSlots()

    var currentStep by mutableStateOf(1)
        private set

    private var lines by mutableStateOf(emptyList<Line>())
    private var shifts by mutableStateOf(emptyList<Shift>())
    private var parts by mutableStateOf(emptyList<Part>())
    private var defectTypes by mutableStateOf(emptyList<DefectType>())
    private var ctqParameters by mutableStateOf(emptyList<CtqParameter>())

    private var selectedLineId by mutableStateOf<Long?>(defaults.lineId)
    private var selectedShiftId by mutableStateOf<Long?>(defaults.shiftId)
    private var selectedPartId by mutableStateOf<Long?>(defaults.partId)
    private var selectedCtqParameterId by mutableStateOf<Long?>(defaults.ctqParameterId)
    private var inspectionKind by mutableStateOf(defaults.kind ?: InspectionKind.DEFECT)
    private var ctqValueInput by mutableStateOf("")
    private var totalCheckInput by mutableStateOf("")
    private val defectSlotInputs = androidx.compose.runtime.mutableStateMapOf<DefectSlotKey, String>()

    var feedback by mutableStateOf<UserFeedback?>(null)
        private set

    val uiState: InspectionUiState
        get() =
            InspectionUiState(
                stepLabels = stepLabels,
                currentStep = currentStep,
                milestoneItems = milestoneItems,
                feedback = feedback,
                ctqWarning = ctqWarning,
                context =
                    ContextStepBundle(
                        state = contextState,
                        options = contextOptions,
                        actions = contextActions,
                    ),
                input =
                    InputStepBundle(
                        state = inputState,
                        options = inputOptions,
                        actions = inputActions,
                    ),
                confirm = confirmState,
            )

    fun loadMasterData() {
        lines = dependencies.masterData.getLines.execute()
        shifts = dependencies.masterData.getShifts.execute()
        parts = dependencies.masterData.getParts.execute()
        defectTypes = dependencies.masterData.getDefectTypes.execute()
        ctqParameters = dependencies.masterData.getCtqParameters.execute()
        syncSelections()
        ensureDefectInputs()
    }

    fun onStepChange(nextStep: Int) {
        val stepAllowed =
            when (nextStep) {
                2 -> validateStep(contextValid, "Lengkapi Line, Shift, dan Part sebelum lanjut.")
                3 -> validateStep(inputValid, "Lengkapi input utama sebelum lanjut.")
                else -> true
            }

        if (stepAllowed) {
            feedback = null
            currentStep = nextStep
        }
    }

    fun onSave(onRecordSaved: (InspectionRecord) -> Unit) {
        val result = dependencies.createInspectionUseCase.execute(buildInput())
        feedback = result.feedback
        result.record?.let { record ->
            onRecordSaved(record)
            saveDefaults()
            resetAfterSave()
        }
    }

    private fun validateStep(
        isValid: Boolean,
        errorMessage: String,
    ): Boolean {
        if (!isValid) {
            feedback = UserFeedback(FeedbackType.ERROR, errorMessage)
        }
        return isValid
    }

    private fun saveDefaults() {
        dependencies.defaults.saveDefaults.execute(
            defaults.copy(
                lineId = selectedLine?.id,
                shiftId = selectedShift?.id,
                partId = selectedPart?.id,
                ctqParameterId = selectedCtqParameter?.id,
                kind = inspectionKind,
            ),
        )
    }

    private fun resetAfterSave() {
        currentStep = 1
        ctqValueInput = ""
        totalCheckInput = ""
        defectSlotInputs.keys.toList().forEach { defectSlotInputs[it] = "" }
    }

    private fun buildInput(): InspectionInput {
        val createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val defectEntries = buildDefectEntries()
        return InspectionInput(
            kind = inspectionKind,
            lineId = selectedLine?.id ?: 0L,
            shiftId = selectedShift?.id ?: 0L,
            partId = selectedPart?.id ?: 0L,
            defectTypeId = null,
            defectQuantity = null,
            defects = defectEntries,
            ctqParameterId = selectedCtqParameter?.id,
            ctqValue = ctqValue,
            createdAt = createdAt,
        )
    }

    private fun syncSelections() {
        selectedLineId = resolveSelection(selectedLineId, defaults.lineId, lines.map { it.id })
        selectedShiftId = resolveSelection(selectedShiftId, defaults.shiftId, shifts.map { it.id })
        selectedPartId =
            resolveSelection(selectedPartId, defaults.partId, partsForLine().map { it.id })
        selectedCtqParameterId =
            resolveSelection(selectedCtqParameterId, defaults.ctqParameterId, ctqParameters.map { it.id })
    }

    private val selectedLine: Line?
        get() = lines.firstOrNull { it.id == selectedLineId }

    private val selectedShift: Shift?
        get() = shifts.firstOrNull { it.id == selectedShiftId }

    private val selectedPart: Part?
        get() = partsForLine().firstOrNull { it.id == selectedPartId }

    private val selectedCtqParameter: CtqParameter?
        get() = ctqParameters.firstOrNull { it.id == selectedCtqParameterId }

    private val ctqValue: Double?
        get() = ctqValueInput.toDoubleOrNull()

    private val totalCheck: Int?
        get() = totalCheckInput.toIntOrNull()

    private val contextValid: Boolean
        get() = selectedLine != null && selectedShift != null && selectedPart != null

    private val inputValid: Boolean
        get() =
            when (inspectionKind) {
                InspectionKind.DEFECT -> {
                    val totalNg = totalDefectQuantity
                    val totalCheckValue = totalCheck
                    val totalCheckValid = totalCheckValue == null || totalCheckValue >= totalNg
                    totalNg > 0 && totalCheckValid
                }
                InspectionKind.CTQ -> selectedCtqParameter != null && ctqValue != null
            }

    private val milestoneItems: List<MilestoneItem>
        get() =
            listOf(
                MilestoneItem(
                    title = "Konteks",
                    subtitle = "Line, shift, dan part",
                    status = milestoneStatus(currentStep, 1, contextValid),
                ),
                MilestoneItem(
                    title = "Input",
                    subtitle = "Detail cacat / CTQ",
                    status = milestoneStatus(currentStep, 2, inputValid),
                ),
                MilestoneItem(
                    title = "Simpan",
                    subtitle = "Konfirmasi dan simpan",
                    status = milestoneStatus(currentStep, 3, contextValid && inputValid),
                ),
            )

    private val ctqWarning: UserFeedback?
        get() =
            if (inspectionKind == InspectionKind.CTQ) {
                buildCtqWarning(selectedCtqParameter, ctqValue)
            } else {
                null
            }

    private val contextState: ContextStepState
        get() =
            ContextStepState(
                selectedLine = selectedLine,
                selectedShift = selectedShift,
                selectedPart = selectedPart,
            )

    private val contextOptions: ContextStepOptions
        get() =
            ContextStepOptions(
                lineOptions = lines.map { DropdownOption(it.id, it.name) },
                shiftOptions =
                    shifts.map { shift ->
                        DropdownOption(
                            id = shift.id,
                            label = "${shift.code} - ${shift.name}",
                            helper = shiftTimeLabel(shift),
                        )
                    },
                partOptions =
                    partsForLine().map { part ->
                        DropdownOption(
                            id = part.id,
                            label = "${part.partNumber} - ${part.name}",
                            helper = "${part.model} | ${part.material}",
                        )
                    },
            )

    private val contextActions: ContextStepActions =
        ContextStepActions(
            onLineSelected = { option ->
                selectedLineId = option.id
                selectedPartId = partsForLine().firstOrNull()?.id
            },
            onShiftSelected = { selectedShiftId = it.id },
            onPartSelected = { selectedPartId = it.id },
        )

    private val inputState: InputStepState
        get() =
            InputStepState(
                inspectionKind = inspectionKind,
                selectedCtqParameter = selectedCtqParameter,
                ctqValueInput = ctqValueInput,
                defectTypes = defectTypes,
                timeSlots = timeSlots,
                defectSlotValues = defectSlotInputs.toMap(),
                totalCheckInput = totalCheckInput,
                totalDefectQuantity = totalDefectQuantity,
                totalCheck = totalCheck,
                totalOk = totalOk,
                selectedPart = selectedPart,
            )

    private val inputOptions: InputStepOptions
        get() =
            InputStepOptions(
                ctqOptions =
                    ctqParameters.map { ctq ->
                        DropdownOption(
                            id = ctq.id,
                            label = "${ctq.code} - ${ctq.name}",
                            helper = ctqLimitLabel(ctq),
                        )
                    },
            )

    private val inputActions: InputStepActions =
        InputStepActions(
            onInspectionKindChanged = { inspectionKind = it },
            onCtqParameterSelected = { selectedCtqParameterId = it.id },
            onDefectSlotChanged = { defectId, slot, value ->
                defectSlotInputs[DefectSlotKey(defectId, slot)] = value
            },
            onTotalCheckChanged = { totalCheckInput = it },
            onCtqValueChanged = { ctqValueInput = it },
        )

    private val confirmState: ConfirmStepState
        get() =
            ConfirmStepState(
                context =
                    ConfirmContextSummary(
                        line = selectedLine,
                        shift = selectedShift,
                        part = selectedPart,
                    ),
                input =
                    ConfirmInputSummary(
                        inspectionKind = inspectionKind,
                        defects = defectSummaries,
                        totalCheck = totalCheck,
                        totalNg = totalDefectQuantity,
                        totalOk = totalOk,
                        ctqParameter = selectedCtqParameter,
                        ctqValue = ctqValue,
                    ),
            )

    private fun partsForLine(): List<Part> {
        val line = selectedLine
        return if (line == null) {
            parts
        } else {
            parts.filter { it.lineCode == line.code }
        }
    }

    private fun ensureDefectInputs() {
        val validKeys =
            buildSet {
                defectTypes.forEach { defect ->
                    timeSlots.forEach { slot ->
                        add(DefectSlotKey(defect.id, slot))
                    }
                }
            }
        defectSlotInputs.keys.filter { it !in validKeys }.forEach { defectSlotInputs.remove(it) }
        validKeys.forEach { key ->
            defectSlotInputs.putIfAbsent(key, "")
        }
    }

    private fun slotQuantity(
        defectId: Long,
        slot: InspectionTimeSlot,
    ): Int =
        defectSlotInputs[DefectSlotKey(defectId, slot)]
            ?.toIntOrNull()
            ?: 0

    private fun defectRowTotal(defectId: Long): Int = timeSlots.sumOf { slot -> slotQuantity(defectId, slot) }

    private val totalDefectQuantity: Int
        get() = defectTypes.sumOf { defectRowTotal(it.id) }

    private val totalOk: Int
        get() = ((totalCheck ?: 0) - totalDefectQuantity).coerceAtLeast(0)

    private val defectSummaries: List<DefectSummary>
        get() =
            defectTypes.mapNotNull { defect ->
                val qty = defectRowTotal(defect.id)
                if (qty > 0) {
                    DefectSummary(defect = defect, quantity = qty)
                } else {
                    null
                }
            }

    private fun buildDefectEntries(): List<InspectionDefectEntry> =
        defectTypes.mapNotNull { defect ->
            val slots =
                timeSlots.mapNotNull { slot ->
                    val qty = slotQuantity(defect.id, slot)
                    if (qty > 0) {
                        InspectionDefectSlot(slot, qty)
                    } else {
                        null
                    }
                }
            val total = slots.sumOf { it.quantity }
            if (total > 0) {
                InspectionDefectEntry(
                    defectTypeId = defect.id,
                    quantity = total,
                    slots = slots,
                )
            } else {
                null
            }
        }
}

@Composable
private fun ContextStepContent(
    state: ContextStepState,
    options: ContextStepOptions,
    actions: ContextStepActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        InfoCard(title = "Default Konteks") {
            Text("Tanggal: ${DateTimeFormats.formatDate(LocalDate.now())}")
            Text("Mode: Offline - Lokal", color = NeutralTextMuted)
            Text(
                "Shift: ${state.selectedShift?.let { "${it.code} - ${it.name}" } ?: "-"}",
                color = NeutralTextMuted,
            )
        }

        AppDropdown(
            label = "Line",
            options = options.lineOptions,
            selectedOption = state.selectedLine?.let { DropdownOption(it.id, it.name) },
            onSelected = actions.onLineSelected,
            placeholder = "Pilih Line",
        )
        AppDropdown(
            label = "Shift",
            options = options.shiftOptions,
            selectedOption =
                state.selectedShift?.let { shift ->
                    DropdownOption(
                        id = shift.id,
                        label = "${shift.code} - ${shift.name}",
                        helper = shiftTimeLabel(shift),
                    )
                },
            onSelected = actions.onShiftSelected,
            placeholder = "Pilih Shift",
            enabled = options.shiftOptions.size > 1,
        )
        AppDropdown(
            label = "Produk / Part",
            options = options.partOptions,
            selectedOption =
                state.selectedPart?.let { part ->
                    DropdownOption(
                        id = part.id,
                        label = "${part.partNumber} - ${part.name}",
                        helper = "${part.model} | ${part.material}",
                    )
                },
            onSelected = actions.onPartSelected,
            placeholder = "Pilih Part",
        )

        state.selectedPart?.let { part ->
            InfoCard(title = "Detail Part") {
                Text("Part Number: ${part.partNumber}")
                Text("Model: ${part.model}")
                Text("Nama: ${part.name}")
                Text("UNIQ Code: ${part.uniqCode}")
                Text("Material: ${part.material}")
            }
        }
    }
}

@Composable
private fun InputStepContent(
    state: InputStepState,
    options: InputStepOptions,
    actions: InputStepActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val kindOptions =
            InspectionKind.values().map { kind ->
                DropdownOption(kind.ordinal.toLong(), kind.toDisplayLabel())
            }
        AppDropdown(
            label = "Jenis Inspeksi",
            options = kindOptions,
            selectedOption =
                DropdownOption(
                    state.inspectionKind.ordinal.toLong(),
                    state.inspectionKind.toDisplayLabel(),
                ),
            onSelected = { option -> actions.onInspectionKindChanged(InspectionKind.values()[option.id.toInt()]) },
        )

        when (state.inspectionKind) {
            InspectionKind.DEFECT -> {
                DefectTableContent(
                    state = state,
                    actions = actions,
                )
            }

            InspectionKind.CTQ -> {
                AppDropdown(
                    label = "Parameter CTQ",
                    options = options.ctqOptions,
                    selectedOption =
                        state.selectedCtqParameter?.let { ctq ->
                            DropdownOption(
                                id = ctq.id,
                                label = "${ctq.code} - ${ctq.name}",
                                helper = ctqLimitLabel(ctq),
                            )
                        },
                    onSelected = actions.onCtqParameterSelected,
                    placeholder = "Pilih Parameter CTQ",
                )
                AppNumberField(
                    spec =
                        FieldSpec(
                            label = "Nilai CTQ",
                            placeholder = "Contoh: 10.02",
                            helperText =
                                state.selectedCtqParameter?.let { ctqLimitLabel(it) }
                                    ?: "Masukkan hasil pengukuran.",
                        ),
                    value = state.ctqValueInput,
                    onValueChange = actions.onCtqValueChanged,
                    allowDecimal = true,
                )
            }
        }
    }
}

@Composable
private fun ConfirmStepContent(state: ConfirmStepState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        InfoCard(title = "Ringkasan Input") {
            Text("Jenis inspeksi: ${state.input.inspectionKind.toDisplayLabel()}")
            Text("Line: ${state.context.line?.name ?: "-"}")
            Text("Shift: ${state.context.shift?.name ?: "-"}")
            Text("Part: ${state.partLabel}")
            when (state.input.inspectionKind) {
                InspectionKind.DEFECT -> {
                    if (state.input.defects.isEmpty()) {
                        Text("Cacat: -")
                    } else {
                        state.input.defects.forEach { defect ->
                            Text("Cacat: ${defect.defect.name} (Qty: ${defect.quantity})")
                        }
                    }
                    Text("Total NG: ${state.input.totalNg}")
                    Text("Total Check: ${state.input.totalCheck?.toString() ?: "-"}")
                    Text("Total OK: ${state.totalOkLabel}")
                }

                InspectionKind.CTQ ->
                    Text(
                        "CTQ: ${state.input.ctqParameter?.name ?: "-"} " +
                            "(Nilai: ${state.ctqValueLabel})",
                    )
            }
        }
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

private data class InspectionUiState(
    val stepLabels: List<String>,
    val currentStep: Int,
    val milestoneItems: List<MilestoneItem>,
    val feedback: UserFeedback?,
    val ctqWarning: UserFeedback?,
    val context: ContextStepBundle,
    val input: InputStepBundle,
    val confirm: ConfirmStepState,
)

private data class ContextStepBundle(
    val state: ContextStepState,
    val options: ContextStepOptions,
    val actions: ContextStepActions,
)

private data class InputStepBundle(
    val state: InputStepState,
    val options: InputStepOptions,
    val actions: InputStepActions,
)

private data class ContextStepState(
    val selectedLine: Line?,
    val selectedShift: Shift?,
    val selectedPart: Part?,
)

private data class ContextStepOptions(
    val lineOptions: List<DropdownOption>,
    val shiftOptions: List<DropdownOption>,
    val partOptions: List<DropdownOption>,
)

private data class ContextStepActions(
    val onLineSelected: (DropdownOption) -> Unit,
    val onShiftSelected: (DropdownOption) -> Unit,
    val onPartSelected: (DropdownOption) -> Unit,
)

internal data class InputStepState(
    val inspectionKind: InspectionKind,
    val selectedCtqParameter: CtqParameter?,
    val ctqValueInput: String,
    val defectTypes: List<DefectType>,
    val timeSlots: List<InspectionTimeSlot>,
    val defectSlotValues: Map<DefectSlotKey, String>,
    val totalCheckInput: String,
    val totalDefectQuantity: Int,
    val totalCheck: Int?,
    val totalOk: Int,
    val selectedPart: Part?,
)

internal data class InputStepOptions(
    val ctqOptions: List<DropdownOption>,
)

internal data class InputStepActions(
    val onInspectionKindChanged: (InspectionKind) -> Unit,
    val onCtqParameterSelected: (DropdownOption) -> Unit,
    val onDefectSlotChanged: (Long, InspectionTimeSlot, String) -> Unit,
    val onTotalCheckChanged: (String) -> Unit,
    val onCtqValueChanged: (String) -> Unit,
)

private data class ConfirmContextSummary(
    val line: Line?,
    val shift: Shift?,
    val part: Part?,
)

private data class ConfirmInputSummary(
    val inspectionKind: InspectionKind,
    val defects: List<DefectSummary>,
    val totalCheck: Int?,
    val totalNg: Int,
    val totalOk: Int,
    val ctqParameter: CtqParameter?,
    val ctqValue: Double?,
)

private data class ConfirmStepState(
    val context: ConfirmContextSummary,
    val input: ConfirmInputSummary,
) {
    val partLabel: String
        get() = "${context.part?.partNumber ?: "-"} - ${context.part?.name ?: "-"}"

    val ctqValueLabel: String
        get() = formatDecimal(input.ctqValue)

    val totalOkLabel: String
        get() = input.totalCheck?.let { input.totalOk.toString() } ?: "-"
}

private data class DefectSummary(
    val defect: DefectType,
    val quantity: Int,
)

internal data class DefectSlotKey(
    val defectTypeId: Long,
    val slot: InspectionTimeSlot,
)
