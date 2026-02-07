package id.co.nierstyd.mutugemba.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.data.AttachmentStore
import id.co.nierstyd.mutugemba.data.FileSettingsRepository
import id.co.nierstyd.mutugemba.data.SettingsSessionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightAppDataResetter
import id.co.nierstyd.mutugemba.data.SqlDelightInspectionRepository
import id.co.nierstyd.mutugemba.data.SqlDelightMasterDataRepository
import id.co.nierstyd.mutugemba.data.db.DatabaseFactory
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.FooterBar
import id.co.nierstyd.mutugemba.desktop.ui.components.HeaderBar
import id.co.nierstyd.mutugemba.desktop.ui.layout.AppLayout
import id.co.nierstyd.mutugemba.desktop.ui.screens.AbnormalScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.HomeScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionDefaultsUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionPolicyUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.MasterDataUseCaseBundle
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.theme.MutuGembaTheme
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
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
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDemoModeUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevDummyDataUseCase
import id.co.nierstyd.mutugemba.usecase.SetDevQcLineUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.SaveManualHolidayDatesUseCase

@Composable
fun MutuGembaApp() {
    val settingsRepository = remember { FileSettingsRepository(AppDataPaths.settingsFile()) }
    val sessionRepository = remember { SettingsSessionRepository(settingsRepository) }
    val getLastPageUseCase = remember { GetLastVisitedPageUseCase(settingsRepository) }
    val setLastPageUseCase = remember { SetLastVisitedPageUseCase(settingsRepository) }
    val getInspectionDefaultsUseCase = remember { GetInspectionDefaultsUseCase(settingsRepository) }
    val saveInspectionDefaultsUseCase = remember { SaveInspectionDefaultsUseCase(settingsRepository) }
    val getAllowDuplicateInspectionUseCase = remember { GetAllowDuplicateInspectionUseCase(settingsRepository) }
    val setAllowDuplicateInspectionUseCase = remember { SetAllowDuplicateInspectionUseCase(settingsRepository) }
    val getDevQcLineUseCase = remember { GetDevQcLineUseCase(settingsRepository) }
    val setDevQcLineUseCase = remember { SetDevQcLineUseCase(settingsRepository) }
    val getDevDemoModeUseCase = remember { GetDevDemoModeUseCase(settingsRepository) }
    val setDevDemoModeUseCase = remember { SetDevDemoModeUseCase(settingsRepository) }
    val getDevDummyDataUseCase = remember { GetDevDummyDataUseCase(settingsRepository) }
    val setDevDummyDataUseCase = remember { SetDevDummyDataUseCase(settingsRepository) }
    val getManualHolidayDatesUseCase = remember { GetManualHolidayDatesUseCase(settingsRepository) }
    val saveManualHolidayDatesUseCase = remember { SaveManualHolidayDatesUseCase(settingsRepository) }

    val databaseHandle = remember { DatabaseFactory.createDatabaseHandle(AppDataPaths.databaseFile()) }
    val database = databaseHandle.database
    val inspectionRepository = remember { SqlDelightInspectionRepository(database) }
    val masterDataRepository = remember { SqlDelightMasterDataRepository(database) }
    val dataResetter = remember { SqlDelightAppDataResetter(database, databaseHandle.driver) }
    val resetDataUseCase =
        remember {
            ResetDataUseCase(
                dataResetter,
                sessionRepository,
                listOf(masterDataRepository, inspectionRepository),
            )
        }
    val createInspectionUseCase = remember { CreateInspectionRecordUseCase(inspectionRepository) }
    val createBatchInspectionUseCase =
        remember { CreateBatchInspectionRecordsUseCase(createInspectionUseCase) }
    val getRecentInspectionsUseCase = remember { GetRecentInspectionsUseCase(inspectionRepository) }
    val getDailySummariesUseCase = remember { GetMonthlyDailyChecksheetSummariesUseCase(inspectionRepository) }
    val getDailyDetailUseCase = remember { GetDailyChecksheetDetailUseCase(inspectionRepository) }
    val getMonthlyDefectSummaryUseCase = remember { GetMonthlyDefectSummaryUseCase(inspectionRepository) }
    val getLinesUseCase = remember { GetLinesUseCase(masterDataRepository) }
    val getShiftsUseCase = remember { GetShiftsUseCase(masterDataRepository) }
    val getPartsUseCase = remember { GetPartsUseCase(masterDataRepository) }
    val getDefectTypesUseCase = remember { GetDefectTypesUseCase(masterDataRepository) }
    val inspectionDependencies =
        remember {
            InspectionScreenDependencies(
                defaults =
                    InspectionDefaultsUseCases(
                        getDefaults = getInspectionDefaultsUseCase,
                        saveDefaults = saveInspectionDefaultsUseCase,
                    ),
                createInspectionUseCase = createInspectionUseCase,
                createBatchInspectionUseCase = createBatchInspectionUseCase,
                policies =
                    InspectionPolicyUseCases(
                        getAllowDuplicate = getAllowDuplicateInspectionUseCase,
                    ),
                masterData =
                    MasterDataUseCaseBundle(
                        getLines = getLinesUseCase,
                        getShifts = getShiftsUseCase,
                        getParts = getPartsUseCase,
                        getDefectTypes = getDefectTypesUseCase,
                    ),
            )
        }
    val attachmentStore = remember { AttachmentStore(AppDataPaths.attachmentsDir()) }

    var currentRoute by remember {
        mutableStateOf(
            AppRoute.fromKey(getLastPageUseCase.execute()) ?: AppRoute.Home,
        )
    }
    var inspectionRecords by remember { mutableStateOf<List<InspectionRecord>>(emptyList()) }
    var lines by remember { mutableStateOf<List<Line>>(emptyList()) }
    var dailySummaries by remember {
        mutableStateOf<List<DailyChecksheetSummary>>(emptyList())
    }
    val refreshData = {
        inspectionRecords = getRecentInspectionsUseCase.execute()
        lines = getLinesUseCase.execute()
        dailySummaries = getDailySummariesUseCase.execute()
    }

    LaunchedEffect(Unit) {
        attachmentStore.ensureBaseDir()
        refreshData()
    }

    LaunchedEffect(currentRoute) {
        setLastPageUseCase.execute(currentRoute.key)
    }

    MutuGembaTheme {
        AppLayout(
            routes = AppRoute.values().toList(),
            currentRoute = currentRoute,
            onRouteSelected = { currentRoute = it },
            scrollableContent = currentRoute != AppRoute.Inspection && currentRoute != AppRoute.Reports,
            headerContent = {
                HeaderBar(
                    title = "MutuGemba",
                    subtitle = "PT. Primaraya Graha Nusantara",
                )
            },
            footerContent = {
                FooterBar(
                    statusText = "Offline - Lokal",
                    hintText = "Halaman: ${currentRoute.label}",
                )
            },
        ) {
            when (currentRoute) {
                AppRoute.Home ->
                    HomeScreen(
                        recentRecords = inspectionRecords,
                        lines = lines,
                        resetData = resetDataUseCase,
                        onNavigateToInspection = { currentRoute = AppRoute.Inspection },
                        onRefreshData = refreshData,
                    )

                AppRoute.Inspection ->
                    InspectionScreen(
                        dependencies = inspectionDependencies,
                        onRecordsSaved = {
                            inspectionRecords = getRecentInspectionsUseCase.execute()
                        },
                    )

                AppRoute.Abnormal -> AbnormalScreen()
                AppRoute.Reports ->
                    ReportsScreen(
                        lines = lines,
                        dailySummaries = dailySummaries,
                        loadDailyDetail = { lineId, date -> getDailyDetailUseCase.execute(lineId, date) },
                        loadMonthlyDefectSummary = { month -> getMonthlyDefectSummaryUseCase.execute(month) },
                        loadManualHolidays = { getManualHolidayDatesUseCase.execute() },
                        saveManualHolidays = { saveManualHolidayDatesUseCase.execute(it) },
                    )
                AppRoute.Settings ->
                    SettingsScreen(
                        dependencies =
                            SettingsScreenDependencies(
                                getAllowDuplicateInspection = getAllowDuplicateInspectionUseCase,
                                setAllowDuplicateInspection = setAllowDuplicateInspectionUseCase,
                                resetData = resetDataUseCase,
                                getLines = getLinesUseCase,
                                getDevQcLine = getDevQcLineUseCase,
                                setDevQcLine = setDevQcLineUseCase,
                                getDevDemoMode = getDevDemoModeUseCase,
                                setDevDemoMode = setDevDemoModeUseCase,
                                getDevDummyData = getDevDummyDataUseCase,
                                setDevDummyData = setDevDummyDataUseCase,
                                onResetCompleted = refreshData,
                            ),
                    )
            }
        }
    }
}
