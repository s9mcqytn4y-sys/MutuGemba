@file:Suppress("TooManyFunctions")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
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
import id.co.nierstyd.mutugemba.domain.model.DefectMaster
import id.co.nierstyd.mutugemba.domain.model.MaterialMaster
import id.co.nierstyd.mutugemba.domain.model.NgOriginType
import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.domain.model.PartMasterDetail
import id.co.nierstyd.mutugemba.domain.model.PartMasterListItem
import id.co.nierstyd.mutugemba.domain.model.SaveDefectMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SaveMaterialMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SupplierMaster
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import id.co.nierstyd.mutugemba.usecase.asset.GetActiveImageRefUseCase
import id.co.nierstyd.mutugemba.usecase.asset.LoadImageBytesUseCase
import id.co.nierstyd.mutugemba.usecase.part.GetPartDetailUseCase
import id.co.nierstyd.mutugemba.usecase.part.GetPartMasterDetailUseCase
import id.co.nierstyd.mutugemba.usecase.part.ListDefectMastersUseCase
import id.co.nierstyd.mutugemba.usecase.part.ListMaterialMastersUseCase
import id.co.nierstyd.mutugemba.usecase.part.ListPartMastersUseCase
import id.co.nierstyd.mutugemba.usecase.part.ListSupplierMastersUseCase
import id.co.nierstyd.mutugemba.usecase.part.ObservePartsUseCase
import id.co.nierstyd.mutugemba.usecase.part.ReplacePartDefectsUseCase
import id.co.nierstyd.mutugemba.usecase.part.ReplacePartMaterialsUseCase
import id.co.nierstyd.mutugemba.usecase.part.SaveDefectMasterUseCase
import id.co.nierstyd.mutugemba.usecase.part.SaveMaterialMasterUseCase
import id.co.nierstyd.mutugemba.usecase.part.SavePartMasterUseCase
import id.co.nierstyd.mutugemba.usecase.part.SaveSupplierMasterUseCase
import id.co.nierstyd.mutugemba.usecase.qa.GetDefectHeatmapUseCase
import id.co.nierstyd.mutugemba.usecase.qa.GetTopDefectsPerModelMonthlyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import org.jetbrains.skia.Image as SkiaImage

data class PartMappingScreenDependencies(
    val observeParts: ObservePartsUseCase,
    val getPartDetail: GetPartDetailUseCase,
    val listPartMasters: ListPartMastersUseCase,
    val getPartMasterDetail: GetPartMasterDetailUseCase,
    val savePartMaster: SavePartMasterUseCase,
    val replacePartMaterials: ReplacePartMaterialsUseCase,
    val replacePartDefects: ReplacePartDefectsUseCase,
    val listMaterialMasters: ListMaterialMastersUseCase,
    val saveMaterialMaster: SaveMaterialMasterUseCase,
    val listSupplierMasters: ListSupplierMastersUseCase,
    val saveSupplierMaster: SaveSupplierMasterUseCase,
    val listDefectMasters: ListDefectMastersUseCase,
    val saveDefectMaster: SaveDefectMasterUseCase,
    val getTopDefects: GetTopDefectsPerModelMonthlyUseCase,
    val getDefectHeatmap: GetDefectHeatmapUseCase,
    val getActiveImageRef: GetActiveImageRefUseCase,
    val loadImageBytes: LoadImageBytesUseCase,
)

