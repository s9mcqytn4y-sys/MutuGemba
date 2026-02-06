package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppRadioGroup
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.InspectionDefaults
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import kotlinx.coroutines.delay
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
    val feedback = state.feedback

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(4000)
            state.clearFeedback()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            SectionHeader(
                title = "Input Inspeksi Harian",
                subtitle = "Masukkan data checksheet harian secara cepat dan terstruktur.",
            )
        }

        item {
            ProcessStepsRow()
        }

        item {
            SectionLabel(
                title = "Konteks Inspeksi",
                subtitle = "Pastikan tanggal dan shift sesuai kondisi produksi hari ini.",
            )
        }

        item {
            HeaderContextCard(
                dateLabel = DateTimeFormats.formatDate(state.today),
                shiftLabel = state.shiftLabel,
            )
        }

        item {
            SectionLabel(
                title = "Line Produksi",
                subtitle = "Pilih area produksi yang akan diinput hari ini.",
            )
        }

        item {
            InspectionSelectorCard(
                lineOptions = state.lineOptions,
                selectedLineOption = state.selectedLineOption,
                onLineSelected = state::onLineSelected,
                lineHint = state.lineHint,
                allowDuplicate = state.isDuplicateAllowed(),
            )
        }

        item {
            SectionLabel(
                title = "Checksheet Per Part",
                subtitle = "Isi part yang diproduksi hari ini. Part yang tidak diproduksi boleh dibiarkan kosong.",
            )
        }

        stickyHeader {
            SummaryStickyBar(
                summary = state.summaryTotals,
            )
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
                    status = state.partStatus(part.id),
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
        defectSummaries = state.filledPartSummaries,
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

    val lineHint: String
        get() {
            val qcLine =
                defaults.qcLineId?.let { id ->
                    lines.firstOrNull { it.id == id }?.name
                }
            return if (qcLine != null) {
                "Auto-select line QC: $qcLine (bisa diubah)."
            } else {
                "Part akan muncul otomatis sesuai line."
            }
        }

    val selectedLineOption: DropdownOption?
        get() = selectedLine?.let { DropdownOption(it.id, it.name) }

    val shiftLabel: String
        get() = selectedShift?.let { "${it.code} • ${it.name}" } ?: "Shift 1 (08:00-17:00 WIB)"

    val summaryTotals: SummaryTotals
        get() {
            val partsForLine = partsForLine()
            val totals = partsForLine.map { partIdSummary(it.id) }
            val totalCheck = totals.sumOf { it.totalCheck }
            val totalDefect = totals.sumOf { it.totalDefect }
            val totalOk = totals.sumOf { it.totalOk }
            val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
            return SummaryTotals(
                totalCheck = totalCheck,
                totalDefect = totalDefect,
                totalOk = totalOk,
                ngRatio = ratio,
                totalParts = partsForLine.size,
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

    val canSave: Boolean
        get() = selectedLine != null && filledPartSummaries.isNotEmpty() && !hasInvalidTotals

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

    fun partStatus(partId: Long): PartInputStatus =
        run {
            val summary = partIdSummary(partId)
            val hasData = summary.totalCheck > 0 || summary.totalDefect > 0
            when {
                !hasData -> PartInputStatus.EMPTY
                isTotalCheckInvalid(partId) -> PartInputStatus.ERROR
                summary.totalDefect > 0 -> PartInputStatus.COMPLETE
                summary.totalCheck > 0 -> PartInputStatus.COMPLETE
                else -> PartInputStatus.EMPTY
            }
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
        if (selectedLine == null || selectedShift == null) {
            feedback = UserFeedback(FeedbackType.ERROR, "Pilih line produksi terlebih dahulu.")
            return
        }
        if (filledPartSummaries.isEmpty()) {
            feedback = UserFeedback(FeedbackType.ERROR, "Isi minimal satu part sebelum disimpan.")
            return
        }
        if (hasInvalidTotals) {
            feedback = UserFeedback(FeedbackType.ERROR, "Periksa total periksa yang kurang dari total NG.")
            return
        }
        showConfirmDialog = true
    }

    fun isDuplicateAllowed(): Boolean = dependencies.policies.getAllowDuplicate.execute()

    fun dismissConfirm() {
        showConfirmDialog = false
    }

    fun clearFeedback() {
        feedback = null
    }

    fun onConfirmSave(onRecordsSaved: (List<InspectionRecord>) -> Unit) {
        val inputs = buildInputs()
        val allowDuplicate = dependencies.policies.getAllowDuplicate.execute()
        val result =
            dependencies.createBatchInspectionUseCase.execute(
                inputs = inputs,
                allowDuplicateSameDay = allowDuplicate,
            )
        val failedDetails =
            result.failedParts.mapNotNull { failed ->
                val part = partsForLine().firstOrNull { it.id == failed.partId } ?: return@mapNotNull null
                val reason = failed.feedback.message
                "• ${part.partNumber} ${part.name} — $reason"
            }
        feedback =
            if (failedDetails.isNotEmpty()) {
                val type =
                    if (result.feedback.type == FeedbackType.ERROR) {
                        FeedbackType.ERROR
                    } else {
                        FeedbackType.WARNING
                    }
                UserFeedback(
                    type,
                    "Sebagian data gagal disimpan:\n${failedDetails.joinToString("\n")}",
                )
            } else {
                result.feedback
            }
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
                    kind = id.co.nierstyd.mutugemba.domain.InspectionKind.DEFECT,
                    lineId = line.id,
                    shiftId = shift.id,
                    partId = part.id,
                    totalCheck = totalCheck,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = defectEntries,
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
                        id.co.nierstyd.mutugemba.domain
                            .InspectionDefectSlot(slot, quantity)
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
private fun SectionLabel(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.subtitle1)
        Text(text = subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
    }
}

@Composable
private fun ProcessStepsRow() {
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
            StepItem(step = "1", label = "Pilih Line")
            StepDivider()
            StepItem(step = "2", label = "Isi Checksheet")
            StepDivider()
            StepItem(step = "3", label = "Konfirmasi")
        }
    }
}

@Composable
private fun StepItem(
    step: String,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        AppBadge(
            text = step,
            backgroundColor = BrandBlue,
            contentColor = NeutralSurface,
        )
        Text(text = label, style = MaterialTheme.typography.body2, color = NeutralText)
    }
}

