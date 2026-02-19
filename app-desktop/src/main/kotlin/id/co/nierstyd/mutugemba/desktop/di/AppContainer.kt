package id.co.nierstyd.mutugemba.desktop.di

import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.data.AttachmentStore
import id.co.nierstyd.mutugemba.data.FileSettingsRepository
import id.co.nierstyd.mutugemba.data.LocalBackupRepository
import id.co.nierstyd.mutugemba.data.SettingsSessionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightAppDataResetter
import id.co.nierstyd.mutugemba.data.SqlDelightInspectionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightMasterDataRepository
import id.co.nierstyd.mutugemba.data.bootstrap.PartZipBootstrapper
import id.co.nierstyd.mutugemba.data.db.DatabaseFactory
import id.co.nierstyd.mutugemba.data.local.assetstore.LocalAssetRepository
import id.co.nierstyd.mutugemba.data.local.assetstore.createDesktopHashAssetStore
import id.co.nierstyd.mutugemba.data.local.db.SqlitePartMasterRepository
import id.co.nierstyd.mutugemba.data.local.db.SqlitePartRepository
import id.co.nierstyd.mutugemba.data.local.db.SqliteQaRepository
import id.co.nierstyd.mutugemba.usecase.AppendReportArchiveEntryUseCase
import id.co.nierstyd.mutugemba.usecase.BackupDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.CreateBatchInspectionRecordsUseCase
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDailyChecksheetDetailUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionPartDefectLayoutUseCase
import id.co.nierstyd.mutugemba.usecase.GetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyChecksheetEntriesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDailyChecksheetSummariesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDefectSummaryUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyReportDocumentUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetRecentInspectionsUseCase
import id.co.nierstyd.mutugemba.usecase.GetReportArchiveEntriesUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.RestoreDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionPartDefectLayoutUseCase
import id.co.nierstyd.mutugemba.usecase.SaveManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.UpsertDefectTypeUseCase
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Comparator
import kotlin.random.Random
import id.co.nierstyd.mutugemba.data.local.db.DatabaseFactory as MappingDatabaseFactory

class AppContainer {
    val settingsRepository = FileSettingsRepository(AppDataPaths.settingsFile())
    val sessionRepository = SettingsSessionRepository(settingsRepository)

    private val databaseHandle = DatabaseFactory.createDatabaseHandle(AppDataPaths.databaseFile())
    private val database = databaseHandle.database

    val inspectionRepository = SqlDelightInspectionRepository(database)
    val masterDataRepository = SqlDelightMasterDataRepository(database)
    val attachmentStore = AttachmentStore(AppDataPaths.attachmentsDir())
    private val dataResetter = SqlDelightAppDataResetter(database, databaseHandle.driver)
    private val backupRepository =
        LocalBackupRepository(
            baseDir = AppDataPaths.dataDir(),
            databaseFile = AppDataPaths.databaseFile(),
            settingsFile = AppDataPaths.settingsFile(),
            attachmentsDir = AppDataPaths.attachmentsDir(),
            assetsStoreDir = AppDataPaths.assetsStoreDir(),
        )
    private val mappingDatabase = MappingDatabaseFactory.create()
    private val hashAssetStore = createDesktopHashAssetStore(AppDataPaths.dataDir())
    private val partRepository = SqlitePartRepository(mappingDatabase)
    private val partMasterRepository = SqlitePartMasterRepository(mappingDatabase)
    private val qaRepository = SqliteQaRepository(mappingDatabase)
    private val assetRepository = LocalAssetRepository(mappingDatabase, hashAssetStore)
    private val partZipBootstrapper = PartZipBootstrapper(mappingDatabase, hashAssetStore)

    val getLastPageUseCase = GetLastVisitedPageUseCase(settingsRepository)
    val setLastPageUseCase = SetLastVisitedPageUseCase(settingsRepository)
    val getInspectionDefaultsUseCase = GetInspectionDefaultsUseCase(settingsRepository)
    val saveInspectionDefaultsUseCase = SaveInspectionDefaultsUseCase(settingsRepository)
    val getInspectionPartDefectLayoutUseCase = GetInspectionPartDefectLayoutUseCase(settingsRepository)
    val saveInspectionPartDefectLayoutUseCase = SaveInspectionPartDefectLayoutUseCase(settingsRepository)
    val getAllowDuplicateInspectionUseCase = GetAllowDuplicateInspectionUseCase(settingsRepository)
    val setAllowDuplicateInspectionUseCase = SetAllowDuplicateInspectionUseCase(settingsRepository)
    val getDevQcLineUseCase = GetDevQcLineUseCase(settingsRepository)
    val setDevQcLineUseCase = SetDevQcLineUseCase(settingsRepository)
    val getManualHolidayDatesUseCase = GetManualHolidayDatesUseCase(settingsRepository)
    val saveManualHolidayDatesUseCase = SaveManualHolidayDatesUseCase(settingsRepository)
    val getReportArchiveEntriesUseCase = GetReportArchiveEntriesUseCase(settingsRepository)
    val appendReportArchiveEntryUseCase = AppendReportArchiveEntryUseCase(settingsRepository)

