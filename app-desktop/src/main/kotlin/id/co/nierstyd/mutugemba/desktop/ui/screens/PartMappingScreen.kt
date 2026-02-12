package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import id.co.nierstyd.mutugemba.usecase.asset.GetActiveImageRefUseCase
import id.co.nierstyd.mutugemba.usecase.asset.LoadImageBytesUseCase
import id.co.nierstyd.mutugemba.usecase.part.GetPartDetailUseCase
import id.co.nierstyd.mutugemba.usecase.part.ObservePartsUseCase
import id.co.nierstyd.mutugemba.usecase.qa.GetDefectHeatmapUseCase
import id.co.nierstyd.mutugemba.usecase.qa.GetTopDefectsPerModelMonthlyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.time.YearMonth
import org.jetbrains.skia.Image as SkiaImage

data class PartMappingScreenDependencies(
    val observeParts: ObservePartsUseCase,
    val getPartDetail: GetPartDetailUseCase,
    val getTopDefects: GetTopDefectsPerModelMonthlyUseCase,
    val getDefectHeatmap: GetDefectHeatmapUseCase,
    val getActiveImageRef: GetActiveImageRefUseCase,
    val loadImageBytes: LoadImageBytesUseCase,
)

@Composable
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun PartMappingScreen(dependencies: PartMappingScreenDependencies) {
    val now = remember { YearMonth.now() }

    var lineCode by rememberSaveable { mutableStateOf("") }
    var modelCode by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var monthInput by rememberSaveable { mutableStateOf("%04d-%02d".format(now.year, now.monthValue)) }
    val selectedMonth = remember(monthInput) { parseYearMonth(monthInput) ?: now }
    val partFilterFlow =
        remember {
            MutableStateFlow(
                PartFilter(
                    lineCode = null,
                    modelCode = null,
                    search = null,
                    year = now.year,
                    month = now.monthValue,
                ),
            )
        }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lineCode, modelCode, search, selectedMonth) {
        partFilterFlow.value =
            PartFilter(
                lineCode = lineCode.takeIf { it.isNotBlank() }?.lowercase(),
                modelCode = modelCode.takeIf { it.isNotBlank() },
                search = search.takeIf { it.isNotBlank() },
                year = selectedMonth.year,
                month = selectedMonth.monthValue,
            )
    }

    val partsFlow =
        remember(partFilterFlow) {
            partFilterFlow
                .debounce(250)
                .flatMapLatest { filter -> dependencies.observeParts.execute(filter) }
                .catch { throwable ->
                    loadError = throwable.message ?: "Gagal memuat data part."
                    emit(emptyList())
                }
        }
    val parts by partsFlow.collectAsState(initial = emptyList())
    val catalogFlow =
        remember(selectedMonth) {
            dependencies.observeParts
                .execute(
                    PartFilter(
                        lineCode = null,
                        modelCode = null,
                        search = null,
                        year = selectedMonth.year,
                        month = selectedMonth.monthValue,
                    ),
                ).catch { emit(emptyList()) }
        }
    val catalogParts by catalogFlow.collectAsState(initial = emptyList())
    val lineOptions =
        remember(catalogParts) {
            listOf(DropdownOption(-1, "Semua")) +
                catalogParts
                    .map { it.lineCode }
                    .distinct()
                    .sorted()
                    .map { DropdownOption(it.hashCode().toLong(), it) }
        }
    val modelOptions =
        remember(catalogParts) {
            listOf(DropdownOption(-1, "Semua")) +
                catalogParts
                    .flatMap { it.modelCodes }
                    .distinct()
                    .sorted()
                    .map { DropdownOption(it.hashCode().toLong(), it) }
        }

    var selectedUniqNo by rememberSaveable { mutableStateOf<String?>(null) }
    var partDetail by remember { mutableStateOf<PartDetail?>(null) }
    var partDetailLoading by remember { mutableStateOf(false) }

    LaunchedEffect(parts) {
        if (parts.isNotEmpty() && selectedUniqNo !in parts.map { it.uniqNo }) {
            selectedUniqNo = parts.first().uniqNo
        }
    }

    LaunchedEffect(selectedUniqNo, selectedMonth) {
        val uniq =
            selectedUniqNo ?: run {
                partDetail = null
                return@LaunchedEffect
            }
        partDetailLoading = true
        partDetail =
            withContext(Dispatchers.IO) {
                dependencies.getPartDetail.execute(uniq, selectedMonth.year, selectedMonth.monthValue)
            }
        partDetailLoading = false
    }

    val thumbnailMap = remember { mutableStateMapOf<String, ImageBitmap?>() }
    var thumbnailLoading by remember { mutableStateOf(false) }
    LaunchedEffect(parts) {
        val missing = parts.filterNot { thumbnailMap.containsKey(it.uniqNo) }
        if (missing.isEmpty()) return@LaunchedEffect
        thumbnailLoading = true
        val loaded =
            withContext(Dispatchers.IO) {
                missing.associate { part ->
                    val ref = dependencies.getActiveImageRef.execute(part.uniqNo)
                    val bytes = ref?.let { dependencies.loadImageBytes.execute(it) }
                    part.uniqNo to decodeImageBitmap(bytes)
                }
            }
        thumbnailMap.putAll(loaded)
        thumbnailLoading = false
    }

    LaunchedEffect(parts.size) {
        // Clear stale thumbnails gradually to avoid unbounded cache growth across long sessions.
        if (thumbnailMap.size > 260) {
            val active = parts.map { it.uniqNo }.toSet()
            thumbnailMap.keys
                .filter { it !in active }
                .take(80)
                .forEach { thumbnailMap.remove(it) }
        }
    }

    var detailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(partDetail?.image?.storageRelPath) {
        val image =
            partDetail?.image ?: run {
                detailBitmap = null
                return@LaunchedEffect
            }
        detailBitmap =
            withContext(Dispatchers.IO) {
                dependencies
                    .loadImageBytes
                    .execute(
                        AssetRef(
                            storageRelPath = image.storageRelPath,
                            sha256 = image.sha256,
                            mime = image.mime,
                            sizeBytes = image.sizeBytes,
                        ),
                    )?.let(::decodeImageBitmap)
            }
    }

    var topDefects by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var heatmapRows by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(selectedMonth, modelCode) {
        val top =
            withContext(Dispatchers.IO) {
                dependencies
                    .getTopDefects
                    .execute(selectedMonth.year, selectedMonth.monthValue, 5)
                    .map { item -> "${item.modelCode} - ${item.defectName}" to item.totalQty }
            }
        val heatmap =
            withContext(Dispatchers.IO) {
                dependencies
                    .getDefectHeatmap
                    .execute(
                        selectedMonth.year,
                        selectedMonth.monthValue,
                        modelCode.takeIf { it.isNotBlank() },
                    ).map { row ->
                        "${row.reportDate ?: "-"} | ${row.defectName} | ${row.totalQty}"
                    }
            }
        topDefects = top
        heatmapRows = heatmap
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.PartMapping.Title,
            subtitle = AppStrings.PartMapping.Subtitle,
        )
        StatusBanner(
            feedback = UserFeedback(FeedbackType.INFO, AppStrings.PartMapping.ImportBannerOffline),
            dense = true,
        )
        loadError?.let { message ->
            StatusBanner(
                feedback = UserFeedback(FeedbackType.ERROR, "Gagal memuat part: $message"),
                dense = true,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NeutralSurface,
            border = BorderStroke(1.dp, NeutralBorder),
            shape = MaterialTheme.shapes.medium,
            elevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = monthInput,
                    onValueChange = { monthInput = it },
                    label = { Text(AppStrings.PartMapping.MonthLabel) },
                    modifier = Modifier.weight(0.18f),
                    singleLine = true,
                )
                AppDropdown(
                    label = AppStrings.PartMapping.LineLabel,
                    options = lineOptions,
                    selectedOption =
                        lineOptions.firstOrNull {
                            it.label.equals(lineCode, ignoreCase = true) ||
                                (lineCode.isBlank() && it.id == -1L)
                        },
                    onSelected = { lineCode = if (it.id == -1L) "" else it.label },
                    modifier = Modifier.weight(0.18f),
                )
                AppDropdown(
                    label = AppStrings.PartMapping.ModelLabel,
                    options = modelOptions,
                    selectedOption =
                        modelOptions.firstOrNull {
                            it.label.equals(modelCode, ignoreCase = true) ||
                                (modelCode.isBlank() && it.id == -1L)
                        },
                    onSelected = { modelCode = if (it.id == -1L) "" else it.label },
                    modifier = Modifier.weight(0.2f),
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(AppStrings.PartMapping.SearchLabel) },
                    modifier = Modifier.weight(0.44f),
                    singleLine = true,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Surface(
                modifier = Modifier.weight(0.44f).fillMaxHeight(),
                color = NeutralSurface,
                border = BorderStroke(1.dp, NeutralBorder),
                shape = MaterialTheme.shapes.medium,
                elevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "${AppStrings.PartMapping.PartListTitle} (${parts.size})",
                        style = MaterialTheme.typography.subtitle1,
                    )
                    if (parts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) {
                            Text(
                                text = AppStrings.PartMapping.EmptyParts,
                                style = MaterialTheme.typography.body2,
                                color = NeutralTextMuted,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(parts, key = { it.partId }) { item ->
                                PartCard(
                                    item = item,
                                    thumbnail = thumbnailMap[item.uniqNo],
                                    thumbnailLoading = thumbnailLoading && !thumbnailMap.containsKey(item.uniqNo),
                                    selected = item.uniqNo == selectedUniqNo,
                                    onClick = { selectedUniqNo = item.uniqNo },
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.56f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(0.58f),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.medium,
                    elevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(text = AppStrings.PartMapping.DetailTitle, style = MaterialTheme.typography.subtitle1)
                        when {
                            partDetailLoading ->
                                Text(
                                    "Memuat detail...",
                                    style = MaterialTheme.typography.body2,
                                    color = NeutralTextMuted,
                                )
                            partDetail == null ->
                                Text(
                                    AppStrings.PartMapping.EmptyDetail,
                                    style = MaterialTheme.typography.body2,
                                    color = NeutralTextMuted,
                                )
                            else -> PartDetailContent(detail = partDetail!!, bitmap = detailBitmap)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(0.42f),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.medium,
                    elevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(text = AppStrings.PartMapping.DashboardTitle, style = MaterialTheme.typography.subtitle1)
                        Text(
                            text = AppStrings.PartMapping.TopDefects,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (topDefects.isEmpty()) {
                            Text(
                                "Belum ada data defect.",
                                style = MaterialTheme.typography.body2,
                                color = NeutralTextMuted,
                            )
                        } else {
                            topDefects.forEachIndexed { index, pair ->
                                Text(
                                    "${index + 1}. ${pair.first}: ${pair.second}",
                                    style = MaterialTheme.typography.body2,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = AppStrings.PartMapping.Heatmap,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (heatmapRows.isEmpty()) {
                            Text(
                                "Belum ada data heatmap.",
                                style = MaterialTheme.typography.body2,
                                color = NeutralTextMuted,
                            )
                        } else {
                            heatmapRows.take(10).forEach { row ->
                                Text(row, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartCard(
    item: PartListItem,
    thumbnail: ImageBitmap?,
    thumbnailLoading: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colors.primary else NeutralBorder
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .pointerInput(onClick) {
                        detectTapGestures(onTap = { onClick() })
                    },
            color = NeutralSurface,
            border = BorderStroke(1.dp, borderColor),
            shape = MaterialTheme.shapes.medium,
            elevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, NeutralBorder, RoundedCornerShape(8.dp))
                            .background(NeutralLight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else if (thumbnailLoading) {
                        SkeletonBlock(width = 54.dp, height = 54.dp, color = NeutralBorder)
                    } else {
                        Text("PNG", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${item.partNumber} (${item.uniqNo})", style = MaterialTheme.typography.subtitle2)
                    Text(item.partName, style = MaterialTheme.typography.body2, color = NeutralText)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        AppBadge(
                            text = item.lineCode.uppercase(),
                            backgroundColor = NeutralLight,
                            contentColor = NeutralText,
                        )
                        AppBadge(
                            text = item.modelCodes.joinToString(", ").ifBlank { "-" },
                            backgroundColor = NeutralLight,
                            contentColor = NeutralTextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartDetailContent(
    detail: PartDetail,
    bitmap: ImageBitmap?,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("${detail.partNumber} (${detail.uniqNo})", style = MaterialTheme.typography.subtitle1)
        Text(detail.partName, style = MaterialTheme.typography.body2, color = NeutralTextMuted)

        Row(
            modifier = Modifier.fillMaxWidth().height(230.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ZoomableImagePanel(bitmap = bitmap, modifier = Modifier.weight(0.48f))
            Column(modifier = Modifier.weight(0.52f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppBadge(
                    text = "Line ${detail.lineCode.uppercase()}",
                    backgroundColor = NeutralLight,
                    contentColor = NeutralText,
                )
                AppBadge(
                    text = "Models: ${detail.models.joinToString(", ").ifBlank { "-" }}",
                    backgroundColor = NeutralLight,
                    contentColor = NeutralTextMuted,
                )
                Text("Qty KBN per model", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
                detail.requirements.ifEmpty { listOf() }.forEach { requirement ->
                    Text("- ${requirement.modelCode}: ${requirement.qtyKbn}", style = MaterialTheme.typography.caption)
                }
            }
        }

        Text("Material Layers", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
        if (detail.materials.isEmpty()) {
            Text("-", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        } else {
            detail.materials.forEach { material ->
                val materialLine =
                    "L${material.layerOrder} ${material.materialName} | g=${material.weightG ?: "-"} | " +
                        "gsm=${material.basisWeightGsm ?: "-"} | ${material.unit ?: "-"}"
                Text(
                    text = materialLine,
                    style = MaterialTheme.typography.caption,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))
        Text("Defect Risk x Material", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
        if (detail.materialDefectRisks.isEmpty()) {
            Text("-", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        } else {
            detail.materialDefectRisks.take(8).forEach { risk ->
                Text(
                    text =
                        "${risk.defectName} | ${risk.sourceLine.uppercase()} | score ${"%.2f".format(
                            risk.riskScore,
                        )} | ${risk.affectedParts} part",
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun ZoomableImagePanel(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = NeutralLight,
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap == null) {
                Text("No image", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
            } else {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                )
            }
        }
    }
}

private fun parseYearMonth(input: String): YearMonth? {
    val text = input.trim()
    if (!Regex("^\\d{4}-\\d{2}$").matches(text)) return null
    return runCatching { YearMonth.parse(text) }.getOrNull()
}

private fun decodeImageBitmap(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    return runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}
