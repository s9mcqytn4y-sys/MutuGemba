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
import id.co.nierstyd.mutugemba.desktop.ui.components.AppDropdown
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
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
import id.co.nierstyd.mutugemba.usecase.part.DeleteDefectMasterUseCase
import id.co.nierstyd.mutugemba.usecase.part.DeleteMaterialMasterUseCase
import id.co.nierstyd.mutugemba.usecase.part.DeleteSupplierMasterUseCase
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
    val deleteMaterialMaster: DeleteMaterialMasterUseCase,
    val listSupplierMasters: ListSupplierMastersUseCase,
    val saveSupplierMaster: SaveSupplierMasterUseCase,
    val deleteSupplierMaster: DeleteSupplierMasterUseCase,
    val listDefectMasters: ListDefectMastersUseCase,
    val saveDefectMaster: SaveDefectMasterUseCase,
    val deleteDefectMaster: DeleteDefectMasterUseCase,
    val getTopDefects: GetTopDefectsPerModelMonthlyUseCase,
    val getDefectHeatmap: GetDefectHeatmapUseCase,
    val getActiveImageRef: GetActiveImageRefUseCase,
    val loadImageBytes: LoadImageBytesUseCase,
)

enum class PartMappingViewMode {
    ALL,
    CATALOG_ONLY,
    MASTER_ONLY,
}