    val createInspectionUseCase = CreateInspectionRecordUseCase(inspectionRepository)
    val createBatchInspectionUseCase = CreateBatchInspectionRecordsUseCase(createInspectionUseCase)
    val getRecentInspectionsUseCase = GetRecentInspectionsUseCase(inspectionRepository)
    val getDailySummariesUseCase = GetMonthlyDailyChecksheetSummariesUseCase(inspectionRepository)
    val getDailyDetailUseCase = GetDailyChecksheetDetailUseCase(inspectionRepository)
    val getMonthlyDefectSummaryUseCase = GetMonthlyDefectSummaryUseCase(inspectionRepository)
    val getMonthlyEntriesUseCase = GetMonthlyChecksheetEntriesUseCase(inspectionRepository)
    val getMonthlyReportDocumentUseCase = GetMonthlyReportDocumentUseCase(inspectionRepository, masterDataRepository)
    val getLinesUseCase = GetLinesUseCase(masterDataRepository)
    val getShiftsUseCase = GetShiftsUseCase(masterDataRepository)
    val getPartsUseCase = GetPartsUseCase(masterDataRepository)
    val getDefectTypesUseCase = GetDefectTypesUseCase(masterDataRepository)
    val upsertDefectTypeUseCase = UpsertDefectTypeUseCase(masterDataRepository)
    val resetDataUseCase =
        ResetDataUseCase(
            dataResetter,
            sessionRepository,
            listOf(masterDataRepository, inspectionRepository),
        )
    val backupDatabaseUseCase = BackupDatabaseUseCase(backupRepository)
    val restoreDatabaseUseCase = RestoreDatabaseUseCase(backupRepository)
    val observePartsUseCase = ObservePartsUseCase(partRepository)
    val getPartDetailUseCase = GetPartDetailUseCase(partRepository)
    val listPartMastersUseCase = ListPartMastersUseCase(partMasterRepository)
    val getPartMasterDetailUseCase = GetPartMasterDetailUseCase(partMasterRepository)
    val savePartMasterUseCase = SavePartMasterUseCase(partMasterRepository)
    val replacePartMaterialsUseCase = ReplacePartMaterialsUseCase(partMasterRepository)
    val replacePartDefectsUseCase = ReplacePartDefectsUseCase(partMasterRepository)
    val listMaterialMastersUseCase = ListMaterialMastersUseCase(partMasterRepository)
    val saveMaterialMasterUseCase = SaveMaterialMasterUseCase(partMasterRepository)
    val deleteMaterialMasterUseCase = DeleteMaterialMasterUseCase(partMasterRepository)
    val listSupplierMastersUseCase = ListSupplierMastersUseCase(partMasterRepository)
    val saveSupplierMasterUseCase = SaveSupplierMasterUseCase(partMasterRepository)
    val deleteSupplierMasterUseCase = DeleteSupplierMasterUseCase(partMasterRepository)
    val listDefectMastersUseCase = ListDefectMastersUseCase(partMasterRepository)
    val saveDefectMasterUseCase = SaveDefectMasterUseCase(partMasterRepository)
    val deleteDefectMasterUseCase = DeleteDefectMasterUseCase(partMasterRepository)
    val getTopDefectsPerModelMonthlyUseCase = GetTopDefectsPerModelMonthlyUseCase(qaRepository)
    val getDefectHeatmapUseCase = GetDefectHeatmapUseCase(qaRepository)
    val getActiveImageRefUseCase = GetActiveImageRefUseCase(assetRepository)
    val loadImageBytesUseCase = LoadImageBytesUseCase(assetRepository)

    init {
        bootstrapPartDomainFromExtracted()
    }

