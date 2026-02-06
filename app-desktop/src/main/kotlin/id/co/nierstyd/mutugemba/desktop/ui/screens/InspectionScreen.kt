package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.desktop.ui.util.toDisplayLabel
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InspectionScreen(
    dependencies: InspectionScreenDependencies,
    onRecordsSaved: (List<InspectionRecord>) -> Unit,
) {
    val formState = remember { InspectionFormState(dependencies) }

    LaunchedEffect(Unit) {
        formState.loadMasterData()
    }

    InspectionScreenContent(
        state = formState,
        onRecordsSaved = onRecordsSaved,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InspectionScreenContent(
    state: InspectionFormState,
    onRecordsSaved: (List<InspectionRecord>) -> Unit,
) {
    val parts = state.partsForLine()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            SectionHeader(
                title = "Input Inspeksi Harian",
                subtitle = "Pilih line, lalu isi lembar inspeksi untuk setiap part produksi.",
            )
        }

        item {
            HeaderContextCard(
                dateLabel = DateTimeFormats.formatDate(state.today),
                shiftLabel = state.shiftLabel,
            )
        }

        item {
            AppDropdown(
                label = "Line Produksi",
                options = state.lineOptions,
                selectedOption = state.selectedLineOption,
                onSelected = state::onLineSelected,
                placeholder = "Pilih line produksi",
                helperText = "Part akan muncul otomatis sesuai line.",
            )
        }

        item {
            AppDropdown(
                label = "Jenis Inspeksi",
                options = state.kindOptions,
                selectedOption = state.selectedKindOption,
                onSelected = state::onInspectionKindSelected,
                placeholder = "Pilih jenis inspeksi",
                helperText = "Saat ini fokus pada input cacat harian.",
            )
        }

        stickyHeader {
            SummaryStickyBar(summary = state.summaryTotals)
        }

        if (state.shouldShowModeWarning) {
            item {
                StatusBanner(feedback = state.modeWarning)
            }
        }

        if (parts.isEmpty()) {
            item {
                EmptyPartState()
            }
        } else {
            items(items = parts, key = { it.id }) { part ->
                PartChecksheetCard(
                    part = part,
                    defectTypes = state.defectTypes,
                    timeSlots = state.timeSlots,
                    totalCheckInput = state.totalCheckInput(part.id),
                    totalDefect = state.totalDefectQuantity(part.id),
                    totalOk = state.totalOk(part.id),
                    totalCheckInvalid = state.isTotalCheckInvalid(part.id),
                    defectSlotValues = state.defectSlotInputs,
                    expanded = state.isExpanded(part.id),
                    onToggleExpanded = { state.toggleExpanded(part.id) },
                    onTotalCheckChanged = { state.onTotalCheckChanged(part.id, it) },
                    onDefectSlotChanged = { defectId, slot, value ->
                        state.onDefectSlotChanged(part.id, defectId, slot, value)
                    },
                )
            }
        }

        item {
            state.feedback?.let { StatusBanner(feedback = it) }
        }

        item {
            InspectionActionsBar(
                canSave = state.canSave,
                onSaveRequest = { state.onSaveRequested() },
                onClearAll = { state.clearAllInputs() },
            )
        }
    }

    InspectionConfirmDialog(
        open = state.showConfirmDialog,
        summaries = state.filledPartSummaries,
        summaryTotals = state.summaryTotals,
        onConfirm = {
            state.onConfirmSave(onRecordsSaved)
        },
        onDismiss = { state.dismissConfirm() },
    )
}

