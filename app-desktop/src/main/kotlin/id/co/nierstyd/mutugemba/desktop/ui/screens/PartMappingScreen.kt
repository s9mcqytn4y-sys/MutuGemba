package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
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
import kotlinx.coroutines.flow.first
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
fun PartMappingScreen(dependencies: PartMappingScreenDependencies) {
    val period = remember { YearMonth.now() }

    var parts by remember { mutableStateOf<List<PartListItem>>(emptyList()) }
    var partsLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var selectedUniqNo by rememberSaveable { mutableStateOf<String?>(null) }
    var partDetail by remember { mutableStateOf<PartDetail?>(null) }
    var partDetailLoading by remember { mutableStateOf(false) }

    val thumbnailMap = remember { mutableStateMapOf<String, ImageBitmap?>() }
    var thumbnailLoading by remember { mutableStateOf(false) }
    var detailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        partsLoading = true
        loadError = null
        runCatching {
            withContext(Dispatchers.IO) {
                dependencies
                    .observeParts
                    .execute(
                        PartFilter(
                            lineCode = null,
                            modelCode = null,
                            search = null,
                            year = null,
                            month = null,
                            limit = Int.MAX_VALUE,
                        ),
                    ).first()
            }
        }.onSuccess { loaded ->
            parts = loaded
            if (selectedUniqNo == null && loaded.isNotEmpty()) {
                selectedUniqNo = loaded.first().uniqNo
            }
        }.onFailure { throwable ->
            parts = emptyList()
            loadError = throwable.message ?: "Gagal memuat part."
        }
        partsLoading = false
    }

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

    LaunchedEffect(selectedUniqNo) {
        val uniq =
            selectedUniqNo ?: run {
                partDetail = null
                detailBitmap = null
                return@LaunchedEffect
            }

        partDetailLoading = true
        partDetail =
            withContext(Dispatchers.IO) {
                dependencies.getPartDetail.execute(uniq, period.year, period.monthValue)
            }
        partDetailLoading = false
    }

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val assetLoadedCount = thumbnailMap.values.count { it != null }
        val selectedPartLabel = partDetail?.partNumber ?: selectedUniqNo ?: "-"

        SectionHeader(
            title = AppStrings.PartMapping.Title,
            subtitle = "Data part resmi PT. Primaraya Graha Nusantara (tanpa filter).",
        )

        StatusBanner(
            feedback =
                UserFeedback(
                    FeedbackType.INFO,
                    "Data part dibaca offline dari SQLite lokal. " +
                        "Pilih part untuk melihat detail model dan kebutuhan qty KBN.",
                ),
            dense = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Periode QA",
                value = "${period.monthValue.toString().padStart(2, '0')}-${period.year}",
                hint = "Sumber analitik defect bulanan",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Total Part Aktif",
                value = parts.size.toString(),
                hint = "Part tersedia untuk line produksi",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Aset Gambar",
                value = "$assetLoadedCount/${parts.size}",
                hint = if (thumbnailLoading) "Memuat thumbnail..." else "Sinkron dari asset hash store",
            )
            PartContextCard(
                modifier = Modifier.weight(1f),
                title = "Part Terpilih",
                value = selectedPartLabel,
                hint = "Gunakan daftar kiri untuk berpindah cepat",
            )
        }

        loadError?.let { message ->
            StatusBanner(
                feedback = UserFeedback(FeedbackType.ERROR, "Gagal memuat part: $message"),
                dense = true,
            )
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

                    when {
                        partsLoading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                repeat(8) {
                                    SkeletonBlock(width = 420.dp, height = 72.dp, color = NeutralLight)
                                }
                            }
                        }

                        parts.isEmpty() -> {
                            Text(
                                text = AppStrings.PartMapping.EmptyParts,
                                style = MaterialTheme.typography.body2,
                                color = NeutralTextMuted,
                            )
                        }

                        else -> {
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
            }

            Surface(
                modifier = Modifier.weight(0.56f).fillMaxHeight(),
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
                        partDetailLoading -> {
                            Text("Memuat detail...", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
                        }

                        partDetail == null -> {
                            Text(
                                AppStrings.PartMapping.EmptyDetail,
                                style = MaterialTheme.typography.body2,
                                color = NeutralTextMuted,
                            )
                        }

                        else -> {
                            PartDetailContent(detail = partDetail!!, bitmap = detailBitmap)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartContextCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    hint: String,
) {
    Surface(
        modifier = modifier,
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Text(text = value, style = MaterialTheme.typography.subtitle2, color = NeutralText)
            Text(text = hint, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
    Surface(
        modifier =
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).pointerInput(onClick) {
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
                Text(text = "(${item.uniqNo}) ${item.partName}", style = MaterialTheme.typography.subtitle2)
                Text(
                    text = "Part Number ${item.partNumber}",
                    style = MaterialTheme.typography.body2,
                    color = NeutralText,
                )
                AppBadge(
                    text = item.lineCode.uppercase(),
                    backgroundColor = NeutralLight,
                    contentColor = NeutralText,
                )
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
        Text("(${detail.uniqNo}) ${detail.partName}", style = MaterialTheme.typography.subtitle1)
        Text("Part Number ${detail.partNumber}", style = MaterialTheme.typography.body2, color = NeutralTextMuted)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, NeutralBorder, MaterialTheme.shapes.small)
                    .background(NeutralLight),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap == null) {
                Text("Gambar tidak tersedia", style = MaterialTheme.typography.body2, color = NeutralTextMuted)
            } else {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AppBadge(
            text = "Line ${detail.lineCode.uppercase()}",
            backgroundColor = NeutralLight,
            contentColor = NeutralText,
        )

        Text("Model", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
        Text(
            detail.models.joinToString(", ").ifBlank { "-" },
            style = MaterialTheme.typography.caption,
            color = NeutralTextMuted,
        )

        Text("Qty KBN", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
        if (detail.requirements.isEmpty()) {
            Text("-", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        } else {
            detail.requirements.forEach {
                Text("${it.modelCode}: ${it.qtyKbn}", style = MaterialTheme.typography.caption)
            }
        }
    }
}

private fun decodeImageBitmap(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    return runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}