    fun close() {
        databaseHandle.driver.close()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun generateHighVolumeSimulation(
        days: Int = 45,
        density: Int = 4,
    ): Int {
        val lines = masterDataRepository.getLines()
        val shifts = masterDataRepository.getShifts()
        val parts = masterDataRepository.getParts()
        val defectTypes = masterDataRepository.getDefectTypes()
        if (lines.isEmpty() || shifts.isEmpty() || parts.isEmpty() || defectTypes.isEmpty()) return 0

        val shiftId = shifts.first().id
        val now = LocalDate.now()
        val random = Random(240211L)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        var inserted = 0

        database.withBulkMutation {
            repeat(days.coerceAtLeast(1)) { dayOffset ->
                val date = now.minusDays(dayOffset.toLong())
                val isWeekend = date.dayOfWeek.value >= 6
                lines.forEach { line ->
                    val lineParts = parts.filter { it.lineCode == line.code }
                    if (lineParts.isEmpty()) return@forEach
                    val densityMultiplier =
                        when {
                            isWeekend -> 0.35
                            dayOffset % 7 == 1 -> 0.95
                            else -> 0.65
                        }
                    val minBatch = if (isWeekend) 2 else 6
                    val batchSize =
                        (lineParts.size * densityMultiplier)
                            .toInt()
                            .coerceIn(minBatch.coerceAtMost(lineParts.size), lineParts.size)
                    lineParts.shuffled(random).take(batchSize).forEachIndexed { slotIndex, part ->
                        val repeatPerPart =
                            if (isWeekend) {
                                1
                            } else {
                                density.coerceAtLeast(1)
                            }
                        repeat(repeatPerPart) { densityIndex ->
                            val candidateDefects = candidateDefectsForPart(part.id, line.id, defectTypes)
                            if (candidateDefects.isEmpty()) return@repeat

                            val pickedCount =
                                when {
                                    candidateDefects.size == 1 -> 1
                                    random.nextInt(100) < 70 -> 1
                                    else -> 2
                                }
                            val picked = candidateDefects.shuffled(random).take(pickedCount)
                            val entries =
                                picked
                                    .map { defect ->
                                        val baseQty = if (isWeekend) random.nextInt(0, 3) else random.nextInt(1, 5)
                                        val spike =
                                            if (!isWeekend && random.nextInt(100) < 18) {
                                                random.nextInt(2, 7)
                                            } else {
                                                0
                                            }
                                        id.co.nierstyd.mutugemba.domain.InspectionDefectEntry(
                                            defectTypeId = defect.id,
                                            quantity = (baseQty + spike).coerceAtLeast(1),
                                            slots = emptyList(),
                                        )
                                    }.filter { it.quantity > 0 }
                            if (entries.isEmpty()) return@repeat

                            val totalDefect = entries.sumOf { it.quantity }.coerceAtLeast(1)
                            val baseCheck = if (isWeekend) random.nextInt(6, 20) else random.nextInt(18, 55)
                            val totalCheck = (totalDefect + baseCheck).coerceAtLeast(totalDefect)
                            val clock = LocalTime.of((8 + (slotIndex % 8)).coerceAtMost(16), random.nextInt(0, 59))
                            val createdAt = LocalDateTime.of(date, clock).plusMinutes((densityIndex * 7).toLong())
                            inspectionRepository.insert(
                                id.co.nierstyd.mutugemba.domain.InspectionInput(
                                    kind = id.co.nierstyd.mutugemba.domain.InspectionKind.DEFECT,
                                    lineId = line.id,
                                    shiftId = shiftId,
                                    partId = part.id,
                                    totalCheck = totalCheck,
                                    defectTypeId = null,
                                    defectQuantity = null,
                                    defects = entries,
                                    picName = "Simulasi-${line.name}",
                                    createdAt = createdAt.format(formatter),
                                ),
                            )
                            inserted += 1
                        }
                    }
                }
            }
        }
        return inserted
    }

    private fun candidateDefectsForPart(
        partId: Long,
        lineId: Long,
        defectTypes: List<id.co.nierstyd.mutugemba.domain.DefectType>,
    ): List<id.co.nierstyd.mutugemba.domain.DefectType> {
        val line = masterDataRepository.getLines().firstOrNull { it.id == lineId }
        val recommended = database.recommendedDefectTypeIds(partId)
        val preferred =
            defectTypes.filter { defect ->
                defect.id in recommended &&
                    (line == null || defect.lineCode == null || defect.lineCode == line.code)
            }
        if (preferred.isNotEmpty()) return preferred
        return defectTypes.filter { defect ->
            line == null || defect.lineCode == null || defect.lineCode == line.code
        }
    }

    fun clearAppCaches(): Boolean =
        runCatching {
            listOf(AppDataPaths.importLogsDir(), AppDataPaths.exportsDir()).forEach { path ->
                if (Files.exists(path)) {
                    Files.walk(path).use { stream ->
                        stream.sorted(Comparator.reverseOrder()).forEach { entry ->
                            if (entry != path) Files.deleteIfExists(entry)
                        }
                    }
                } else {
                    Files.createDirectories(path)
                }
            }
            true
        }.getOrDefault(false)

    private fun bootstrapPartDomainFromExtracted() {
        val candidates =
            listOf(
                AppDataPaths.defaultPartAssetsExtractedDir(),
                AppDataPaths.projectPartAssetsDir().resolve("extracted"),
                Paths.get("data", "part_assets", "extracted").toAbsolutePath().normalize(),
            ).distinct().filter { candidate ->
                Files.exists(candidate.resolve("mappings").resolve("mapping.json"))
            }
        candidates.forEach { candidate: Path ->
            val imported = partZipBootstrapper.bootstrapFromExtractedDirIfEmpty(candidate)
            if (imported != null) return
        }
    }
}
