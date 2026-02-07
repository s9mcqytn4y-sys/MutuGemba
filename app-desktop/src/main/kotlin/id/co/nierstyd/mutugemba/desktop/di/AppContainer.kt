package id.co.nierstyd.mutugemba.desktop.di

import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.data.AttachmentStore
import id.co.nierstyd.mutugemba.data.FileSettingsRepository
import id.co.nierstyd.mutugemba.data.LocalBackupRepository
import id.co.nierstyd.mutugemba.data.SettingsSessionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightAppDataResetter
import id.co.nierstyd.mutugemba.data.SqlDelightInspectionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightMasterDataRepository
import id.co.nierstyd.mutugemba.data.db.DatabaseFactory
import id.co.nierstyd.mutugemba.usecase.BackupDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.CreateBatchInspectionRecordsUseCase
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDailyChecksheetDetailUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevDemoModeUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevDummyDataUseCase
import id.co.nierstyd.mutugemba.usecase.GetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDailyChecksheetSummariesUseCase
import id.co.nierstyd.mutugemba.usecase.GetMonthlyDefectSummaryUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetRecentInspectionsUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.RestoreDatabaseUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveManualHolidayDatesUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDemoModeUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDummyDataUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastVisitedPageUseCase

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
        )

    val getLastPageUseCase = GetLastVisitedPageUseCase(settingsRepository)
    val setLastPageUseCase = SetLastVisitedPageUseCase(settingsRepository)
    val getInspectionDefaultsUseCase = GetInspectionDefaultsUseCase(settingsRepository)
    val saveInspectionDefaultsUseCase = SaveInspectionDefaultsUseCase(settingsRepository)
    val getAllowDuplicateInspectionUseCase = GetAllowDuplicateInspectionUseCase(settingsRepository)
    val setAllowDuplicateInspectionUseCase = SetAllowDuplicateInspectionUseCase(settingsRepository)
    val getDevQcLineUseCase = GetDevQcLineUseCase(settingsRepository)
    val setDevQcLineUseCase = SetDevQcLineUseCase(settingsRepository)
    val getDevDemoModeUseCase = GetDevDemoModeUseCase(settingsRepository)
    val setDevDemoModeUseCase = SetDevDemoModeUseCase(settingsRepository)
    val getDevDummyDataUseCase = GetDevDummyDataUseCase(settingsRepository)
    val setDevDummyDataUseCase = SetDevDummyDataUseCase(settingsRepository)
    val getManualHolidayDatesUseCase = GetManualHolidayDatesUseCase(settingsRepository)
    val saveManualHolidayDatesUseCase = SaveManualHolidayDatesUseCase(settingsRepository)

    val createInspectionUseCase = CreateInspectionRecordUseCase(inspectionRepository)
    val createBatchInspectionUseCase = CreateBatchInspectionRecordsUseCase(createInspectionUseCase)
    val getRecentInspectionsUseCase = GetRecentInspectionsUseCase(inspectionRepository)
    val getDailySummariesUseCase = GetMonthlyDailyChecksheetSummariesUseCase(inspectionRepository)
    val getDailyDetailUseCase = GetDailyChecksheetDetailUseCase(inspectionRepository)
    val getMonthlyDefectSummaryUseCase = GetMonthlyDefectSummaryUseCase(inspectionRepository)
    val getLinesUseCase = GetLinesUseCase(masterDataRepository)
    val getShiftsUseCase = GetShiftsUseCase(masterDataRepository)
    val getPartsUseCase = GetPartsUseCase(masterDataRepository)
    val getDefectTypesUseCase = GetDefectTypesUseCase(masterDataRepository)
    val resetDataUseCase =
        ResetDataUseCase(
            dataResetter,
            sessionRepository,
            listOf(masterDataRepository, inspectionRepository),
        )
    val backupDatabaseUseCase = BackupDatabaseUseCase(backupRepository)
    val restoreDatabaseUseCase = RestoreDatabaseUseCase(backupRepository)

    fun close() {
        databaseHandle.driver.close()
    }
}