private enum class CatalogFocusMode {
    LIST,
    DETAIL,
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun PartMappingScreen(
    dependencies: PartMappingScreenDependencies,
    viewMode: PartMappingViewMode = PartMappingViewMode.ALL,
) {
    val period = remember { YearMonth.now() }
    val scope = rememberCoroutineScope()

    var parts by remember { mutableStateOf<List<PartListItem>>(emptyList()) }
    var partsLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var selectedUniqNo by rememberSaveable { mutableStateOf<String?>(null) }
    var partDetail by remember { mutableStateOf<PartDetail?>(null) }
    var partDetailLoading by remember { mutableStateOf(false) }
    var screenMode by rememberSaveable {
        mutableStateOf(
            when (viewMode) {
                PartMappingViewMode.ALL -> 0
                PartMappingViewMode.CATALOG_ONLY -> 0
                PartMappingViewMode.MASTER_ONLY -> 1
            },
        )
    }
    var managerTabIndex by rememberSaveable { mutableStateOf(0) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var catalogSort by rememberSaveable { mutableStateOf("uniq") }
    var catalogFocusMode by rememberSaveable { mutableStateOf(CatalogFocusMode.LIST) }
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

    LaunchedEffect(viewMode) {
        when (viewMode) {
            PartMappingViewMode.ALL -> Unit
            PartMappingViewMode.CATALOG_ONLY -> {
                screenMode = 0
                catalogFocusMode = CatalogFocusMode.LIST
            }
            PartMappingViewMode.MASTER_ONLY -> screenMode = 1
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

        val pageTitle =
            when (viewMode) {
                PartMappingViewMode.MASTER_ONLY -> AppStrings.PartMaster.Title
                else -> AppStrings.PartCatalog.Title
            }
        val pageSubtitle =
            when (viewMode) {
                PartMappingViewMode.MASTER_ONLY -> AppStrings.PartMaster.Subtitle
                else -> AppStrings.PartCatalog.Subtitle
            }
        SectionHeader(title = pageTitle, subtitle = pageSubtitle)

        StatusBanner(
            feedback =
                UserFeedback(
                    FeedbackType.INFO,
                    "Data part dibaca offline dari SQLite lokal. " +
                        "Pilih part untuk melihat detail model dan kebutuhan qty KBN.",
                ),
            dense = true,
        )

        if (viewMode == PartMappingViewMode.ALL) {
            TabRow(selectedTabIndex = screenMode, backgroundColor = NeutralSurface) {
                Tab(
                    selected = screenMode == 0,
                    onClick = {
                        screenMode = 0
                        catalogFocusMode = CatalogFocusMode.LIST
                    },
                    text = { Text("Katalog Part") },
                )
                Tab(
                    selected = screenMode == 1,
                    onClick = { screenMode = 1 },
                    text = { Text("Administrasi Master") },
                )
            }
        }

        if (screenMode == 1) {
            PartMasterAdminScreen(
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
                onSaveSupplier = { id, name ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val normalized = name.trim()
                                require(normalized.isNotBlank()) { "Nama pemasok wajib diisi." }
                                dependencies.saveSupplierMaster.execute(id = id, name = normalized)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo =
                                if (id == null) {
                                    "Data pemasok berhasil ditambahkan."
                                } else {
                                    "Data pemasok berhasil diperbarui."
                                }
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan pemasok: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onDeleteSupplier = { id ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                require(id > 0L) { "ID pemasok tidak valid." }
                                dependencies.deleteSupplierMaster.execute(id)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Pemasok berhasil dihapus."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal hapus pemasok: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onSaveMaterial = { id, name, supplierId, clientSupplied ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val normalized = name.trim()
                                require(normalized.isNotBlank()) { "Nama bahan wajib diisi." }
                                if (supplierId != null) {
                                    require(masterSuppliers.any { it.id == supplierId }) {
                                        "ID pemasok tidak ditemukan."
                                    }
                                }
                                dependencies.saveMaterialMaster.execute(
                                    SaveMaterialMasterCommand(
                                        id = id,
                                        name = normalized,
                                        supplierId = supplierId,
                                        clientSupplied = clientSupplied,
                                    ),
                                )
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo =
                                if (id == null) {
                                    "Data bahan berhasil ditambahkan."
                                } else {
                                    "Data bahan berhasil diperbarui."
                                }
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan data bahan: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onDeleteMaterial = { id ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                require(id > 0L) { "ID bahan tidak valid." }
                                dependencies.deleteMaterialMaster.execute(id)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Bahan berhasil dihapus."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal hapus bahan: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onSaveDefect = { id, name, originType, lineCode ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val normalized = name.trim()
                                require(normalized.isNotBlank()) { "Nama Jenis NG wajib diisi." }
                                val normalizedLine =
                                    lineCode
                                        ?.trim()
                                        ?.lowercase()
                                        ?.takeIf { it.isNotBlank() }
                                if (normalizedLine != null) {
                                    require(normalizedLine == "press" || normalizedLine == "sewing") {
                                        "Line untuk Jenis NG harus press atau sewing."
                                    }
                                }
                                dependencies.saveDefectMaster.execute(
                                    SaveDefectMasterCommand(
                                        id = id,
                                        name = normalized,
                                        originType = originType,
                                        lineCode = normalizedLine,
                                    ),
                                )
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo =
                                if (id == null) {
                                    "Jenis NG berhasil ditambahkan."
                                } else {
                                    "Jenis NG berhasil diperbarui."
                                }
                        }.onFailure { throwable ->
                            managerInfo = "Gagal simpan jenis NG: ${throwable.message ?: "-"}"
                        }
                    }
                },
                onDeleteDefect = { id ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                require(id > 0L) { "ID Jenis NG tidak valid." }
                                dependencies.deleteDefectMaster.execute(id)
                                reloadMasters()
                            }
                        }.onSuccess {
                            managerInfo = "Jenis NG berhasil dihapus."
                        }.onFailure { throwable ->
                            managerInfo = "Gagal hapus Jenis NG: ${throwable.message ?: "-"}"
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
            when (catalogFocusMode) {
                CatalogFocusMode.LIST ->
                    PartCatalogScreen(
                        period = period,
                        parts = parts,
                        filteredParts = catalogParts,
                        selectedUniqNo = selectedUniqNo,
                        selectedPartLabel = selectedPartLabel,
                        assetLoadedCount = assetLoadedCount,
                        thumbnailLoading = thumbnailLoading,
                        thumbnailMap = thumbnailMap,
                        partsLoading = partsLoading,
                        loadError = loadError,
                        catalogQuery = catalogQuery,
                        catalogSort = catalogSort,
                        onCatalogQueryChange = { catalogQuery = it },
                        onCatalogSortChange = { catalogSort = it.trim().lowercase() },
                        onDismissError = { loadError = null },
                        onSelectPart = { item ->
                            selectedUniqNo = item.uniqNo
                            catalogFocusMode = CatalogFocusMode.DETAIL
                        },
                    )

                CatalogFocusMode.DETAIL ->
                    PartDetailScreen(
                        selectedUniqNo = selectedUniqNo,
                        detail = partDetail,
                        detailBitmap = detailBitmap,
                        detailLoading = partDetailLoading,
                        onBack = { catalogFocusMode = CatalogFocusMode.LIST },
                    )
            }
        }
    }
}

@Composable
internal fun PartContextCard(
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
internal fun PartCard(
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
internal fun PartDetailContent(
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

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun PartMasterManagerPanel(
    tabIndex: Int,
    onTabSelected: (Int) -> Unit,
    parts: List<PartMasterListItem>,
    materials: List<MaterialMaster>,
    suppliers: List<SupplierMaster>,
    defects: List<DefectMaster>,
    infoText: String,
    onSavePart: (uniqNo: String, partNumber: String, partName: String, lineCode: String, excluded: Boolean) -> Unit,
    onSaveSupplier: (id: Long?, name: String) -> Unit,
    onDeleteSupplier: (id: Long) -> Unit,
    onSaveMaterial: (id: Long?, name: String, supplierId: Long?, clientSupplied: Boolean) -> Unit,
    onDeleteMaterial: (id: Long) -> Unit,
    onSaveDefect: (id: Long?, name: String, originType: NgOriginType, lineCode: String?) -> Unit,
    onDeleteDefect: (id: Long) -> Unit,
    onAssignPartMaterials: (partId: Long, materialIds: List<Long>) -> Unit,
    onAssignPartDefects: (partId: Long, defectIds: List<Long>) -> Unit,
    loadPartDetail: suspend (Long) -> PartMasterDetail?,
) {
    var impactRows by remember { mutableStateOf<List<MaterialImpactRow>>(emptyList()) }
    var impactLoading by remember { mutableStateOf(true) }
    var impactError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(parts, materials, suppliers, defects) {
        impactLoading = true
        impactError = null
        runCatching {
            withContext(Dispatchers.IO) {
                buildMaterialImpactRows(
                    parts = parts,
                    materials = materials,
                    defects = defects,
                    loadPartDetail = loadPartDetail,
                )
            }
        }.onSuccess { rows ->
            impactRows = rows
        }.onFailure { throwable ->
            impactRows = emptyList()
            impactError = throwable.message ?: "Gagal menyusun panel relasi dampak."
        }
        impactLoading = false
    }

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
                        onDeleteMaterial = onDeleteMaterial,
                    )

                2 ->
                    SupplierEditorTab(
                        suppliers = suppliers,
                        onSaveSupplier = onSaveSupplier,
                        onDeleteSupplier = onDeleteSupplier,
                    )

                else ->
                    DefectEditorTab(
                        defects = defects,
                        onSaveDefect = onSaveDefect,
                        onDeleteDefect = onDeleteDefect,
                    )
            }
            MaterialImpactPanel(
                rows = impactRows,
                suppliers = suppliers,
                loading = impactLoading,
                error = impactError,
            )
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
    val normalizedUniq = uniqNo.trim().uppercase()
    val normalizedPartNumber = partNumber.normalizeHumanInput().uppercase()
    val normalizedPartName = partName.normalizeHumanInput()
    val normalizedLineCode = lineCode.trim().lowercase()
    val uniqDuplicate =
        normalizedUniq.isNotBlank() &&
            parts.any { it.uniqNo.equals(normalizedUniq, ignoreCase = true) }
    val partNumberDuplicate =
        normalizedPartNumber.isNotBlank() &&
            parts.any { it.partNumber.equals(normalizedPartNumber, ignoreCase = true) }
    val uniqError =
        when {
            normalizedUniq.isBlank() -> "UNIQ wajib diisi."
            uniqDuplicate -> "UNIQ sudah dipakai part lain."
            else -> null
        }
    val partNumberError =
        when {
            normalizedPartNumber.isBlank() -> "Part Number wajib diisi."
            partNumberDuplicate -> "Part Number sudah terdaftar."
            else -> null
        }
    val partNameError =
        when {
            normalizedPartName.isBlank() -> "Nama part wajib diisi."
            else -> null
        }
    val lineCodeError =
        when {
            normalizedLineCode != "press" && normalizedLineCode != "sewing" ->
                "Line produksi harus `press` atau `sewing`."
            else -> null
        }
    val canSubmit = uniqError == null && partNumberError == null && partNameError == null && lineCodeError == null
    val lineOptions =
        remember {
            listOf(
                DropdownOption(1L, "press", "Line press"),
                DropdownOption(2L, "sewing", "Line sewing"),
            )
        }
    val exclusionOptions =
        remember {
            listOf(
                DropdownOption(0L, "Tidak (0)", "Part tetap masuk checksheet."),
                DropdownOption(1L, "Ya (1)", "Part tidak dimunculkan di checksheet."),
            )
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
                spec =
                    FieldSpec(
                        label = "UNIQ",
                        helperText = uniqError ?: "Kode unik part. Contoh: B35",
                        isError = uniqError != null,
                    ),
                value = uniqNo,
                onValueChange = { uniqNo = it.filter { ch -> ch.isLetterOrDigit() || ch == '-' }.uppercase() },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            AppTextField(
                spec =
                    FieldSpec(
                        label = "Part Number",
                        helperText = partNumberError ?: "Nomor part resmi produksi.",
                        isError = partNumberError != null,
                    ),
                value = partNumber,
                onValueChange = {
                    partNumber =
                        it
                            .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '/' }
                            .uppercase()
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                spec =
                    FieldSpec(
                        label = "Nama Part",
                        helperText = partNameError ?: "Nama part akan tampil di katalog dan laporan.",
                        isError = partNameError != null,
                    ),
                value = partName,
                onValueChange = { partName = it },
                modifier = Modifier.weight(1.5f),
                singleLine = true,
            )
            AppDropdown(
                label = "Line Produksi",
                options = lineOptions,
                selectedOption = lineOptions.firstOrNull { it.label == normalizedLineCode },
                onSelected = { option -> lineCode = option.label },
                modifier = Modifier.weight(1f),
                helperText = lineCodeError ?: "Pilih line produksi untuk part ini.",
                isError = lineCodeError != null,
            )
            AppDropdown(
                label = "Kecualikan dari Checksheet",
                options = exclusionOptions,
                selectedOption = exclusionOptions.firstOrNull { it.id == if (excluded) 1L else 0L },
                onSelected = { option -> excluded = option.id == 1L },
                modifier = Modifier.weight(1f),
                helperText = "Atur apakah part ini tampil di checksheet harian.",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PrimaryButton(
                text = "Simpan Data Part",
                enabled = canSubmit,
                onClick = {
                    onSavePart(
                        normalizedUniq,
                        normalizedPartNumber,
                        normalizedPartName,
                        normalizedLineCode,
                        excluded,
                    )
                },
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
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun MaterialEditorTab(
    materials: List<MaterialMaster>,
    suppliers: List<SupplierMaster>,
    onSaveMaterial: (id: Long?, name: String, supplierId: Long?, clientSupplied: Boolean) -> Unit,
    onDeleteMaterial: (id: Long) -> Unit,
) {
    var materialId by remember { mutableStateOf("") }
    var materialName by remember { mutableStateOf("") }
    var supplierRef by remember { mutableStateOf("") }
    var clientSupplied by remember { mutableStateOf(false) }
    val editingId = materialId.toLongOrNull()
    val normalizedName = materialName.normalizeHumanInput()
    val supplierId = supplierRef.toLongOrNull()
    val materialDuplicate =
        normalizedName.isNotBlank() &&
            materials.any { item ->
                item.id != editingId && item.name.normalizeHumanInput().equals(normalizedName, ignoreCase = true)
            }
    val nameError =
        when {
            normalizedName.isBlank() -> "Nama bahan wajib diisi."
            materialDuplicate -> "Nama bahan sudah terdaftar."
            else -> null
        }
    val supplierError =
        when {
            supplierRef.isBlank() -> null
            supplierId == null -> "ID pemasok harus berupa angka."
            suppliers.none { it.id == supplierId } -> "ID pemasok tidak ditemukan."
            else -> null
        }
    val canSave = nameError == null && supplierError == null
    val supplierOptions =
        remember(suppliers) {
            listOf(DropdownOption(0L, "Tanpa pemasok", "Material belum dipetakan ke pemasok")) +
                suppliers.map { supplier ->
                    DropdownOption(
                        id = supplier.id,
                        label = "${supplier.id} - ${supplier.name}",
                    )
                }
        }
    val clientOptions =
        remember {
            listOf(
                DropdownOption(0L, "Tidak (0)", "Material reguler."),
                DropdownOption(1L, "Ya (1)", "Material titipan klien."),
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Material tersedia: ${materials.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec =
                FieldSpec(
                    label = "ID Bahan (kosong = tambah baru)",
                    helperText = "Isi ID untuk update. Kosongkan untuk tambah baru.",
                ),
            value = materialId,
            onValueChange = { materialId = it.filter(Char::isDigit) },
            singleLine = true,
        )
        AppTextField(
            spec =
                FieldSpec(
                    label = "Nama Material",
                    helperText = nameError ?: "Nama bahan unik (tidak boleh duplikat).",
                    isError = nameError != null,
                ),
            value = materialName,
            onValueChange = { materialName = it },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                spec =
                    FieldSpec(
                        label = "ID Pemasok (opsional)",
                        helperText = supplierError ?: "Bisa diisi manual atau pilih dropdown.",
                        isError = supplierError != null,
                    ),
                value = supplierRef,
                onValueChange = { supplierRef = it.filter(Char::isDigit) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppDropdown(
                label = "Pilih Pemasok",
                options = supplierOptions,
                selectedOption =
                    supplierOptions.firstOrNull { option ->
                        option.id == (supplierId ?: 0L)
                    },
                onSelected = { option ->
                    supplierRef = if (option.id == 0L) "" else option.id.toString()
                },
                modifier = Modifier.weight(1f),
                helperText = "Pemilihan cepat pemasok.",
            )
        }
        AppDropdown(
            label = "Bahan Titipan Klien",
            options = clientOptions,
            selectedOption = clientOptions.firstOrNull { it.id == if (clientSupplied) 1L else 0L },
            onSelected = { option -> clientSupplied = option.id == 1L },
            helperText = "Atur status bahan titipan klien.",
        )
        Text(
            "Referensi Pemasok: ${
                suppliers.take(5).joinToString { "${it.id}:${it.name}" }.ifBlank { "-" }
            }",
            style = MaterialTheme.typography.caption,
            color = NeutralTextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PrimaryButton(
                text = "Simpan Data Bahan",
                enabled = canSave,
                onClick = {
                    onSaveMaterial(
                        editingId,
                        normalizedName,
                        supplierId,
                        clientSupplied,
                    )
                },
            )
            SecondaryButton(
                text = "Hapus Bahan",
                onClick = {
                    val id = materialId.toLongOrNull() ?: return@SecondaryButton
                    onDeleteMaterial(id)
                },
            )
        }
        if (materials.isNotEmpty()) {
            Text("Klik untuk edit cepat:", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            materials.take(8).forEach { item ->
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                materialId = item.id.toString()
                                materialName = item.name
                                supplierRef = item.supplierId?.toString().orEmpty()
                                clientSupplied = item.clientSupplied
                            },
                    color = NeutralLight.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.small,
                    elevation = 0.dp,
                ) {
                    Text(
                        text = "${item.id} • ${item.name} • ${item.supplierName ?: "Tanpa pemasok"}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierEditorTab(
    suppliers: List<SupplierMaster>,
    onSaveSupplier: (id: Long?, name: String) -> Unit,
    onDeleteSupplier: (id: Long) -> Unit,
) {
    var supplierId by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    val editingId = supplierId.toLongOrNull()
    val normalizedName = supplierName.normalizeHumanInput()
    val supplierDuplicate =
        normalizedName.isNotBlank() &&
            suppliers.any { item ->
                item.id != editingId && item.name.normalizeHumanInput().equals(normalizedName, ignoreCase = true)
            }
    val nameError =
        when {
            normalizedName.isBlank() -> "Nama pemasok wajib diisi."
            supplierDuplicate -> "Nama pemasok sudah terdaftar."
            else -> null
        }
    val canSave = nameError == null

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Pemasok: ${suppliers.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec =
                FieldSpec(
                    label = "ID Pemasok (kosong = tambah baru)",
                    helperText = "Isi ID untuk update. Kosongkan untuk tambah baru.",
                ),
            value = supplierId,
            onValueChange = { supplierId = it.filter(Char::isDigit) },
            singleLine = true,
        )
        AppTextField(
            spec =
                FieldSpec(
                    label = "Nama Pemasok",
                    helperText = nameError ?: "Nama pemasok unik dan dipakai lintas material.",
                    isError = nameError != null,
                ),
            value = supplierName,
            onValueChange = { supplierName = it },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PrimaryButton(
                text = "Simpan Data Pemasok",
                enabled = canSave,
                onClick = { onSaveSupplier(editingId, normalizedName) },
            )
            SecondaryButton(
                text = "Hapus Pemasok",
                onClick = {
                    val id = supplierId.toLongOrNull() ?: return@SecondaryButton
                    onDeleteSupplier(id)
                },
            )
        }
        if (suppliers.isNotEmpty()) {
            Text("Klik untuk edit cepat:", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            suppliers.take(8).forEach { item ->
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                supplierId = item.id.toString()
                                supplierName = item.name
                            },
                    color = NeutralLight.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.small,
                    elevation = 0.dp,
                ) {
                    Text(
                        text = "${item.id} • ${item.name}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun DefectEditorTab(
    defects: List<DefectMaster>,
    onSaveDefect: (id: Long?, name: String, originType: NgOriginType, lineCode: String?) -> Unit,
    onDeleteDefect: (id: Long) -> Unit,
) {
    var defectId by remember { mutableStateOf("") }
    var defectName by remember { mutableStateOf("") }
    var originRaw by remember { mutableStateOf("material") }
    var lineCode by remember { mutableStateOf("") }
    val editingId = defectId.toLongOrNull()
    val normalizedName = defectName.normalizeHumanInput()
    val normalizedOrigin = originRaw.trim().lowercase()
    val normalizedLineCode = lineCode.trim().lowercase().takeIf { it.isNotBlank() }
    val nameDuplicate =
        normalizedName.isNotBlank() &&
            defects.any { item ->
                item.id != editingId && item.name.normalizeHumanInput().equals(normalizedName, ignoreCase = true)
            }
    val nameError =
        when {
            normalizedName.isBlank() -> "Nama jenis NG wajib diisi."
            nameDuplicate -> "Nama jenis NG sudah terdaftar."
            else -> null
        }
    val originError =
        when (normalizedOrigin) {
            "material", "process" -> null
            else -> "Asal NG hanya boleh `material` atau `process`."
        }
    val lineError =
        when {
            normalizedLineCode == null -> null
            normalizedLineCode == "press" || normalizedLineCode == "sewing" -> null
            else -> "Line opsional harus `press` atau `sewing`."
        }
    val canSave = nameError == null && originError == null && lineError == null
    val originOptions =
        remember {
            listOf(
                DropdownOption(0L, "material", "NG yang dipicu oleh material/bahan."),
                DropdownOption(1L, "process", "NG yang dipicu proses produksi."),
            )
        }
    val lineOptions =
        remember {
            listOf(
                DropdownOption(0L, "Semua line", "Berlaku untuk seluruh line."),
                DropdownOption(1L, "press", "Khusus line press."),
                DropdownOption(2L, "sewing", "Khusus line sewing."),
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Jenis NG: ${defects.size}", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        AppTextField(
            spec =
                FieldSpec(
                    label = "ID Jenis NG (kosong = tambah baru)",
                    helperText = "Isi ID untuk update. Kosongkan untuk tambah baru.",
                ),
            value = defectId,
            onValueChange = { defectId = it.filter(Char::isDigit) },
            singleLine = true,
        )
        AppTextField(
            spec =
                FieldSpec(
                    label = "Nama Jenis NG",
                    helperText = nameError ?: "Nama jenis NG unik global (lintas part/bahan).",
                    isError = nameError != null,
                ),
            value = defectName,
            onValueChange = { defectName = it },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppDropdown(
                label = "Asal NG",
                options = originOptions,
                selectedOption = originOptions.firstOrNull { it.label == normalizedOrigin },
                onSelected = { option -> originRaw = option.label },
                modifier = Modifier.weight(1f),
                helperText = originError ?: "Pilih sumber NG: material atau process.",
                isError = originError != null,
            )
            AppDropdown(
                label = "Line Produksi",
                options = lineOptions,
                selectedOption =
                    lineOptions.firstOrNull {
                        when {
                            normalizedLineCode == null -> it.id == 0L
                            else -> it.label == normalizedLineCode
                        }
                    },
                onSelected = { option ->
                    lineCode = if (option.id == 0L) "" else option.label
                },
                modifier = Modifier.weight(1f),
                helperText = lineError ?: "Kosongkan jika berlaku untuk semua line.",
                isError = lineError != null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PrimaryButton(
                text = "Simpan Jenis NG",
                enabled = canSave,
                onClick = {
                    onSaveDefect(
                        editingId,
                        normalizedName,
                        if (normalizedOrigin == "process") NgOriginType.PROCESS else NgOriginType.MATERIAL,
                        normalizedLineCode,
                    )
                },
            )
            SecondaryButton(
                text = "Hapus Jenis NG",
                onClick = {
                    val id = defectId.toLongOrNull() ?: return@SecondaryButton
                    onDeleteDefect(id)
                },
            )
        }
        if (defects.isNotEmpty()) {
            Text("Klik untuk edit cepat:", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            defects.take(8).forEach { item ->
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                defectId = item.id.toString()
                                defectName = item.name
                                originRaw = if (item.originType == NgOriginType.PROCESS) "process" else "material"
                                lineCode = item.lineCode.orEmpty()
                            },
                    color = NeutralLight.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, NeutralBorder),
                    shape = MaterialTheme.shapes.small,
                    elevation = 0.dp,
                ) {
                    Text(
                        text = "${item.id} • ${item.name} • ${item.originType}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

private data class MaterialImpactDefectRow(
    val defectName: String,
    val impactedPartCount: Int,
    val sharedAcrossMaterialCount: Int,
)

private data class MaterialImpactRow(
    val materialId: Long,
    val materialName: String,
    val supplierId: Long?,
    val supplierLabel: String,
    val impactedPartIds: Set<Long>,
    val impactedPartLabels: List<String>,
    val defects: List<MaterialImpactDefectRow>,
)

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun MaterialImpactPanel(
    rows: List<MaterialImpactRow>,
    suppliers: List<SupplierMaster>,
    loading: Boolean,
    error: String?,
) {
    var selectedMaterialId by remember { mutableStateOf<Long?>(null) }
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }

    val materialOptions =
        remember(rows) {
            listOf(DropdownOption(-1L, "Semua bahan", "Lihat relasi seluruh material.")) +
                rows.map { row ->
                    DropdownOption(
                        id = row.materialId,
                        label = row.materialName,
                        helper = "Pemasok: ${row.supplierLabel}",
                    )
                }
        }
    val supplierOptions =
        remember(suppliers) {
            listOf(DropdownOption(-1L, "Semua pemasok", "Tanpa filter pemasok")) +
                suppliers.map { supplier ->
                    DropdownOption(supplier.id, supplier.name)
                }
        }

    val filteredRows =
        rows.filter { row ->
            val byMaterial = selectedMaterialId == null || row.materialId == selectedMaterialId
            val bySupplier = selectedSupplierId == null || row.supplierId == selectedSupplierId
            byMaterial && bySupplier
        }
    val uniquePartCount = filteredRows.flatMap { it.impactedPartIds }.toSet().size
    val uniqueDefectCount = filteredRows.flatMap { row -> row.defects.map { it.defectName } }.toSet().size
    val sharedDefectCount =
        filteredRows.flatMap { row -> row.defects.filter { it.sharedAcrossMaterialCount > 1 } }.count()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralLight.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = "Panel Cerdas Relasi Dampak",
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Material -> Jenis NG -> Part -> Pemasok (sumber data master real-time).",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppDropdown(
                    label = "Filter Material",
                    options = materialOptions,
                    selectedOption =
                        materialOptions.firstOrNull {
                            it.id == (selectedMaterialId ?: -1L)
                        },
                    onSelected = { option ->
                        selectedMaterialId = if (option.id == -1L) null else option.id
                    },
                    modifier = Modifier.weight(1f),
                )
                AppDropdown(
                    label = "Filter Pemasok",
                    options = supplierOptions,
                    selectedOption =
                        supplierOptions.firstOrNull {
                            it.id == (selectedSupplierId ?: -1L)
                        },
                    onSelected = { option ->
                        selectedSupplierId = if (option.id == -1L) null else option.id
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppBadge(
                    text = "Bahan ${filteredRows.size}",
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralText,
                )
                AppBadge(
                    text = "Jenis NG $uniqueDefectCount",
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralText,
                )
                AppBadge(
                    text = "Part terdampak $uniquePartCount",
                    backgroundColor = NeutralSurface,
                    contentColor = NeutralText,
                )
                AppBadge(
                    text = "Nama NG ganda $sharedDefectCount",
                    backgroundColor = NeutralSurface,
                    contentColor = MaterialTheme.colors.primary,
                )
            }

            when {
                loading -> {
                    Text(
                        text = "Menyusun relasi dampak material...",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }

                error != null -> {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error,
                    )
                }

                filteredRows.isEmpty() -> {
                    Text(
                        text = "Belum ada relasi dampak material yang bisa ditampilkan.",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }

                else -> {
                    filteredRows.take(6).forEach { row ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeutralSurface,
                            border = BorderStroke(1.dp, NeutralBorder),
                            shape = MaterialTheme.shapes.small,
                            elevation = 0.dp,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = row.materialName,
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    AppBadge(
                                        text = row.supplierLabel,
                                        backgroundColor = NeutralLight,
                                        contentColor = NeutralTextMuted,
                                    )
                                }
                                val partPreview = row.impactedPartLabels.take(4).joinToString(" | ")
                                val partSuffix = if (row.impactedPartLabels.size > 4) " ..." else ""
                                Text(
                                    text = "Part terdampak: $partPreview$partSuffix",
                                    style = MaterialTheme.typography.caption,
                                    color = NeutralTextMuted,
                                )
                                row.defects.take(5).forEach { defect ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = defect.defectName,
                                            style = MaterialTheme.typography.caption,
                                            color = NeutralText,
                                            modifier = Modifier.weight(1f),
                                        )
                                        AppBadge(
                                            text = "Part ${defect.impactedPartCount}",
                                            backgroundColor = NeutralLight,
                                            contentColor = NeutralTextMuted,
                                        )
                                        if (defect.sharedAcrossMaterialCount > 1) {
                                            AppBadge(
                                                text = "Nama sama di ${defect.sharedAcrossMaterialCount} bahan",
                                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                                                contentColor = MaterialTheme.colors.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
private suspend fun buildMaterialImpactRows(
    parts: List<PartMasterListItem>,
    materials: List<MaterialMaster>,
    defects: List<DefectMaster>,
    loadPartDetail: suspend (Long) -> PartMasterDetail?,
): List<MaterialImpactRow> {
    if (parts.isEmpty() || materials.isEmpty()) return emptyList()
    val partMap = parts.associateBy { it.id }
    val materialById = materials.associateBy { it.id }
    val defectById = defects.associateBy { it.id }
    val details = parts.mapNotNull { part -> loadPartDetail(part.id) }

    val materialDefectPartMap = mutableMapOf<Long, MutableMap<String, MutableSet<Long>>>()
    details.forEach { detail ->
        val partMaterialIds = detail.materials.map { it.materialId }.toSet()
        if (partMaterialIds.isEmpty()) return@forEach

        detail.defects
            .filter { it.originType == NgOriginType.MATERIAL }
            .forEach { assignment ->
                val defectName =
                    defectById[assignment.defectId]?.name?.normalizeHumanInput()
                        ?: assignment.defectName.normalizeHumanInput()
                val targetMaterialIds =
                    if (assignment.materialId != null) {
                        listOfNotNull(assignment.materialId)
                    } else {
                        partMaterialIds.toList()
                    }
                targetMaterialIds.forEach { materialId ->
                    if (!materialById.containsKey(materialId)) return@forEach
                    materialDefectPartMap
                        .getOrPut(materialId) { mutableMapOf() }
                        .getOrPut(defectName) { mutableSetOf() }
                        .add(detail.id)
                }
            }
    }

    val defectSpreadMap = mutableMapOf<String, MutableSet<Long>>()
    materialDefectPartMap.forEach { (materialId, defectMap) ->
        defectMap.keys.forEach { defectName ->
            defectSpreadMap.getOrPut(defectName) { mutableSetOf() }.add(materialId)
        }
    }

    return materials
        .sortedBy { it.name.lowercase() }
        .map { material ->
            val defectMap = materialDefectPartMap[material.id].orEmpty()
            val impactedPartIds = defectMap.values.flatten().toSet()
            val defectRows =
                defectMap.entries
                    .sortedByDescending { it.value.size }
                    .map { (defectName, partIds) ->
                        MaterialImpactDefectRow(
                            defectName = defectName,
                            impactedPartCount = partIds.size,
                            sharedAcrossMaterialCount = defectSpreadMap[defectName]?.size ?: 1,
                        )
                    }
            MaterialImpactRow(
                materialId = material.id,
                materialName = material.name,
                supplierId = material.supplierId,
                supplierLabel = material.supplierName ?: "Tanpa pemasok",
                impactedPartIds = impactedPartIds,
                impactedPartLabels =
                    impactedPartIds
                        .mapNotNull { partId ->
                            partMap[partId]?.let { part -> "${part.uniqNo}/${part.partNumber}" }
                        }.sorted(),
                defects = defectRows,
            )
        }.filter { row ->
            row.defects.isNotEmpty() || row.impactedPartIds.isNotEmpty()
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

private fun String.normalizeHumanInput(): String = trim().replace("\\s+".toRegex(), " ")
