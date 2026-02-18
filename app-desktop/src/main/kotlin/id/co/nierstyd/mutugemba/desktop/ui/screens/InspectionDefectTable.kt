package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.AppNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.CompactNumberField
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
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
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Part
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

@Composable
@Suppress("LongParameterList")
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
    customDefectInput: String,
    onCustomDefectInputChanged: (String) -> Unit,
    onAddCustomDefect: () -> Unit,
    currentLine: String,
    onDefectSlotChanged: (Long, InspectionTimeSlot, String) -> Unit,
    availableDefectTypes: List<DefectType>,
    onAddDefectToPart: (Long) -> Unit,
    onMoveDefectUp: (Long) -> Unit,
    onMoveDefectDown: (Long) -> Unit,
    onRemoveDefectFromPart: (Long) -> Unit,
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

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
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
                        onMoveUp = onMoveDefectUp,
                        onMoveDown = onMoveDefectDown,
                        onRemove = onRemoveDefectFromPart,
                    )
                    InlineCustomDefectRow(
                        value = customDefectInput,
                        onValueChange = onCustomDefectInputChanged,
                        onAdd = onAddCustomDefect,
                        currentLine = currentLine,
                        availableDefects = availableDefectTypes,
                        onAddExisting = onAddDefectToPart,
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
            verticalAlignment = Alignment.Top,
        ) {
            PartImage(
                picturePath = part.picturePath,
                uniqCode = part.uniqCode,
                partNumber = part.partNumber,
            )
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
            Surface(
                modifier = Modifier.width(196.dp),
                color = NeutralLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                shape = MaterialTheme.shapes.small,
                elevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        }
                        Icon(
                            imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppBadge(
                            text = if (expanded) "Terbuka" else "Tertutup",
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colors.primary,
                        )
                        Text(
                            text = "Klik kartu untuk ${if (expanded) "menyembunyikan" else "membuka"} detail",
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                    }
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
private fun PartImage(
    picturePath: String?,
    uniqCode: String,
    partNumber: String,
) {
    val bitmap = rememberImageBitmap(path = picturePath, uniqCode = uniqCode, partNumber = partNumber)
    Surface(
        modifier = Modifier.width(152.dp).height(120.dp),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(Spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = AppIcons.Inventory,
                        contentDescription = null,
                        tint = NeutralTextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(AppStrings.Inspection.PartImagePlaceholder, color = NeutralTextMuted)
                }
            } else {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = AppStrings.Inspection.PartImageDescription,
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun InlineCustomDefectRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    currentLine: String,
    availableDefects: List<DefectType>,
    onAddExisting: (Long) -> Unit,
) {
    var selectedExisting by remember(availableDefects) { mutableStateOf<DropdownOption?>(null) }
    val existingOptions =
        remember(availableDefects) {
            availableDefects.map { defect ->
                DropdownOption(
                    id = defect.id,
                    label = normalizeDefectName(defect.name),
                )
            }
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Tambah Jenis NG ($currentLine)",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                AppDropdown(
                    label = "Pilih dari daftar Jenis NG",
                    options = existingOptions,
                    selectedOption = selectedExisting,
                    onSelected = { selectedExisting = it },
                    placeholder = "Pilih Jenis NG existing",
                    enabled = existingOptions.isNotEmpty(),
                    helperText =
                        if (existingOptions.isEmpty()) {
                            "Semua Jenis NG sudah aktif untuk part ini."
                        } else {
                            "Pilih untuk menambah cepat tanpa mengetik ulang."
                        },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DefectActionIcon(
                        icon = AppIcons.Add,
                        contentDescription = "Tambah Jenis NG existing",
                        enabled = selectedExisting != null,
                        onClick = {
                            selectedExisting?.let { selected ->
                                onAddExisting(selected.id)
                                selectedExisting = null
                            }
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Atau buat Jenis NG baru",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    placeholder = { Text(AppStrings.Inspection.CustomDefectPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DefectActionIcon(
                        icon = AppIcons.Add,
                        contentDescription = "Tambah Jenis NG baru",
                        enabled = value.isNotBlank(),
                        onClick = onAdd,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberImageBitmap(
    path: String?,
    uniqCode: String,
    partNumber: String,
): ImageBitmap? =
    remember(path, uniqCode, partNumber) {
        try {
            val normalizedInput = path?.replace('\\', '/')
            val direct =
                runCatching {
                    normalizedInput?.let { Paths.get(it) }
                }.getOrNull()
            val cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
            val extractedRoot = AppDataPaths.defaultPartAssetsExtractedDir()
            val projectExtractedRoot = AppDataPaths.projectPartAssetsDir().resolve("extracted")
            val searchRoots =
                listOf(
                    extractedRoot.resolve("assets").resolve("images"),
                    projectExtractedRoot.resolve("assets").resolve("images"),
                ).distinct()
            val extractedFallback =
                searchRoots.firstNotNullOfOrNull { root ->
                    findExtractedImageCandidate(root, uniqCode, partNumber)
                }
            val candidates =
                listOfNotNull(
                    direct?.takeIf { it.isAbsolute },
                    normalizedInput?.let { AppDataPaths.dataDir().resolve(it) },
                    normalizedInput?.let { AppDataPaths.assetsStoreDir().resolve(it) },
                    normalizedInput?.let { AppDataPaths.attachmentsDir().resolve(it) },
                    normalizedInput?.let { extractedRoot.resolve(it) },
                    normalizedInput?.let { projectExtractedRoot.resolve(it) },
                    normalizedInput?.let { cwd.resolve(it).normalize() },
                    extractedFallback,
                ).distinct()

            val existing =
                candidates.firstOrNull { candidate ->
                    Files.exists(candidate) && Files.isRegularFile(candidate)
                } ?: return@remember null
            val bytes = Files.readAllBytes(existing)
            org.jetbrains.skia.Image
                .makeFromEncoded(bytes)
                .toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

private fun findExtractedImageCandidate(
    imageSearchRoot: Path,
    uniqCode: String,
    partNumber: String,
): Path? {
    if (!Files.exists(imageSearchRoot)) return null
    val uniqToken = "(${uniqCode.trim()})".uppercase()
    val partToken = partNumber.trim().uppercase()
    Files.walk(imageSearchRoot, 8).use { stream ->
        val matched =
            stream
                .filter { Files.isRegularFile(it) && it.name.lowercase().endsWith(".png") }
                .filter {
                    val name = it.name.uppercase()
                    name.contains(uniqToken) || (partToken.isNotBlank() && name.contains(partToken))
                }.findFirst()
                .orElse(null)
        return matched
    }
}

@Composable
@Suppress("LongParameterList")
private fun DefectTableGrid(
    defectTypes: List<DefectType>,
    timeSlots: List<InspectionTimeSlot>,
    values: Map<PartDefectSlotKey, String>,
    partId: Long,
    onValueChange: (Long, InspectionTimeSlot, String) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemove: (Long) -> Unit,
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
                defectTypes.forEachIndexed { index, defect ->
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
                        TableActionCell(weight = 0.85f) {
                            DefectActionIcon(
                                icon = AppIcons.ArrowUp,
                                contentDescription = "Geser ke atas",
                                enabled = index > 0,
                                onClick = { onMoveUp(defect.id) },
                            )
                            DefectActionIcon(
                                icon = AppIcons.ArrowDown,
                                contentDescription = "Geser ke bawah",
                                enabled = index < defectTypes.lastIndex,
                                onClick = { onMoveDown(defect.id) },
                            )
                            DefectActionIcon(
                                icon = AppIcons.Delete,
                                contentDescription = "Hapus Jenis NG",
                                enabled = defectTypes.size > 1,
                                onClick = { onRemove(defect.id) },
                                tint = StatusError,
                            )
                        }
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
            TableFooterCell(text = "", weight = 0.85f)
        }
    }
}

private fun normalizeDefectName(raw: String): String = DefectNameSanitizer.normalizeDisplay(raw).ifBlank { raw.trim() }

@Composable
private fun TableHeaderRow(timeSlots: List<InspectionTimeSlot>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TableHeaderCell(text = AppStrings.Inspection.TableDefectType, weight = 1.4f)
        timeSlots.forEach { slot ->
            TableHeaderCell(text = slot.label, weight = 1f)
        }
        TableHeaderCell(text = AppStrings.Inspection.TableTotal, weight = 0.7f)
        TableHeaderCell(text = "Aksi", weight = 0.85f)
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

@Composable
private fun RowScope.TableActionCell(
    weight: Float,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(horizontal = Spacing.xs, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun DefectActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colors.primary,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(30.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else NeutralTextMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}