@Composable
private fun StepDivider() {
    Text(text = ">", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
}

@Composable
private fun InspectionSelectorCard(
    lineOptions: List<DropdownOption>,
    selectedLineOption: DropdownOption?,
    onLineSelected: (DropdownOption) -> Unit,
    lineHint: String,
    allowDuplicate: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppRadioGroup(
                label = "Line Produksi",
                options = lineOptions,
                selectedId = selectedLineOption?.id,
                onSelected = onLineSelected,
                helperText = lineHint,
            )
            androidx.compose.material.Divider(color = NeutralBorder, thickness = 1.dp)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Panduan Singkat", style = MaterialTheme.typography.subtitle2)
                InspectionDataHint()
                DuplicateRuleHint(allowDuplicate = allowDuplicate)
            }
        }
    }
}

@Composable
private fun DuplicateRuleHint(allowDuplicate: Boolean) {
    val label = if (allowDuplicate) "Duplikat Diizinkan" else "Duplikat Diblokir"
    val color = if (allowDuplicate) StatusWarning else StatusSuccess
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(
            text = label,
            backgroundColor = color,
            contentColor = NeutralSurface,
        )
        Text(
            text = "Aturan input ulang hari yang sama ${if (allowDuplicate) "tidak aktif" else "aktif"}.",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
    }
}

@Composable
private fun InspectionDataHint() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(
            text = "INFO",
            backgroundColor = NeutralLight,
            contentColor = NeutralText,
        )
        Text(
            text = "Part dan jenis NG diambil dari master data. Hubungi admin bila ada perubahan.",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
    }
}

@Composable
private fun SummaryStickyBar(summary: SummaryTotals) {
    Surface(
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "Ringkasan Checksheet",
                style = MaterialTheme.typography.subtitle1,
                color = NeutralText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = "Total Periksa", value = summary.totalCheck.toString())
                SummaryStat(title = "Total NG", value = summary.totalDefect.toString())
                SummaryStat(title = "Total OK", value = summary.totalOk.toString())
                SummaryStat(title = "Rasio NG", value = formatPercent(summary.ngRatio))
            }
            val okRatio =
                if (summary.totalCheck > 0) {
                    summary.totalOk.toFloat() / summary.totalCheck.toFloat()
                } else {
                    0f
                }
            val ngRatio =
                if (summary.totalCheck > 0) {
                    summary.totalDefect.toFloat() / summary.totalCheck.toFloat()
                } else {
                    0f
                }
            SummaryRatioBar(
                leftLabel = "OK",
                rightLabel = "NG",
                leftRatio = okRatio,
                rightRatio = ngRatio,
            )
        }
    }
}

