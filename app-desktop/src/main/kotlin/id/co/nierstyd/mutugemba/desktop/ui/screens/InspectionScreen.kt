package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionInputDefaults
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.InspectionDefaults
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

@Composable
private fun InspectionScreenContent(
    state: InspectionFormState,
    onRecordsSaved: (List<InspectionRecord>) -> Unit,
) {
    val parts = state.partsForLine()
    val feedback = state.feedback
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var showSearchModal by remember { mutableStateOf(false) }
    var showSummaryPanel by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = state.searchCandidates(searchQuery).take(8)
    val partItemStartIndex = 9

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(4000)
            state.clearFeedback()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val focusFirstResult by rememberUpdatedState(
        newValue = {
            searchResults.firstOrNull()?.let { part ->
                showSearchModal = false
                searchQuery = part.uniqCode
                state.focusPart(part.id)
                val targetIndex = parts.indexOfFirst { it.id == part.id }
                if (targetIndex >= 0) {
                    scope.launch {
                        listState.animateScrollToItem((partItemStartIndex + targetIndex).coerceAtLeast(0))
                    }
                }
            }
        },
    )
    val toggleSearch by rememberUpdatedState(newValue = { showSearchModal = !showSearchModal })

    DisposableEffect(Unit) {
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher =
            KeyEventDispatcher { event ->
                if (event.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
                if (event.isControlDown && event.keyCode == KeyEvent.VK_K) {
                    toggleSearch()
                    return@KeyEventDispatcher true
                }
                if (showSearchModal && event.keyCode == KeyEvent.VK_ENTER) {
                    focusFirstResult()
                    return@KeyEventDispatcher true
                }
                if (showSearchModal && event.keyCode == KeyEvent.VK_ESCAPE) {
                    showSearchModal = false
                    return@KeyEventDispatcher true
                }
                false
            }
        manager.addKeyEventDispatcher(dispatcher)
        onDispose { manager.removeKeyEventDispatcher(dispatcher) }
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.K) {
                        showSearchModal = !showSearchModal
                        true
                    } else if (event.type == KeyEventType.KeyDown && showSearchModal && event.key == Key.Enter) {
                        focusFirstResult()
                        true
                    } else if (event.type == KeyEventType.KeyDown && showSearchModal && event.key == Key.Escape) {
                        showSearchModal = false
                        true
                    } else {
                        false
                    }
                },
    ) {
        val viewportMaxHeight = if (maxHeight != Dp.Infinity) maxHeight else 900.dp
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = viewportMaxHeight)
                    .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                SectionHeader(
                    title = AppStrings.Inspection.Title,
                    subtitle = AppStrings.Inspection.Subtitle,
                )
            }

            item {
                InspectionIntroCard()
            }

            item {
                SectionLabel(
                    title = AppStrings.Inspection.ContextTitle,
                    subtitle = AppStrings.Inspection.ContextSubtitle,
                )
            }

            item {
                HeaderContextCard(
                    dateLabel = DateTimeFormats.formatDate(state.today),
                    shiftLabel = state.shiftLabel,
                    picName = state.picName,
                )
            }

            item {
                StatusBanner(
                    feedback =
                        UserFeedback(
                            FeedbackType.INFO,
                            AppStrings.Inspection.ContextBanner,
                        ),
                )
            }

            item {
                SectionLabel(
                    title = AppStrings.Inspection.LineTitle,
                    subtitle = AppStrings.Inspection.LineSubtitle,
                )
            }

            item {
                InspectionSelectorCard(
                    lineName = state.selectedLineName,
                    lineCode = state.selectedLineCodeName,
                    lineHint = state.lineHint,
                    allowDuplicate = state.isDuplicateAllowed(),
                )
            }

            item {
                SectionLabel(
                    title = AppStrings.Inspection.PartTitle,
                    subtitle = AppStrings.Inspection.PartSubtitle,
                )
            }

            if (parts.isEmpty()) {
                item {
                    if (state.isMasterLoading) {
                        InspectionLoadingState()
                    } else {
                        EmptyPartState()
                    }
                }
            } else {
                items(items = parts, key = { it.id }) { part ->
                    PartChecksheetCard(
                        part = part,
                        defectTypes = state.defectTypesForPart(part.id),
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
                        customDefectInput = state.customDefectInput,
                        onCustomDefectInputChanged = state::onCustomDefectInputChanged,
                        onAddCustomDefect = state::addCustomDefectType,
                        currentLine = state.selectedLineName,
                        onDefectSlotChanged = { defectId, slot, value ->
                            state.onDefectSlotChanged(part.id, defectId, slot, value)
                        },
                        availableDefectTypes = state.availableDefectTypesForPart(part.id),
                        onAddDefectToPart = { state.addDefectToPart(part.id, it) },
                        onMoveDefectUp = { state.moveDefectUp(part.id, it) },
                        onMoveDefectDown = { state.moveDefectDown(part.id, it) },
                        onRemoveDefectFromPart = { state.removeDefectFromPart(part.id, it) },
                    )
                }
            }

            item {
                state.feedback?.let { StatusBanner(feedback = it) }
            }
        }

        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            color = NeutralSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
            elevation = 2.dp,
        ) {
            InspectionActionsBar(
                canSave = state.canSave,
                onClearAll = { state.clearAllInputs() },
            )
        }

        InspectionFloatingActions(
            showSummaryPanel = showSummaryPanel,
            summary = state.summaryTotals,
            onToggleSummary = { showSummaryPanel = !showSummaryPanel },
            onOpenSearch = { showSearchModal = true },
            canSave = state.canSave,
            onConfirmSave = { state.onSaveRequested() },
        )
    }

    InspectionSearchModal(
        open = showSearchModal,
        query = searchQuery,
        results = searchResults,
        onQueryChange = { searchQuery = it },
        onClose = { showSearchModal = false },
        onPickPart = { part ->
            showSearchModal = false
            searchQuery = part.uniqCode
            state.focusPart(part.id)
            val targetIndex = parts.indexOfFirst { it.id == part.id }
            if (targetIndex >= 0) {
                scope.launch {
                    listState.animateScrollToItem((partItemStartIndex + targetIndex).coerceAtLeast(0))
                }
            }
        },
    )

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
    var customDefectInput by mutableStateOf("")
        private set
    var isMasterLoading by mutableStateOf(false)
        private set

    var feedback by mutableStateOf<UserFeedback?>(null)
        private set
    var showConfirmDialog by mutableStateOf(false)
        private set

    val defectSlotInputs = mutableStateMapOf<PartDefectSlotKey, String>()
    private val totalCheckInputs = mutableStateMapOf<Long, String>()
    private val expandedPartIds = mutableStateMapOf<Long, Boolean>()
    private val partDefectOverrides = mutableStateMapOf<Long, List<Long>>()

    val today = java.time.LocalDate.now()

    val lineHint: String
        get() {
            val qcLine =
                defaults.qcLineId?.let { id ->
                    lines.firstOrNull { it.id == id }?.name
                }
            return if (qcLine != null) {
                AppStrings.Inspection.lineHintQc(qcLine)
            } else {
                AppStrings.Inspection.LineHintDefault
            }
        }

    val picName: String = InspectionInputDefaults.DEFAULT_PIC_NAME

    val selectedLineName: String
        get() = selectedLine?.name ?: AppStrings.Common.Placeholder

    val selectedLineCodeName: String
        get() = selectedLine?.code?.label ?: AppStrings.Common.Placeholder

    val shiftLabel: String
        get() = selectedShift?.let { "${it.code} - ${it.name}" } ?: AppStrings.Inspection.DefaultShiftLabel

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

    suspend fun loadMasterData() {
        isMasterLoading = true
        val loaded =
            withContext(Dispatchers.Default) {
                MasterSnapshot(
                    lines = dependencies.masterData.getLines.execute(),
                    shifts = dependencies.masterData.getShifts.execute(),
                    parts = dependencies.masterData.getParts.execute(),
                    defectTypes = dependencies.masterData.getDefectTypes.execute(),
                )
            }
        lines = loaded.lines
        shifts = loaded.shifts
        parts = loaded.parts
        defectTypes = loaded.defectTypes
        partDefectOverrides.clear()
        partDefectOverrides.putAll(dependencies.defectLayout.getLayout.execute())
        syncSelections()
        ensureInputs()
        persistDefectLayout()
        isMasterLoading = false
    }

    fun partsForLine(): List<Part> {
        val line = selectedLine
        return if (line == null) {
            emptyList()
        } else {
            parts.filter { it.lineCode == line.code }
        }
    }

    fun searchCandidates(rawQuery: String): List<Part> {
        val keyword = rawQuery.trim().lowercase()
        val lineParts = partsForLine()
        if (keyword.isBlank()) {
            return lineParts.sortedBy { it.uniqCode }
        }
        return lineParts
            .filter { part ->
                part.uniqCode.lowercase().contains(keyword) ||
                    part.partNumber.lowercase().contains(keyword) ||
                    part.name.lowercase().contains(keyword) ||
                    part.material.lowercase().contains(keyword)
            }.sortedBy { it.uniqCode }
    }

    fun totalCheckInput(partId: Long): String = totalCheckInputs[partId] ?: ""

    @Suppress("ReturnCount")
    private fun baseDefectTypesForPart(partId: Long): List<DefectType> {
        val part = parts.firstOrNull { it.id == partId } ?: return defectTypes
        val mapped =
            part.recommendedDefectCodes
                .mapNotNull { code -> defectTypes.firstOrNull { it.code == code } }
                .distinctBy { it.id }
        val customForLine =
            defectTypes
                .filter { it.category == "CUSTOM" }
                .filter { it.lineCode == null || it.lineCode == part.lineCode }
                .sortedBy { it.name }
        if (mapped.isNotEmpty()) {
            return sanitizeDefectTypesForLine((mapped + customForLine).distinctBy { it.id }, part.lineCode)
        }
        return sanitizeDefectTypesForLine(
            defectTypes
                .filter { it.lineCode == null || it.lineCode == part.lineCode }
                .sortedBy { it.name },
            part.lineCode,
        )
    }

    fun defectTypesForPart(partId: Long): List<DefectType> {
        val overrideIds = partDefectOverrides[partId]
        if (overrideIds.isNullOrEmpty()) {
            return baseDefectTypesForPart(partId)
        }
        val byId = defectTypes.associateBy { it.id }
        val resolved = overrideIds.mapNotNull { byId[it] }
        return if (resolved.isEmpty()) baseDefectTypesForPart(partId) else resolved
    }

    fun availableDefectTypesForPart(partId: Long): List<DefectType> {
        val part = parts.firstOrNull { it.id == partId } ?: return emptyList()
        val active = defectTypesForPart(partId).map { it.id }.toSet()
        return sanitizeDefectTypesForLine(
            defectTypes
                .filter { it.lineCode == null || it.lineCode == part.lineCode }
                .filter { it.id !in active }
                .sortedBy { it.name },
            part.lineCode,
        )
    }

    fun addDefectToPart(
        partId: Long,
        defectId: Long,
    ) {
        val active = defectTypesForPart(partId).map { it.id }.toMutableList()
        if (defectId !in active) {
            active += defectId
            partDefectOverrides[partId] = active
            ensureInputs()
            persistDefectLayout()
        }
    }

    fun removeDefectFromPart(
        partId: Long,
        defectId: Long,
    ) {
        val active = defectTypesForPart(partId).map { it.id }.toMutableList()
        if (active.size <= 1) return
        if (active.remove(defectId)) {
            partDefectOverrides[partId] = active
            ensureInputs()
            persistDefectLayout()
        }
    }

    fun moveDefectUp(
        partId: Long,
        defectId: Long,
    ) {
        val active = defectTypesForPart(partId).map { it.id }.toMutableList()
        val index = active.indexOf(defectId)
        if (index <= 0) return
        val previous = active[index - 1]
        active[index - 1] = active[index]
        active[index] = previous
        partDefectOverrides[partId] = active
        ensureInputs()
        persistDefectLayout()
    }

    fun moveDefectDown(
        partId: Long,
        defectId: Long,
    ) {
        val active = defectTypesForPart(partId).map { it.id }.toMutableList()
        val index = active.indexOf(defectId)
        if (index == -1 || index >= active.lastIndex) return
        val next = active[index + 1]
        active[index + 1] = active[index]
        active[index] = next
        partDefectOverrides[partId] = active
        ensureInputs()
        persistDefectLayout()
    }

    fun totalDefectQuantity(partId: Long): Int = defectTypesForPart(partId).sumOf { defectRowTotal(partId, it.id) }

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
        totalCheckInputs[partId] = sanitizeCountInput(value)
    }

    fun onDefectSlotChanged(
        partId: Long,
        defectId: Long,
        slot: InspectionTimeSlot,
        value: String,
    ) {
        defectSlotInputs[PartDefectSlotKey(partId, defectId, slot)] = sanitizeCountInput(value)
    }

    fun onCustomDefectInputChanged(value: String) {
        customDefectInput = value.take(80)
    }

    fun focusPart(partId: Long) {
        expandedPartIds.keys.toList().forEach { expandedPartIds[it] = false }
        expandedPartIds[partId] = true
    }

    fun addCustomDefectType() {
        val line =
            selectedLine
                ?: run {
                    feedback = UserFeedback(FeedbackType.ERROR, AppStrings.Inspection.ErrorLineRequired)
                    return
                }
        val normalized = customDefectInput.trim()
        if (normalized.isBlank()) {
            feedback = UserFeedback(FeedbackType.WARNING, AppStrings.Inspection.CustomDefectEmpty)
            return
        }
        dependencies.masterData.upsertDefectType.execute(normalized, line.code)
        defectTypes = dependencies.masterData.getDefectTypes.execute()
        ensureInputs()
        persistDefectLayout()
        customDefectInput = ""
        feedback = UserFeedback(FeedbackType.SUCCESS, AppStrings.Inspection.customDefectAdded(normalized))
    }

    fun isExpanded(partId: Long): Boolean = expandedPartIds[partId] ?: false

    fun toggleExpanded(partId: Long) {
        val current = expandedPartIds[partId] ?: false
        expandedPartIds[partId] = !current
    }

    fun onSaveRequested() {
        feedback = null
        if (selectedLine == null || selectedShift == null) {
            feedback = UserFeedback(FeedbackType.ERROR, AppStrings.Inspection.ErrorLineRequired)
            return
        }
        if (filledPartSummaries.isEmpty()) {
            feedback = UserFeedback(FeedbackType.ERROR, AppStrings.Inspection.ErrorPartRequired)
            return
        }
        if (hasInvalidTotals) {
            feedback = UserFeedback(FeedbackType.ERROR, AppStrings.Inspection.ErrorTotalCheckInvalid)
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
                AppStrings.Inspection.failedPartLine(part.partNumber, part.name, reason)
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
                    AppStrings.Inspection.partialSaveFailed(failedDetails.joinToString("\n")),
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
                    picName = picName,
                    createdAt = createdAt,
                )
            }
        }
    }

    private fun buildDefectEntries(partId: Long): List<InspectionDefectEntry> =
        defectTypesForPart(partId).mapNotNull { defect ->
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
        selectedLineId = resolveSelection(defaults.qcLineId, defaults.lineId, lines.map { it.id })
    }

    private fun ensureInputs() {
        val validPartIds = partsForLine().map { it.id }.toSet()
        totalCheckInputs.keys.filter { it !in validPartIds }.forEach { totalCheckInputs.remove(it) }
        expandedPartIds.keys.filter { it !in validPartIds }.forEach { expandedPartIds.remove(it) }
        defectSlotInputs.keys.filter { it.partId !in validPartIds }.forEach { defectSlotInputs.remove(it) }
        partDefectOverrides.keys.filter { it !in validPartIds }.forEach { partDefectOverrides.remove(it) }
        validPartIds.forEach { partId ->
            totalCheckInputs.putIfAbsent(partId, "")
            expandedPartIds.putIfAbsent(partId, false)
            val fallbackDefectIds = baseDefectTypesForPart(partId).map { it.id }
            val overrideIds = partDefectOverrides[partId].orEmpty()
            val validDefectIds = defectTypes.map { it.id }.toSet()
            val cleanedOverride = overrideIds.filter { it in validDefectIds }
            if (cleanedOverride.isEmpty()) {
                partDefectOverrides[partId] = fallbackDefectIds
            } else if (cleanedOverride.size != overrideIds.size) {
                partDefectOverrides[partId] = cleanedOverride
            }
            val activeDefectIds = defectTypesForPart(partId).map { it.id }.toSet()
            defectSlotInputs.keys
                .filter { it.partId == partId && it.defectTypeId !in activeDefectIds }
                .forEach { defectSlotInputs.remove(it) }
            activeDefectIds.forEach { defectId ->
                timeSlots.forEach { slot ->
                    defectSlotInputs.putIfAbsent(PartDefectSlotKey(partId, defectId, slot), "")
                }
            }
        }
        if (expandedPartIds.values.none { it } && validPartIds.isNotEmpty()) {
            expandedPartIds[validPartIds.first()] = true
        }
    }

    private fun sanitizeDefectTypesForLine(
        source: List<DefectType>,
        lineCode: LineCode,
    ): List<DefectType> {
        val byName = linkedMapOf<String, DefectType>()
        source.forEach { defect ->
            val display = DefectNameSanitizer.normalizeDisplay(defect.name)
            if (!DefectNameSanitizer.isMeaningfulItem(display)) return@forEach
            val key = DefectNameSanitizer.canonicalKey(display)
            if (key.isBlank()) return@forEach

            val normalized = defect.copy(name = display)
            val existing = byName[key]
            if (existing == null) {
                byName[key] = normalized
                return@forEach
            }
            if (defectLinePriority(normalized, lineCode) > defectLinePriority(existing, lineCode)) {
                byName[key] = normalized
            }
        }
        return byName.values.toList()
    }

    private fun defectLinePriority(
        defect: DefectType,
        lineCode: LineCode,
    ): Int =
        when (defect.lineCode) {
            lineCode -> 2
            null -> 1
            else -> 0
        }

    private fun saveDefaults() {
        dependencies.defaults.saveDefaults.execute(
            defaults.copy(
                lineId = selectedLine?.id,
                shiftId = selectedShift?.id,
            ),
        )
    }

    private fun persistDefectLayout() {
        dependencies.defectLayout.saveLayout.execute(partDefectOverrides.toMap())
    }
}

