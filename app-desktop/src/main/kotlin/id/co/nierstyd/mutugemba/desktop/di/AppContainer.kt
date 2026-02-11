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
import id.co.nierstyd.mutugemba.data.local.db.SqlitePartRepository
import id.co.nierstyd.mutugemba.data.local.db.SqliteQaRepository
import id.co.nierstyd.mutugemba.usecase.BackupDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.CreateBatchInspectionRecordsUseCase
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDailyChecksheetDetailUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyChecksheetEntriesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDailyChecksheetSummariesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDefectSummaryUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyReportDocumentUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetRecentInspectionsUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.RestoreDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.UpsertDefectTypeUseCase
import id.co.nierstyd.mutugemba.usecase.asset.GetActiveImageRefUseCase
import id.co.nierstyd.mutugemba.usecase.asset.LoadImageBytesUseCase
import id.co.nierstyd.mutugemba.usecase.part.GetPartDetailUseCase
import id.co.nierstyd.mutugemba.usecase.part.ObservePartsUseCase
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
    private val qaRepository = SqliteQaRepository(mappingDatabase)
    private val assetRepository = LocalAssetRepository(mappingDatabase, hashAssetStore)
    private val partZipBootstrapper = PartZipBootstrapper(mappingDatabase, hashAssetStore)

    val getLastPageUseCase = GetLastVisitedPageUseCase(settingsRepository)
    val setLastPageUseCase = SetLastVisitedPageUseCase(settingsRepository)
    val getInspectionDefaultsUseCase = GetInspectionDefaultsUseCase(settingsRepository)
    val saveInspectionDefaultsUseCase = SaveInspectionDefaultsUseCase(settingsRepository)
    val getAllowDuplicateInspectionUseCase = GetAllowDuplicateInspectionUseCase(settingsRepository)
    val setAllowDuplicateInspectionUseCase = SetAllowDuplicateInspectionUseCase(settingsRepository)
    val getDevQcLineUseCase = GetDevQcLineUseCase(settingsRepository)
    val setDevQcLineUseCase = SetDevQcLineUseCase(settingsRepository)
    val getManualHolidayDatesUseCase = GetManualHolidayDatesUseCase(settingsRepository)
    val saveManualHolidayDatesUseCase = SaveManualHolidayDatesUseCase(settingsRepository)

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

        repeat(days.coerceAtLeast(1)) { dayOffset ->
            val date = now.minusDays(dayOffset.toLong())
            lines.forEach { line ->
                val lineParts = parts.filter { it.lineCode == line.code }
                if (lineParts.isEmpty()) return@forEach
                val batchSize = (lineParts.size * 0.4).toInt().coerceIn(8, lineParts.size)
                lineParts.shuffled(random).take(batchSize).forEachIndexed { slotIndex, part ->
                    repeat(density.coerceAtLeast(1)) { densityIndex ->
                        val candidateDefects =
                            part.recommendedDefectCodes
                                .mapNotNull { code -> defectTypes.firstOrNull { it.code == code } }
                                .ifEmpty {
                                    defectTypes
                                        .filter { it.lineCode == null || it.lineCode == line.code }
                                        .shuffled(random)
                                        .take(3)
                                }
                        if (candidateDefects.isEmpty()) return@repeat

                        val picked =
                            candidateDefects
                                .shuffled(random)
                                .take((1 + random.nextInt(2)).coerceAtMost(candidateDefects.size))
                        val entries =
                            picked.map { defect ->
                                val q1 = random.nextInt(0, 4)
                                val q2 = random.nextInt(0, 4)
                                val q3 = random.nextInt(0, 3)
                                val total = q1 + q2 + q3
                                id.co.nierstyd.mutugemba.domain.InspectionDefectEntry(
                                    defectTypeId = defect.id,
                                    quantity = total.coerceAtLeast(1),
                                    slots = emptyList(),
                                )
                            }
                        val totalDefect = entries.sumOf { it.quantity }.coerceAtLeast(1)
                        val totalCheck = totalDefect + random.nextInt(8, 40)
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
        return inserted
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