private class InspectionFormState(
    private val dependencies: InspectionScreenDependencies,
) {
    private val defaults: InspectionDefaults = dependencies.defaults.getDefaults.execute()
    val timeSlots: List<InspectionTimeSlot> = InspectionTimeSlot.standardSlots()

    var lines by mutableStateOf(emptyList<Line>())
        private set
    var shifts by mutableStateOf(emptyList<Shift>())
        private set
    var parts by mutableStateOf(emptyList<Part>())
        private set
    var defectTypes by mutableStateOf(emptyList<DefectType>())
        private set

    private var selectedLineId by mutableStateOf<Long?>(defaults.lineId)
    private var inspectionKind by mutableStateOf(defaults.kind ?: InspectionKind.DEFECT)

    var feedback by mutableStateOf<UserFeedback?>(null)
        private set
    var showConfirmDialog by mutableStateOf(false)
        private set

    val defectSlotInputs = mutableStateMapOf<PartDefectSlotKey, String>()
    private val totalCheckInputs = mutableStateMapOf<Long, String>()
    private val expandedPartIds = mutableStateMapOf<Long, Boolean>()

    val today = java.time.LocalDate.now()

    val lineOptions: List<DropdownOption>
        get() = lines.map { DropdownOption(it.id, it.name) }

    val kindOptions: List<DropdownOption>
        get() = InspectionKind.values().map { DropdownOption(it.ordinal.toLong(), it.toDisplayLabel()) }

    val selectedLineOption: DropdownOption?
        get() = selectedLine?.let { DropdownOption(it.id, it.name) }

    val selectedKindOption: DropdownOption?
        get() = DropdownOption(inspectionKind.ordinal.toLong(), inspectionKind.toDisplayLabel())

    val shiftLabel: String
        get() = selectedShift?.let { "${it.code} • ${it.name}" } ?: "Shift 1 (08:00-17:00 WIB)"

    val summaryTotals: SummaryTotals
        get() {
            val totals = partsForLine().map { partIdSummary(it.id) }
            val totalCheck = totals.sumOf { it.totalCheck }
            val totalDefect = totals.sumOf { it.totalDefect }
            val totalOk = totals.sumOf { it.totalOk }
            val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
            return SummaryTotals(
                totalCheck = totalCheck,
                totalDefect = totalDefect,
                totalOk = totalOk,
                ngRatio = ratio,
            )
        }

    val filledPartSummaries: List<PartSummaryRow>
        get() =
            partsForLine().mapNotNull { part ->
                val summary = partIdSummary(part.id)
                val hasData = summary.totalCheck > 0 || summary.totalDefect > 0
                if (!hasData) {
                    null
                } else {
                    PartSummaryRow(
                        partNumber = part.partNumber,
                        partName = part.name,
                        totalCheck = summary.totalCheck,
                        totalDefect = summary.totalDefect,
                        totalOk = summary.totalOk,
                    )
                }
            }

    val modeWarning: UserFeedback
        get() = UserFeedback(FeedbackType.WARNING, "Mode CTQ belum tersedia di input massal.")

    val shouldShowModeWarning: Boolean
        get() = inspectionKind == InspectionKind.CTQ

    val canSave: Boolean
        get() =
            inspectionKind == InspectionKind.DEFECT &&
                selectedLine != null &&
                filledPartSummaries.isNotEmpty() &&
                !hasInvalidTotals

    private val selectedLine: Line?
        get() = lines.firstOrNull { it.id == selectedLineId }

    private val selectedShift: Shift?
        get() = shifts.firstOrNull()

    private val hasInvalidTotals: Boolean
        get() = partsForLine().any { isTotalCheckInvalid(it.id) }

    fun loadMasterData() {
        lines = dependencies.masterData.getLines.execute()
        shifts = dependencies.masterData.getShifts.execute()
        parts = dependencies.masterData.getParts.execute()
        defectTypes = dependencies.masterData.getDefectTypes.execute()
        syncSelections()
        ensureInputs()
    }

    fun onLineSelected(option: DropdownOption) {
        selectedLineId = option.id
        ensureInputs()
    }

    fun onInspectionKindSelected(option: DropdownOption) {
        inspectionKind = InspectionKind.values()[option.id.toInt()]
    }

    fun partsForLine(): List<Part> {
        val line = selectedLine
        return if (line == null) {
            emptyList()
        } else {
            parts.filter { it.lineCode == line.code }
        }
    }

    fun totalCheckInput(partId: Long): String = totalCheckInputs[partId] ?: ""

    fun totalDefectQuantity(partId: Long): Int = defectTypes.sumOf { defectRowTotal(partId, it.id) }

    fun totalOk(partId: Long): Int {
        val totalCheck = totalCheckInputs[partId]?.toIntOrNull() ?: 0
        return (totalCheck - totalDefectQuantity(partId)).coerceAtLeast(0)
    }

    fun isTotalCheckInvalid(partId: Long): Boolean {
        val totalCheck = totalCheckInputs[partId]?.toIntOrNull()
        val totalDefect = totalDefectQuantity(partId)
        return totalCheck != null && totalCheck < totalDefect
    }

    fun onTotalCheckChanged(
        partId: Long,
        value: String,
    ) {
        totalCheckInputs[partId] = value
    }

    fun onDefectSlotChanged(
        partId: Long,
        defectId: Long,
        slot: InspectionTimeSlot,
        value: String,
    ) {
        defectSlotInputs[PartDefectSlotKey(partId, defectId, slot)] = value
    }

    fun isExpanded(partId: Long): Boolean = expandedPartIds[partId] ?: false

    fun toggleExpanded(partId: Long) {
        val current = expandedPartIds[partId] ?: false
        expandedPartIds[partId] = !current
    }

    fun onSaveRequested() {
        feedback = null
        if (inspectionKind == InspectionKind.CTQ) {
            feedback = modeWarning
            return
        }
        if (selectedLine == null || selectedShift == null) {
            feedback = UserFeedback(FeedbackType.ERROR, "Pilih line produksi terlebih dahulu.")
            return
        }
        if (filledPartSummaries.isEmpty()) {
            feedback = UserFeedback(FeedbackType.ERROR, "Isi minimal satu part sebelum disimpan.")
            return
        }
        if (hasInvalidTotals) {
            feedback = UserFeedback(FeedbackType.ERROR, "Periksa total check yang kurang dari total cacat.")
            return
        }
        showConfirmDialog = true
    }

    fun dismissConfirm() {
        showConfirmDialog = false
    }

    fun onConfirmSave(onRecordsSaved: (List<InspectionRecord>) -> Unit) {
        val inputs = buildInputs()
        val result = dependencies.createBatchInspectionUseCase.execute(inputs)
        feedback = result.feedback
        showConfirmDialog = false
        if (result.savedRecords.isNotEmpty()) {
            onRecordsSaved(result.savedRecords)
        }
        if (result.feedback.type == FeedbackType.SUCCESS) {
            clearAllInputs()
        }
        saveDefaults()
    }

    fun clearAllInputs() {
        totalCheckInputs.keys.toList().forEach { totalCheckInputs[it] = "" }
        defectSlotInputs.keys.toList().forEach { defectSlotInputs[it] = "" }
    }

    private fun buildInputs(): List<InspectionInput> {
        val line = selectedLine ?: return emptyList()
        val shift = selectedShift ?: return emptyList()
        val createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        return partsForLine().mapNotNull { part ->
            val defectEntries = buildDefectEntries(part.id)
            val totalCheck = totalCheckInputs[part.id]?.toIntOrNull()
            val hasData = defectEntries.sumOf { it.totalQuantity } > 0 || (totalCheck ?: 0) > 0
            if (!hasData) {
                null
            } else {
                InspectionInput(
                    kind = inspectionKind,
                    lineId = line.id,
                    shiftId = shift.id,
                    partId = part.id,
                    totalCheck = totalCheck,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = defectEntries,
                    ctqParameterId = null,
                    ctqValue = null,
                    createdAt = createdAt,
                )
            }
        }
    }

    private fun buildDefectEntries(partId: Long): List<InspectionDefectEntry> =
        defectTypes.mapNotNull { defect ->
            val slots =
                timeSlots.mapNotNull { slot ->
                    val quantity = slotQuantity(partId, defect.id, slot)
                    if (quantity > 0) {
                        id.co.nierstyd.mutugemba.domain.InspectionDefectSlot(slot, quantity)
                    } else {
                        null
                    }
                }
            val total = slots.sumOf { it.quantity }
            if (total > 0) {
                InspectionDefectEntry(defectTypeId = defect.id, quantity = total, slots = slots)
            } else {
                null
            }
        }

    private fun slotQuantity(
        partId: Long,
        defectId: Long,
        slot: InspectionTimeSlot,
    ): Int =
        defectSlotInputs[PartDefectSlotKey(partId, defectId, slot)]
            ?.toIntOrNull()
            ?: 0

    private fun defectRowTotal(
        partId: Long,
        defectId: Long,
    ): Int = timeSlots.sumOf { slot -> slotQuantity(partId, defectId, slot) }

    private fun partIdSummary(partId: Long): PartTotals {
        val totalCheck = totalCheckInputs[partId]?.toIntOrNull() ?: 0
        val totalDefect = totalDefectQuantity(partId)
        val totalOk = (totalCheck - totalDefect).coerceAtLeast(0)
        return PartTotals(totalCheck = totalCheck, totalDefect = totalDefect, totalOk = totalOk)
    }

    private fun syncSelections() {
        selectedLineId = resolveSelection(selectedLineId, defaults.lineId, lines.map { it.id })
    }

    private fun ensureInputs() {
        val validPartIds = partsForLine().map { it.id }.toSet()
        totalCheckInputs.keys.filter { it !in validPartIds }.forEach { totalCheckInputs.remove(it) }
        expandedPartIds.keys.filter { it !in validPartIds }.forEach { expandedPartIds.remove(it) }
        defectSlotInputs.keys.filter { it.partId !in validPartIds }.forEach { defectSlotInputs.remove(it) }
        validPartIds.forEach { partId ->
            totalCheckInputs.putIfAbsent(partId, "")
            expandedPartIds.putIfAbsent(partId, false)
        }
        if (expandedPartIds.values.none { it } && validPartIds.isNotEmpty()) {
            expandedPartIds[validPartIds.first()] = true
        }
        defectTypes.forEach { defect ->
            validPartIds.forEach { partId ->
                timeSlots.forEach { slot ->
                    defectSlotInputs.putIfAbsent(PartDefectSlotKey(partId, defect.id, slot), "")
                }
            }
        }
    }

    private fun saveDefaults() {
        dependencies.defaults.saveDefaults.execute(
            defaults.copy(
                lineId = selectedLine?.id,
                shiftId = selectedShift?.id,
                kind = inspectionKind,
            ),
        )
    }
}