private data class MasterSnapshot(
    val lines: List<Line>,
    val shifts: List<Shift>,
    val parts: List<Part>,
    val defectTypes: List<DefectType>,
)

@Composable
private fun HeaderContextCard(
    dateLabel: String,
    shiftLabel: String,
    picName: String,
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
                Text(
                    text = AppStrings.Inspection.HeaderDate,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
                Text(text = dateLabel, style = MaterialTheme.typography.h6)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = AppStrings.Inspection.HeaderPic,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
                Text(text = picName, style = MaterialTheme.typography.subtitle1)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
                Text(
                    text = AppStrings.Inspection.HeaderShift,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
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
private fun InspectionIntroCard() {
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
            Text(text = AppStrings.Inspection.IntroTitle, style = MaterialTheme.typography.subtitle1)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                IntroStepRow(step = "1", text = AppStrings.Inspection.IntroStep1)
                IntroStepRow(step = "2", text = AppStrings.Inspection.IntroStep2)
                IntroStepRow(step = "3", text = AppStrings.Inspection.IntroStep3)
            }
        }
    }
}

@Composable
private fun IntroStepRow(
    step: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        AppBadge(
            text = step,
            backgroundColor = NeutralLight,
            contentColor = NeutralText,
        )
        Text(text = text, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
    }
}

