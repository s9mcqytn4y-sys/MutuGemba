package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.CompactNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Part
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Composable
internal fun PartChecksheetCard(
    part: Part,
    defectTypes: List<DefectType>,
    timeSlots: List<InspectionTimeSlot>,
    totalCheckInput: String,
    totalDefect: Int,
    totalOk: Int,
    totalCheckInvalid: Boolean,
    defectSlotValues: Map<PartDefectSlotKey, String>,
    status: PartInputStatus,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTotalCheckChanged: (String) -> Unit,
    onDefectSlotChanged: (Long, InspectionTimeSlot, String) -> Unit,
) {
    val borderColor = if (expanded) MaterialTheme.colors.primary else NeutralBorder
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PartHeader(
                part = part,
                totalCheck = totalCheckInput,
                totalDefect = totalDefect,
                totalOk = totalOk,
                status = status,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
            )

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        AppNumberField(
                            spec =
                                FieldSpec(
                                    label = AppStrings.Inspection.TotalCheckLabel,
                                    placeholder = AppStrings.Inspection.TotalCheckPlaceholder,
                                    helperText =
                                        AppStrings.Inspection.totalCheckHelper(!totalCheckInvalid),
                                    isError = totalCheckInvalid,
                                ),
                            value = totalCheckInput,
                            onValueChange = onTotalCheckChanged,
                            modifier = Modifier.weight(1f),
                        )
                        PartStatChip(
                            title = AppStrings.Inspection.TotalNgLabel,
                            value = totalDefect.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        PartStatChip(
                            title = AppStrings.Inspection.TotalOkLabel,
                            value = totalOk.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    DefectTableGrid(
                        defectTypes = defectTypes,
                        timeSlots = timeSlots,
                        values = defectSlotValues,
                        partId = part.id,
                        onValueChange = onDefectSlotChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun PartHeader(
    part: Part,
    totalCheck: String,
    totalDefect: Int,
    totalOk: Int,
    status: PartInputStatus,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val hasInput = status != PartInputStatus.EMPTY
    val accentColor =
        when (status) {
            PartInputStatus.COMPLETE -> StatusSuccess
            PartInputStatus.WARNING -> StatusWarning
            PartInputStatus.ERROR -> StatusError
            PartInputStatus.INCOMPLETE -> NeutralBorder
            PartInputStatus.EMPTY -> NeutralBorder
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable { onToggleExpanded() }
                .background(if (expanded) NeutralLight else NeutralSurface),
    ) {
        Box(
            modifier =
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor),
        )
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PartImage(picturePath = part.picturePath)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "(${part.uniqCode}) ${part.name}", style = MaterialTheme.typography.h6)
                Text(
                    text = "Part Number ${part.partNumber}",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
                Text(
                    text = "Material ${part.material}",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                PartStatusBadge(status = status, hasInput = hasInput)
            }
            Column(
                modifier = Modifier.width(140.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = AppStrings.Inspection.CheckLabel,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = totalCheck.ifBlank { AppStrings.Common.Placeholder },
                    style = MaterialTheme.typography.subtitle1,
                )
                Text(
                    text = AppStrings.Inspection.partTotals(totalDefect, totalOk),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppStrings.Inspection.toggleDetail(expanded),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                    Icon(
                        imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PartStatusBadge(
    status: PartInputStatus,
    hasInput: Boolean,
) {
    val (label, color, textColor) =
        when (status) {
            PartInputStatus.COMPLETE -> Triple(AppStrings.Inspection.PartStatusComplete, StatusSuccess, NeutralSurface)
            PartInputStatus.WARNING -> Triple(AppStrings.Inspection.PartStatusWarning, StatusWarning, NeutralSurface)
            PartInputStatus.ERROR -> Triple(AppStrings.Inspection.PartStatusError, StatusError, NeutralSurface)
            PartInputStatus.INCOMPLETE -> Triple(AppStrings.Inspection.PartStatusPartial, NeutralBorder, NeutralText)
            PartInputStatus.EMPTY ->
                if (hasInput) {
                    Triple(AppStrings.Inspection.PartStatusPartial, NeutralBorder, NeutralText)
                } else {
                    Triple(AppStrings.Inspection.PartStatusNotProduced, NeutralBorder, NeutralText)
                }
        }
    AppBadge(
        text = label,
        backgroundColor = color,
        contentColor = textColor,
    )
}

@Composable
private fun PartStatChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Text(text = value, style = MaterialTheme.typography.subtitle1, color = NeutralText)
        }
    }
}

@Composable
private fun PartImage(picturePath: String?) {
    val bitmap = rememberImageBitmap(picturePath)
    Surface(
        modifier = Modifier.width(128.dp).height(104.dp),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap == null) {
                Text(AppStrings.Inspection.PartImagePlaceholder, color = NeutralTextMuted)
            } else {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = AppStrings.Inspection.PartImageDescription,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.xs),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun rememberImageBitmap(path: String?): ImageBitmap? =
    remember(path) {
        try {
            if (path.isNullOrBlank()) {
                null
            } else {
                val normalized = path.replace('\\', '/')
                val direct = Paths.get(path)
                val candidates =
                    listOf(
                        if (direct.isAbsolute) direct else null,
                        AppDataPaths.dataDir().resolve(normalized),
                        AppDataPaths.attachmentsDir().resolve(normalized),
                        Paths.get(System.getProperty("user.dir", "."), normalized).normalize(),
                        File(System.getProperty("user.dir", "."), normalized).toPath().normalize(),
                    ).filterNotNull().distinct()
                val existing = candidates.firstOrNull { Files.exists(it) }
                if (existing != null) {
                    val bytes = Files.readAllBytes(existing)
                    org.jetbrains.skia.Image
                        .makeFromEncoded(bytes)
                        .toComposeImageBitmap()
                } else {
                    null
                }
            }
        } catch (ex: Exception) {
            null
        }
    }

@Composable
private fun DefectTableGrid(
    defectTypes: List<DefectType>,
    timeSlots: List<InspectionTimeSlot>,
    values: Map<PartDefectSlotKey, String>,
    partId: Long,
    onValueChange: (Long, InspectionTimeSlot, String) -> Unit,
) {
    fun slotValue(
        defectId: Long,
        slot: InspectionTimeSlot,
    ): String = values[PartDefectSlotKey(partId, defectId, slot)] ?: ""

    fun slotQuantity(
        defectId: Long,
        slot: InspectionTimeSlot,
    ): Int = slotValue(defectId, slot).toIntOrNull() ?: 0

    fun rowTotal(defectId: Long): Int = timeSlots.sumOf { slotQuantity(defectId, it) }

    fun columnTotal(slot: InspectionTimeSlot): Int = defectTypes.sumOf { defect -> slotQuantity(defect.id, slot) }

    val totalNg = defectTypes.sumOf { defect -> rowTotal(defect.id) }

    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(timeSlots = timeSlots)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                defectTypes.forEach { defect ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TableCell(text = normalizeDefectName(defect.name), weight = 1.4f)
                        timeSlots.forEach { slot ->
                            TableInputCell(
                                value = slotValue(defect.id, slot),
                                onValueChange = { input -> onValueChange(defect.id, slot, input) },
                                weight = 1f,
                            )
                        }
                        TableCell(text = rowTotal(defect.id).toString(), weight = 0.7f, alignCenter = true)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TableFooterCell(text = AppStrings.Inspection.TableSubtotal, weight = 1.4f)
            timeSlots.forEach { slot ->
                TableFooterCell(text = columnTotal(slot).toString(), weight = 1f, alignCenter = true)
            }
            TableFooterCell(text = totalNg.toString(), weight = 0.7f, alignCenter = true)
        }
    }
}

private fun normalizeDefectName(raw: String): String {
    val compact = raw.trim().replace("\\s+".toRegex(), " ")
    return compact
        .replace("^\\([^)]+\\)\\s*".toRegex(), "")
        .replace("\\s+[A-Z]$".toRegex(), "")
}

@Composable
private fun TableHeaderRow(timeSlots: List<InspectionTimeSlot>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TableHeaderCell(text = AppStrings.Inspection.TableDefectType, weight = 1.4f)
        timeSlots.forEach { slot ->
            TableHeaderCell(text = slot.label, weight = 1f)
        }
        TableHeaderCell(text = AppStrings.Inspection.TableTotal, weight = 0.7f)
    }
}

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = NeutralText)
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text, color = NeutralText)
    }
}

@Composable
private fun RowScope.TableInputCell(
    value: String,
    onValueChange: (String) -> Unit,
    weight: Float,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        CompactNumberField(
            value = value,
            onValueChange = onValueChange,
            placeholder = AppStrings.Common.Zero,
        )
    }
}

@Composable
private fun RowScope.TableFooterCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text, color = NeutralText)
    }
}