@Composable
private fun HeaderContextCard(
    dateLabel: String,
    shiftLabel: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Tanggal", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
                Text(text = dateLabel, style = MaterialTheme.typography.h6)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
                Text(text = "Shift Aktif", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
                Text(text = shiftLabel, style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}

@Composable
private fun SummaryStickyBar(summary: SummaryTotals) {
    Surface(
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SummaryStat(title = "Total Periksa", value = summary.totalCheck.toString())
            SummaryStat(title = "Total Cacat", value = summary.totalDefect.toString())
            SummaryStat(title = "Total OK", value = summary.totalOk.toString())
            SummaryStat(title = "Rasio NG", value = formatPercent(summary.ngRatio))
        }
    }
}

@Composable
private fun RowScope.SummaryStat(
    title: String,
    value: String,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
private fun EmptyPartState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Belum ada part untuk line ini.", style = MaterialTheme.typography.body1)
            Text(
                "Periksa master part atau ganti line produksi.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun InspectionActionsBar(
    canSave: Boolean,
    onSaveRequest: () -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryButton(
            text = "Bersihkan Semua",
            onClick = onClearAll,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = "Konfirmasi & Simpan",
            onClick = onSaveRequest,
            enabled = canSave,
        )
    }
}

@Composable
private fun InspectionConfirmDialog(
    open: Boolean,
    summaries: List<PartSummaryRow>,
    summaryTotals: SummaryTotals,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) {
        return
    }

    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Simpan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "Data inspeksi akan disimpan dan tidak bisa diubah kembali.",
                    style = MaterialTheme.typography.body2,
                )
                Text("Ringkasan Part Terisi", style = MaterialTheme.typography.subtitle1)
                summaries.forEach { row ->
                    Text("${row.partNumber} • ${row.partName} (OK: ${row.totalOk}, NG: ${row.totalDefect})")
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Total Periksa: ${summaryTotals.totalCheck} | Total NG: ${summaryTotals.totalDefect} | Total OK: ${summaryTotals.totalOk}",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }
        },
        confirmButton = {
            PrimaryButton(text = "Simpan Sekarang", onClick = onConfirm)
        },
        dismissButton = {
            SecondaryButton(text = "Batal", onClick = onDismiss)
        },
    )
}

private fun formatPercent(value: Double): String =
    if (value <= 0.0) {
        "-"
    } else {
        "${"%.1f".format(value * 100)}%"
    }