@Composable
private fun InspectionSelectorCard(
    lineName: String,
    lineCode: String,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBadge(
                    text = "QC LINE AKTIF",
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colors.primary,
                )
                Text("$lineName ($lineCode)", style = MaterialTheme.typography.subtitle1, color = NeutralText)
            }
            Text(
                text = lineHint,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            androidx.compose.material.Divider(color = NeutralBorder, thickness = 1.dp)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralLight.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(text = "Panduan Singkat", style = MaterialTheme.typography.subtitle2, color = NeutralText)
                InspectionDataHint()
                DuplicateRuleHint(allowDuplicate = allowDuplicate)
                Text(
                    text = "Aksi cepat: Summary, Cari Part (Ctrl+K), dan Konfirmasi ada di kanan bawah.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = "Urutan/aktif Jenis NG per part akan langsung dipakai saat Konfirmasi & Simpan.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
    }
}

private fun sanitizeCountInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(5)
    if (digits.isBlank()) return ""
    return digits.trimStart('0').ifBlank { "0" }
}

@Composable
private fun BoxScope.InspectionFloatingActions(
    showSummaryPanel: Boolean,
    summary: SummaryTotals,
    onToggleSummary: () -> Unit,
    onOpenSearch: () -> Unit,
    canSave: Boolean,
    onConfirmSave: () -> Unit,
) {
    Column(
        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = Spacing.lg, end = Spacing.lg),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AnimatedVisibility(
            visible = showSummaryPanel,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.95f, animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.95f, animationSpec = tween(160)),
        ) {
            SummaryCompactPanel(summary = summary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FloatingActionButton(
                onClick = onToggleSummary,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = NeutralSurface,
            ) {
                Icon(imageVector = AppIcons.Assignment, contentDescription = AppStrings.Inspection.SummaryTitle)
            }
            FloatingActionButton(
                onClick = onOpenSearch,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = NeutralSurface,
            ) {
                Icon(imageVector = AppIcons.Search, contentDescription = AppStrings.Inspection.SearchPartLabel)
            }
            FloatingActionButton(
                onClick = { if (canSave) onConfirmSave() },
                backgroundColor = if (canSave) MaterialTheme.colors.primary else NeutralBorder,
                contentColor = NeutralSurface,
            ) {
                Icon(imageVector = AppIcons.CheckCircle, contentDescription = AppStrings.Actions.ConfirmSave)
            }
        }
    }
}