@Composable
private fun SummaryRatioBar(
    leftLabel: String,
    rightLabel: String,
    leftRatio: Float,
    rightRatio: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(NeutralBorder, MaterialTheme.shapes.small),
        ) {
            if (leftRatio > 0f) {
                Box(
                    modifier =
                        Modifier
                            .weight(leftRatio)
                            .fillMaxHeight()
                            .background(StatusSuccess),
                )
            }
            if (rightRatio > 0f) {
                Box(
                    modifier =
                        Modifier
                            .weight(rightRatio)
                            .fillMaxHeight()
                            .background(StatusError),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = leftLabel, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Text(text = rightLabel, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
                "Periksa master part di pengaturan atau pilih line lain.",
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
    defectSummaries: List<PartSummaryRow>,
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
                    "Pastikan data sudah benar. Setelah disimpan, data tidak bisa diubah.",
                    style = MaterialTheme.typography.body2,
                )
                ConfirmSummaryChart(summaryTotals = summaryTotals)
                ConfirmTotalsCard(summaryTotals = summaryTotals)
                Text("Part Terisi", style = MaterialTheme.typography.subtitle1)
                defectSummaries.forEach { row ->
                    ConfirmPartRow(
                        title = "${row.partNumber} • ${row.partName}",
                        badges =
                            listOf(
                                BadgeSpec("OK ${row.totalOk}", StatusSuccess, NeutralSurface),
                                BadgeSpec("NG ${row.totalDefect}", StatusError, NeutralSurface),
                            ),
                    )
                }
                ConfirmNoticeRow()
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

@Composable
private fun ConfirmSummaryChart(summaryTotals: SummaryTotals) {
    val total = summaryTotals.totalCheck.coerceAtLeast(1)
    val okRatio = summaryTotals.totalOk.toFloat() / total.toFloat()
    val ngRatio = summaryTotals.totalDefect.toFloat() / total.toFloat()
    ChartBar(
        title = "Komposisi OK vs NG",
        segments =
            listOf(
                ChartSegment(
                    label = "OK",
                    ratio = okRatio,
                    color = id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess,
                ),
                ChartSegment(
                    label = "NG",
                    ratio = ngRatio,
                    color = id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError,
                ),
            ),
    )
}

private data class BadgeSpec(
    val text: String,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun ConfirmTotalsCard(summaryTotals: SummaryTotals) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ConfirmStatItem(label = "Total Periksa", value = summaryTotals.totalCheck.toString())
            ConfirmStatItem(label = "Total NG", value = summaryTotals.totalDefect.toString())
            ConfirmStatItem(label = "Total OK", value = summaryTotals.totalOk.toString())
        }
    }
}

@Composable
private fun RowScope.ConfirmStatItem(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
private fun ConfirmPartRow(
    title: String,
    badges: List<BadgeSpec>,
    subtitle: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = title, style = MaterialTheme.typography.body2, color = NeutralText)
                subtitle?.let {
                    Text(text = it, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                badges.forEach { badge ->
                    AppBadge(
                        text = badge.text,
                        backgroundColor = badge.backgroundColor,
                        contentColor = badge.contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmNoticeRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(
            text = "FINAL",
            backgroundColor = StatusWarning,
            contentColor = NeutralSurface,
        )
        Text(
            text = "Data yang disimpan akan dianggap final.",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
    }
}

private data class ChartSegment(
    val label: String,
    val ratio: Float,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
private fun ChartBar(
    title: String,
    segments: List<ChartSegment>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(NeutralBorder, MaterialTheme.shapes.small),
        ) {
            segments.forEach { segment ->
                if (segment.ratio > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .weight(segment.ratio)
                                .fillMaxHeight()
                                .background(segment.color),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            segments.forEach { segment ->
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
    }
}

private fun formatPercent(value: Double): String =
    if (value <= 0.0) {
        "-"
    } else {
        "${"%.1f".format(value * 100)}%"
    }