@Composable
fun PartMappingScreen(dependencies: PartMappingScreenDependencies) {
    val period = remember { YearMonth.now() }
    val scope = rememberCoroutineScope()

    var parts by remember { mutableStateOf<List<PartListItem>>(emptyList()) }
    var partsLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var selectedUniqNo by rememberSaveable { mutableStateOf<String?>(null) }
    var partDetail by remember { mutableStateOf<PartDetail?>(null) }
    var partDetailLoading by remember { mutableStateOf(false) }
    var screenMode by rememberSaveable { mutableStateOf(0) }
    var managerTabIndex by rememberSaveable { mutableStateOf(0) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var catalogSort by rememberSaveable { mutableStateOf("uniq") }
    var masterParts by remember { mutableStateOf<List<PartMasterListItem>>(emptyList()) }
    var masterMaterials by remember { mutableStateOf<List<MaterialMaster>>(emptyList()) }
    var masterSuppliers by remember { mutableStateOf<List<SupplierMaster>>(emptyList()) }
    var masterDefects by remember { mutableStateOf<List<DefectMaster>>(emptyList()) }
    var managerInfo by remember { mutableStateOf("Kelola part, material, supplier, dan item defect.") }

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

    suspend fun reloadMasters() {
        val loadedParts = dependencies.listPartMasters.execute()
        val loadedMaterials = dependencies.listMaterialMasters.execute()
        val loadedSuppliers = dependencies.listSupplierMasters.execute()
        val loadedDefects = dependencies.listDefectMasters.execute()
        masterParts = loadedParts
        masterMaterials = loadedMaterials
        masterSuppliers = loadedSuppliers
        masterDefects = loadedDefects
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { reloadMasters() }
        }.onFailure { throwable ->
            managerInfo = "Master data belum bisa dimuat: ${throwable.message ?: "-"}"
        }
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
        val catalogParts =
            parts
                .filter { part ->
                    val query = catalogQuery.trim()
                    query.isBlank() || part.matchesCatalogQuery(query)
                }.let { filtered ->
                    when (catalogSort) {
                        "part_number" -> filtered.sortedBy { it.partNumber }
                        "ng_desc" -> filtered.sortedByDescending { it.totalDefectMonthToDate }
                        else -> filtered.sortedBy { it.uniqNo }
                    }
                }

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

        TabRow(selectedTabIndex = screenMode, backgroundColor = NeutralSurface) {
            Tab(
                selected = screenMode == 0,
                onClick = { screenMode = 0 },
                text = { Text("Katalog Part") },
            )
            Tab(
                selected = screenMode == 1,
                onClick = { screenMode = 1 },
                text = { Text("Administrasi Master") },
            )
        }

        if (screenMode == 1) {
            PartMasterManagerPanel(
                tabIndex = managerTabIndex,
                onTabSelected = { managerTabIndex = it },
                parts = masterParts,
                materials = masterMaterials,
                suppliers = masterSuppliers,
                defects = masterDefects,
                infoText = managerInfo,
                onSavePart = { uniqNo, partNumber, partName, lineCode, excluded ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dependencies.savePartMaster.execute(
                                    SavePartMasterCommand(
                                        id = null,
                                        uniqNo = uniqNo,
                                        partNumber = partNumber,
                                        partName = partName,
                                        lineCode = lineCode,
                                        excludedFromChecksheet = excluded,
                                    ),
                                )
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Data part berhasil disimpan."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan data part: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onSaveSupplier = { name ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dependencies.saveSupplierMaster.execute(id = null, name = name)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Data pemasok berhasil disimpan."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan pemasok: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onSaveMaterial = { name, supplierId, clientSupplied ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dependencies.saveMaterialMaster.execute(
                                    SaveMaterialMasterCommand(
                                        id = null,
                                        name = name,
                                        supplierId = supplierId,
                                        clientSupplied = clientSupplied,
                                    ),
                                )
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Data bahan berhasil disimpan."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan data bahan: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onSaveDefect = { name, originType, lineCode ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dependencies.saveDefectMaster.execute(
                                    SaveDefectMasterCommand(
                                        id = null,
                                        name = name,
                                        originType = originType,
                                        lineCode = lineCode,
                                    ),
                                )
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Jenis NG berhasil disimpan."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan jenis NG: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onAssignPartMaterials = { partId, materialIds ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dependencies.replacePartMaterials.execute(partId, materialIds)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Layer bahan part berhasil diperbarui."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal perbarui layer bahan: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onAssignPartDefects = { partId, defectIds ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val assignments =
                                    defectIds.mapNotNull { defectId ->
                                        masterDefects.firstOrNull { it.id == defectId }?.let { defect ->
                                            id.co.nierstyd.mutugemba.domain.model.SavePartDefectAssignmentCommand(
                                                defectId = defect.id,
                                                originType = defect.originType,
                                                materialId = null,
                                            )
                                        }
                                    }
                                dependencies.replacePartDefects.execute(partId, assignments)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Mapping jenis NG part berhasil diperbarui."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal perbarui mapping jenis NG: ${throwable.message ?: "-"}"
                        }
                    }
                },
                loadPartDetail = { partId ->
                    withContext(Dispatchers.IO) { dependencies.getPartMasterDetail.execute(partId) }
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PartContextCard(
                    modifier = Modifier.weight(1f),
                    title = "Periode QA",
                    value = "${period.monthValue.toString().padStart(2, '0')}-${period.year}",
                    hint = "Sumber analitik NG bulanan",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppTextField(
                    spec =
                        FieldSpec(
                            label = "Cari Katalog Part",
                            placeholder = "Cari UNIQ / part number / nama part",
                        ),
                    value = catalogQuery,
                    onValueChange = { catalogQuery = it },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                )
                AppTextField(
                    spec =
                        FieldSpec(
                            label = "Urutkan (uniq / part_number / ng_desc)",
                        ),
                    value = catalogSort,
                    onValueChange = { catalogSort = it.trim().lowercase() },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
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
                            text = "${AppStrings.PartMapping.PartListTitle} (${catalogParts.size})",
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

                            catalogParts.isEmpty() -> {
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
                                    items(catalogParts, key = { it.partId }) { item ->
                                        PartCard(
                                            item = item,
                                            thumbnail = thumbnailMap[item.uniqNo],
                                            thumbnailLoading =
                                                thumbnailLoading && !thumbnailMap.containsKey(item.uniqNo),
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
                                Text(
                                    "Memuat detail...",
                                    style = MaterialTheme.typography.body2,
                                    color = NeutralTextMuted,
                                )
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
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AppBadge(
                        text = item.lineCode.uppercase(),
                        backgroundColor = NeutralLight,
                        contentColor = NeutralText,
                    )
                    AppBadge(
                        text = "Model ${item.modelCodes.size}",
                        backgroundColor = NeutralLight,
                        contentColor = NeutralTextMuted,
                    )
                    AppBadge(
                        text = "NG Bulan ${item.totalDefectMonthToDate}",
                        backgroundColor = NeutralLight,
                        contentColor =
                            if (item.totalDefectMonthToDate >
                                0
                            ) {
                                MaterialTheme.colors.primary
                            } else {
                                NeutralTextMuted
                            },
                    )
                }
                Text(
                    text = "Klik untuk lihat profil detail part",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun PartDetailContent(
    detail: PartDetail,
    bitmap: ImageBitmap?,
) {
    val requirementTotal = detail.requirements.sumOf { it.qtyKbn }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Profil Detail Part", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(
            text = "(${detail.uniqNo}) ${detail.partName}",
            style = MaterialTheme.typography.subtitle1,
            color = NeutralText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text("Part Number ${detail.partNumber}", style = MaterialTheme.typography.body2, color = NeutralTextMuted)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Surface(
                modifier = Modifier.weight(0.52f).fillMaxHeight(),
                color = NeutralLight,
                border = BorderStroke(1.dp, NeutralBorder),
                shape = MaterialTheme.shapes.medium,
                elevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(Spacing.sm),
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
            }

            Column(
                modifier = Modifier.weight(0.48f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PartProfileMetric(
                    label = "UNIQ",
                    value = detail.uniqNo,
                )
                PartProfileMetric(
                    label = "Line Produksi",
                    value = detail.lineCode.uppercase(),
                )
                PartProfileMetric(
                    label = "Model Terkait",
                    value = detail.models.size.toString(),
                )
                PartProfileMetric(
                    label = "Total Qty KBN",
                    value = requirementTotal.toString(),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeutralLight,
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.small,
                    elevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = "Status Asset",
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                        Text(
                            text = if (bitmap == null) "Belum tersedia" else "Tersedia",
                            style = MaterialTheme.typography.body2,
                            color = if (bitmap == null) NeutralTextMuted else MaterialTheme.colors.primary,
                        )
                    }
                }
            }
        }

        AppBadge(
            text = "Line ${detail.lineCode.uppercase()}",
            backgroundColor = NeutralLight,
            contentColor = NeutralText,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NeutralSurface,
            border = BorderStroke(1.dp, NeutralBorder),
            shape = MaterialTheme.shapes.medium,
            elevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Model Terkait", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
                if (detail.models.isEmpty()) {
                    Text("-", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                } else {
                    detail.models.sorted().forEach { model ->
                        Text(
                            text = "• $model",
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                    }
                }
            }
        }

        PartRequirementTable(requirements = detail.requirements.map { it.modelCode to it.qtyKbn })
    }
}

@Composable
private fun PartProfileMetric(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.body1,
                color = NeutralText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PartRequirementTable(requirements: List<Pair<String, Int>>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("Kebutuhan Qty KBN", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
            if (requirements.isEmpty()) {
                Text("-", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Model",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Qty",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                        modifier = Modifier.weight(0.4f),
                    )
                }
                requirements.sortedBy { it.first }.forEach { (modelCode, qtyKbn) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = modelCode,
                            style = MaterialTheme.typography.body2,
                            color = NeutralText,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = qtyKbn.toString(),
                            style = MaterialTheme.typography.body2,
                            color = NeutralText,
                            modifier = Modifier.weight(0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PartMasterManagerPanel(
    tabIndex: Int,
    onTabSelected: (Int) -> Unit,
    parts: List<PartMasterListItem>,
    materials: List<MaterialMaster>,
    suppliers: List<SupplierMaster>,
    defects: List<DefectMaster>,
    infoText: String,
    onSavePart: (uniqNo: String, partNumber: String, partName: String, lineCode: String, excluded: Boolean) -> Unit,
    onSaveSupplier: (name: String) -> Unit,
    onSaveMaterial: (name: String, supplierId: Long?, clientSupplied: Boolean) -> Unit,
    onSaveDefect: (name: String, originType: NgOriginType, lineCode: String?) -> Unit,
    onAssignPartMaterials: (partId: Long, materialIds: List<Long>) -> Unit,
    onAssignPartDefects: (partId: Long, defectIds: List<Long>) -> Unit,
    loadPartDetail: suspend (Long) -> PartMasterDetail?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "Administrasi Data Master",
                style = MaterialTheme.typography.subtitle1,
                color = NeutralText,
            )
            Text(
                text = infoText,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            TabRow(selectedTabIndex = tabIndex, backgroundColor = NeutralSurface) {
                listOf("Part", "Bahan", "Pemasok", "Jenis NG").forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { onTabSelected(index) },
                        text = { Text(title) },
                    )
                }
            }
            when (tabIndex) {
                0 ->
                    PartEditorTab(
                        totalParts = parts.size,
                        parts = parts,
                        materials = materials,
                        defects = defects,
                        onSavePart = onSavePart,
                        onAssignPartMaterials = onAssignPartMaterials,
                        onAssignPartDefects = onAssignPartDefects,
                        loadPartDetail = loadPartDetail,
                    )

                1 ->
                    MaterialEditorTab(
                        materials = materials,
                        suppliers = suppliers,
                        onSaveMaterial = onSaveMaterial,
                    )

                2 ->
                    SupplierEditorTab(
                        suppliers = suppliers,
                        onSaveSupplier = onSaveSupplier,
                    )

                else ->
                    DefectEditorTab(
                        defects = defects,
                        onSaveDefect = onSaveDefect,
                    )
            }
        }
    }
}

@Composable
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
private fun PartEditorTab(
    totalParts: Int,
    parts: List<PartMasterListItem>,
    materials: List<MaterialMaster>,
    defects: List<DefectMaster>,
    onSavePart: (uniqNo: String, partNumber: String, partName: String, lineCode: String, excluded: Boolean) -> Unit,
    onAssignPartMaterials: (partId: Long, materialIds: List<Long>) -> Unit,
    onAssignPartDefects: (partId: Long, defectIds: List<Long>) -> Unit,
    loadPartDetail: suspend (Long) -> PartMasterDetail?,
) {
    var uniqNo by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var partName by remember { mutableStateOf("") }
    var lineCode by remember { mutableStateOf("press") }
    var excluded by remember { mutableStateOf(false) }
    var assignPartQuery by remember { mutableStateOf("") }
    var selectedAssignPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var assignmentLoading by remember { mutableStateOf(false) }
    val selectedMaterialIds = remember { mutableStateMapOf<Long, Boolean>() }
    val selectedDefectIds = remember { mutableStateMapOf<Long, Boolean>() }
    val selectedPart = parts.firstOrNull { it.id == selectedAssignPartId }
    val assignPartCandidates =
        remember(parts, assignPartQuery) {
            val keyword = assignPartQuery.trim().lowercase()
            parts
                .filter { part ->
                    keyword.isBlank() ||
                        part.uniqNo.lowercase().contains(keyword) ||
                        part.partNumber.lowercase().contains(keyword) ||
                        part.partName.lowercase().contains(keyword)
                }.take(8)
        }
    val eligibleDefects =
        remember(defects, selectedPart?.lineCode) {
            defects.filter { defect ->
                defect.lineCode.isNullOrBlank() ||
                    selectedPart?.lineCode.equals(defect.lineCode, ignoreCase = true)
            }
        }

    LaunchedEffect(parts) {
        if (selectedAssignPartId == null || parts.none { it.id == selectedAssignPartId }) {
            selectedAssignPartId = parts.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedAssignPartId, materials, defects) {
        selectedMaterialIds.clear()
        selectedDefectIds.clear()
        val partId = selectedAssignPartId ?: return@LaunchedEffect
        assignmentLoading = true
        val detail = runCatching { loadPartDetail(partId) }.getOrNull()
        detail?.materials?.forEach { selectedMaterialIds[it.materialId] = true }
        detail?.defects?.forEach { selectedDefectIds[it.defectId] = true }
        assignmentLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Part aktif: $totalParts", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                spec = FieldSpec(label = "UNIQ"),
                value = uniqNo,
                onValueChange = { uniqNo = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            AppTextField(
                spec = FieldSpec(label = "Part Number"),
                value = partNumber,
                onValueChange = { partNumber = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                spec = FieldSpec(label = "Nama Part"),
                value = partName,
                onValueChange = { partName = it },
                modifier = Modifier.weight(1.5f),
                singleLine = true,
            )
            AppTextField(
                spec = FieldSpec(label = "Line Produksi (press/sewing)"),
                value = lineCode,
                onValueChange = { lineCode = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            AppTextField(
                spec = FieldSpec(label = "Kecualikan dari Checksheet (0/1)"),
                value = if (excluded) "1" else "0",
                onValueChange = { excluded = it == "1" },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PrimaryButton(
                text = "Simpan Data Part",
                onClick = { onSavePart(uniqNo, partNumber, partName, lineCode, excluded) },
            )
            SecondaryButton(
                text = "Reset",
                onClick = {
                    uniqNo = ""
                    partNumber = ""
                    partName = ""
                    lineCode = "press"
                    excluded = false
                },
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NeutralLight.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, NeutralBorder),
            shape = MaterialTheme.shapes.small,
            elevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Assignment Cerdas per Part",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "UI granular visual: pilih part, lalu centang layer bahan " +
                            "dan Jenis NG tanpa input ID manual.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                AppTextField(
                    spec = FieldSpec(label = "Cari Part untuk Assignment"),
                    value = assignPartQuery,
                    onValueChange = { assignPartQuery = it },
                    singleLine = true,
                )
                if (assignPartCandidates.isEmpty()) {
                    Text("Part tidak ditemukan.", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        assignPartCandidates.forEach { part ->
                            val selected = part.id == selectedAssignPartId
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAssignPartId = part.id },
                                color = if (selected) NeutralLight else NeutralSurface,
                                border =
                                    BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colors.primary else NeutralBorder,
                                    ),
                                shape = MaterialTheme.shapes.small,
                                elevation = 0.dp,
                            ) {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth().padding(
                                            horizontal = Spacing.sm,
                                            vertical = Spacing.xs,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${part.uniqNo} - ${part.partNumber}",
                                        style = MaterialTheme.typography.body2,
                                        color = NeutralText,
                                    )
                                    AppBadge(
                                        text = part.lineCode.uppercase(),
                                        backgroundColor = NeutralSurface,
                                        contentColor = NeutralTextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
                selectedPart?.let { part ->
                    Text(
                        text = "Part terpilih: ${part.uniqNo} • ${part.partNumber}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
                if (assignmentLoading) {
                    Text(
                        "Memuat assignment part...",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, NeutralBorder),
                        shape = MaterialTheme.shapes.small,
                        elevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text("Layer Bahan", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                            materials.forEach { material ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = !(selectedMaterialIds[material.id] ?: false)
                                                selectedMaterialIds[material.id] = next
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selectedMaterialIds[material.id] ?: false,
                                        onCheckedChange = { checked -> selectedMaterialIds[material.id] = checked },
                                    )
                                    Column {
                                        Text(material.name, style = MaterialTheme.typography.body2)
                                        Text(
                                            material.supplierName ?: "Pemasok belum diatur",
                                            style = MaterialTheme.typography.caption,
                                            color = NeutralTextMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, NeutralBorder),
                        shape = MaterialTheme.shapes.small,
                        elevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text("Jenis NG", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                            eligibleDefects.forEach { defect ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = !(selectedDefectIds[defect.id] ?: false)
                                                selectedDefectIds[defect.id] = next
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selectedDefectIds[defect.id] ?: false,
                                        onCheckedChange = { checked -> selectedDefectIds[defect.id] = checked },
                                    )
                                    Column {
                                        Text(defect.name, style = MaterialTheme.typography.body2)
                                        Text(
                                            "${defect.originType} ${defect.lineCode?.uppercase()?.let {
                                                "• $it"
                                            } ?: ""}",
                                            style = MaterialTheme.typography.caption,
                                            color = NeutralTextMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SecondaryButton(
                        text = "Simpan Layer Bahan",
                        onClick = {
                            val partId = selectedAssignPartId ?: return@SecondaryButton
                            val materialIds =
                                materials
                                    .filter { selectedMaterialIds[it.id] == true }
                                    .map { it.id }
                            onAssignPartMaterials(partId, materialIds)
                        },
                    )
                    SecondaryButton(
                        text = "Simpan Jenis NG",
                        onClick = {
                            val partId = selectedAssignPartId ?: return@SecondaryButton
                            val defectIds =
                                eligibleDefects
                                    .filter { selectedDefectIds[it.id] == true }
                                    .map { it.id }
                            onAssignPartDefects(partId, defectIds)
                        },
                    )
                }
                Text(
                    text =
                        "Part termuat: ${parts.size} | Bahan: ${materials.size} | Jenis NG: ${defects.size}",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
    }
}

@Composable
private fun MaterialEditorTab(
    materials: List<MaterialMaster>,
    suppliers: List<SupplierMaster>,
    onSaveMaterial: (name: String, supplierId: Long?, clientSupplied: Boolean) -> Unit,
) {
    var materialName by remember { mutableStateOf("") }
    var supplierRef by remember { mutableStateOf("") }
    var clientSupplied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Material tersedia: ${materials.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec = FieldSpec(label = "Nama Material"),
            value = materialName,
            onValueChange = { materialName = it },
            singleLine = true,
        )
        AppTextField(
            spec = FieldSpec(label = "ID Pemasok (opsional)"),
            value = supplierRef,
            onValueChange = { supplierRef = it.filter(Char::isDigit) },
            singleLine = true,
        )
        AppTextField(
            spec = FieldSpec(label = "Bahan Titipan Klien (0/1)"),
            value = if (clientSupplied) "1" else "0",
            onValueChange = { clientSupplied = it == "1" },
            singleLine = true,
        )
        Text(
            "Referensi Pemasok: ${
                suppliers.take(5).joinToString { "${it.id}:${it.name}" }.ifBlank { "-" }
            }",
            style = MaterialTheme.typography.caption,
            color = NeutralTextMuted,
        )
        PrimaryButton(
            text = "Simpan Data Bahan",
            onClick = {
                onSaveMaterial(
                    materialName,
                    supplierRef.toLongOrNull(),
                    clientSupplied,
                )
            },
        )
    }
}

@Composable
private fun SupplierEditorTab(
    suppliers: List<SupplierMaster>,
    onSaveSupplier: (name: String) -> Unit,
) {
    var supplierName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Pemasok: ${suppliers.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec = FieldSpec(label = "Nama Pemasok"),
            value = supplierName,
            onValueChange = { supplierName = it },
            singleLine = true,
        )
        PrimaryButton(
            text = "Simpan Data Pemasok",
            onClick = { onSaveSupplier(supplierName) },
        )
    }
}

@Composable
private fun DefectEditorTab(
    defects: List<DefectMaster>,
    onSaveDefect: (name: String, originType: NgOriginType, lineCode: String?) -> Unit,
) {
    var defectName by remember { mutableStateOf("") }
    var originRaw by remember { mutableStateOf("material") }
    var lineCode by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Jenis NG: ${defects.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec = FieldSpec(label = "Nama Jenis NG"),
            value = defectName,
            onValueChange = { defectName = it },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                spec = FieldSpec(label = "Asal NG (material/process)"),
                value = originRaw,
                onValueChange = { originRaw = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            AppTextField(
                spec = FieldSpec(label = "Line Produksi (opsional)"),
                value = lineCode,
                onValueChange = { lineCode = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        PrimaryButton(
            text = "Simpan Jenis NG",
            onClick = {
                onSaveDefect(
                    defectName,
                    if (originRaw.equals("process", true)) NgOriginType.PROCESS else NgOriginType.MATERIAL,
                    lineCode.takeIf { it.isNotBlank() },
                )
            },
        )
    }
}

private fun decodeImageBitmap(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    return runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

private fun PartListItem.matchesCatalogQuery(rawQuery: String): Boolean {
    val query = rawQuery.trim().lowercase()
    if (query.isBlank()) return true
    val searchBucket =
        listOf(
            uniqNo,
            partNumber,
            partName,
            lineCode,
            modelCodes.joinToString(" "),
        ).joinToString(" ").lowercase()
    return query in searchBucket
}