@Composable
private fun SummaryCompactPanel(summary: SummaryTotals) {
    Surface(
        modifier = Modifier.width(340.dp),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = AppStrings.Inspection.SummaryTitle, style = MaterialTheme.typography.subtitle1)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCompact(title = AppStrings.Inspection.TotalCheckLabel, value = summary.totalCheck.toString())
                SummaryStatCompact(title = AppStrings.Inspection.TotalNgLabel, value = summary.totalDefect.toString())
                SummaryStatCompact(title = AppStrings.Inspection.TotalOkLabel, value = summary.totalOk.toString())
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
                leftLabel = AppStrings.Inspection.SummaryRatioOk,
                rightLabel = AppStrings.Inspection.SummaryRatioNg,
                leftRatio = okRatio,
                rightRatio = ngRatio,
            )
        }
    }
}

@Composable
private fun InspectionSearchModal(
    open: Boolean,
    query: String,
    results: List<Part>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onPickPart: (Part) -> Unit,
) {
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(160)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(onClick = onClose),
        ) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 120.dp)
                        .width(620.dp)
                        .clickable(enabled = false) {},
                color = NeutralSurface,
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                elevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = AppStrings.Inspection.SearchPartLabel,
                                style = MaterialTheme.typography.subtitle1,
                            )
                            Text(
                                text = "Cari cepat berdasarkan UNIQ, part number, nama, atau material.",
                                style = MaterialTheme.typography.caption,
                                color = NeutralTextMuted,
                            )
                        }
                        AppBadge(
                            text = "Ctrl+K",
                            backgroundColor = NeutralLight,
                            contentColor = NeutralTextMuted,
                        )
                    }
                    AppTextField(
                        spec =
                            FieldSpec(
                                label = AppStrings.Inspection.SearchPartLabel,
                                placeholder = AppStrings.Inspection.SearchPartPlaceholder,
                                helperText = AppStrings.Inspection.SearchPartHint,
                            ),
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                    )
                    if (results.isEmpty()) {
                        Text(
                            text = "Part tidak ditemukan.",
                            style = MaterialTheme.typography.body2,
                            color = NeutralTextMuted,
                        )
                    } else {
                        Text(
                            text = "Hasil (${results.size}) - klik item atau tekan Enter untuk item teratas.",
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            results.forEachIndexed { index, part ->
                                SearchResultRow(
                                    part = part,
                                    hint = if (index == 0) "Tekan Enter untuk langsung fokus" else null,
                                    onClick = { onPickPart(part) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    part: Part,
    hint: String?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "(${part.uniqCode}) ${part.name}",
                style = MaterialTheme.typography.body1,
                color = NeutralText,
            )
            Text(
                text = "Part Number ${part.partNumber}",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption,
                    color = StatusInfo,
                )
            }
        }
    }
}

@Composable
private fun DuplicateRuleHint(allowDuplicate: Boolean) {
    val label = if (allowDuplicate) AppStrings.Inspection.DuplicateAllowed else AppStrings.Inspection.DuplicateBlocked
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
        val hint =
            if (allowDuplicate) {
                AppStrings.Inspection.DuplicateHintOn
            } else {
                AppStrings.Inspection.DuplicateHintOff
            }
        Text(
            text = hint,
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
            text = AppStrings.Inspection.MasterDataHintTitle,
            backgroundColor = NeutralLight,
            contentColor = NeutralText,
        )
        Text(
            text = AppStrings.Inspection.MasterDataHint,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = AppStrings.Inspection.SummaryTitle,
                            style = MaterialTheme.typography.subtitle1,
                            color = NeutralText,
                        )
                        Text(
                            text = AppStrings.Inspection.SummarySubtitle,
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                    }
                }
                AppBadge(
                    text = AppStrings.Inspection.SummaryAuto,
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralTextMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCompact(title = AppStrings.Inspection.TotalCheckLabel, value = summary.totalCheck.toString())
                SummaryStatCompact(title = AppStrings.Inspection.TotalNgLabel, value = summary.totalDefect.toString())
                SummaryStatCompact(title = AppStrings.Inspection.TotalOkLabel, value = summary.totalOk.toString())
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
                leftLabel = AppStrings.Inspection.SummaryRatioOk,
                rightLabel = AppStrings.Inspection.SummaryRatioNg,
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
                    .height(6.dp)
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
private fun RowScope.SummaryStatCompact(
    title: String,
    value: String,
) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.subtitle2, color = NeutralText)
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
            Text(AppStrings.Inspection.EmptyPartTitle, style = MaterialTheme.typography.body1)
            Text(
                AppStrings.Inspection.EmptyPartSubtitle,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun InspectionLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SkeletonBlock(width = 240.dp, height = 14.dp, color = NeutralLight)
            SkeletonBlock(width = 320.dp, height = 14.dp, color = NeutralLight)
            SkeletonBlock(width = 280.dp, height = 14.dp, color = NeutralLight)
        }
    }
}

@Composable
private fun InspectionActionsBar(
    canSave: Boolean,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryButton(
            text = AppStrings.Actions.ClearAll,
            onClick = onClearAll,
        )
        Spacer(modifier = Modifier.weight(1f))
        AppBadge(
            text = if (canSave) "Siap disimpan" else "Lengkapi data part",
            backgroundColor = if (canSave) StatusSuccess else NeutralLight,
            contentColor = if (canSave) NeutralSurface else NeutralTextMuted,
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(760.dp),
            color = NeutralSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(AppStrings.Inspection.ConfirmTitle, style = MaterialTheme.typography.h6)
                    Text(
                        AppStrings.Inspection.ConfirmSubtitle,
                        style = MaterialTheme.typography.body2,
                    )
                    ConfirmSummaryChart(summaryTotals = summaryTotals)
                    ConfirmTotalsCard(summaryTotals = summaryTotals)
                    Text(AppStrings.Inspection.ConfirmPartTitle, style = MaterialTheme.typography.subtitle1)
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        defectSummaries.forEach { row ->
                            ConfirmPartRow(
                                title = AppStrings.Inspection.partSummaryItem(row.partNumber, row.partName),
                                badges =
                                    listOf(
                                        BadgeSpec(
                                            "${AppStrings.Inspection.SummaryRatioOk} ${row.totalOk}",
                                            StatusSuccess,
                                            NeutralSurface,
                                        ),
                                        BadgeSpec(
                                            "${AppStrings.Inspection.SummaryRatioNg} ${row.totalDefect}",
                                            StatusError,
                                            NeutralSurface,
                                        ),
                                    ),
                            )
                        }
                    }
                    ConfirmNoticeRow()
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(NeutralLight)
                            .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                ) {
                    SecondaryButton(text = AppStrings.Actions.Cancel, onClick = onDismiss)
                    PrimaryButton(text = AppStrings.Actions.SaveNow, onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun ConfirmSummaryChart(summaryTotals: SummaryTotals) {
    val total = summaryTotals.totalCheck.coerceAtLeast(1)
    val okRatio = summaryTotals.totalOk.toFloat() / total.toFloat()
    val ngRatio = summaryTotals.totalDefect.toFloat() / total.toFloat()
    ChartBar(
        title = AppStrings.Inspection.ConfirmChartTitle,
        segments =
            listOf(
                ChartSegment(
                    label = AppStrings.Inspection.SummaryRatioOk,
                    ratio = okRatio,
                    color = id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess,
                ),
                ChartSegment(
                    label = AppStrings.Inspection.SummaryRatioNg,
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
            ConfirmStatItem(label = AppStrings.Inspection.TotalCheckLabel, value = summaryTotals.totalCheck.toString())
            ConfirmStatItem(label = AppStrings.Inspection.TotalNgLabel, value = summaryTotals.totalDefect.toString())
            ConfirmStatItem(label = AppStrings.Inspection.TotalOkLabel, value = summaryTotals.totalOk.toString())
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
            text = AppStrings.Inspection.ConfirmFinal,
            backgroundColor = StatusWarning,
            contentColor = NeutralSurface,
        )
        Text(
            text = AppStrings.Inspection.ConfirmFinalHint,
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
