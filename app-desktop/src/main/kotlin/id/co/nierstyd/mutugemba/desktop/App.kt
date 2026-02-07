package id.co.nierstyd.mutugemba.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import id.co.nierstyd.mutugemba.data.SampleData
import id.co.nierstyd.mutugemba.desktop.di.AppContainer
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.FooterBar
import id.co.nierstyd.mutugemba.desktop.ui.components.HeaderBar
import id.co.nierstyd.mutugemba.desktop.ui.layout.AppLayout
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.screens.AbnormalScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.HomeScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionDefaultsUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionPolicyUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.MasterDataUseCaseBundle
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsMonthlyScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.buildDemoMonthlyReportDocument
import id.co.nierstyd.mutugemba.desktop.ui.theme.MutuGembaTheme
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MutuGembaApp() {
    var container by remember { mutableStateOf(AppContainer()) }
    val scope = rememberCoroutineScope()

    var currentRoute by remember(container) {
        mutableStateOf(
            AppRoute.fromKey(container.getLastPageUseCase.execute()) ?: AppRoute.Home,
        )
    }
    var inspectionRecords by remember(container) { mutableStateOf<List<InspectionRecord>>(emptyList()) }
    var lines by remember(container) { mutableStateOf<List<Line>>(emptyList()) }
    var dailySummaries by remember(container) {
        mutableStateOf<List<DailyChecksheetSummary>>(emptyList())
    }
    var useDummyData by remember(container) {
        mutableStateOf(container.getDevDummyDataUseCase.execute())
    }
    var demoMode by remember(container) {
        mutableStateOf(container.getDevDemoModeUseCase.execute())
    }
    val demoPack =
        remember(useDummyData) {
            if (useDummyData) SampleData.buildDemoPack() else null
        }

    suspend fun loadData() {
        val (records, lineItems, summaries) =
            withContext(Dispatchers.IO) {
                if (useDummyData) {
                    val pack = demoPack ?: SampleData.buildDemoPack()
                    Triple(pack.inspectionRecords, pack.lines, pack.dailySummaries)
                } else {
                    Triple(
                        container.getRecentInspectionsUseCase.execute(),
                        container.getLinesUseCase.execute(),
                        container.getDailySummariesUseCase.execute(),
                    )
                }
            }
        inspectionRecords = records
        lines = lineItems
        dailySummaries = summaries
    }

    val refreshData: () -> Unit = {
        scope.launch { loadData() }
    }

    val loadDailyDetail: (Long, LocalDate) -> DailyChecksheetDetail? = { lineId, date ->
        if (useDummyData) {
            demoPack?.dailyDetails?.get(lineId to date)
        } else {
            container.getDailyDetailUseCase.execute(lineId, date)
        }
    }
    val loadMonthlyDefectSummary: (YearMonth) -> List<DefectSummary> = { month ->
        if (useDummyData) {
            demoPack?.monthlyDefectSummary ?: emptyList()
        } else {
            container.getMonthlyDefectSummaryUseCase.execute(month)
        }
    }
    val loadMonthlyEntries: (Long, YearMonth) -> List<ChecksheetEntry> = { lineId, month ->
        if (useDummyData) {
            demoPack
                ?.dailyDetails
                ?.filterKeys { (storedLineId, date) ->
                    storedLineId == lineId && YearMonth.from(date) == month
                }?.values
                ?.flatMap { it.entries }
                ?: emptyList()
        } else {
            container.getMonthlyEntriesUseCase.execute(lineId, month)
        }
    }

    val loadMonthlyReportDocument: (Long, YearMonth) -> MonthlyReportDocument? = { lineId, month ->
        if (useDummyData) {
            val pack = demoPack ?: SampleData.buildDemoPack()
            buildDemoMonthlyReportDocument(pack, lineId, month)
        } else {
            container.getMonthlyReportDocumentUseCase.execute(lineId, month)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            container.attachmentStore.ensureBaseDir()
        }
    }

    LaunchedEffect(container, useDummyData) {
        loadData()
    }

    LaunchedEffect(currentRoute) {
        container.setLastPageUseCase.execute(currentRoute.key)
    }

    MutuGembaTheme {
        AppLayout(
            routes = AppRoute.values().toList(),
            currentRoute = currentRoute,
            onRouteSelected = { currentRoute = it },
            scrollableContent = currentRoute != AppRoute.Inspection && currentRoute != AppRoute.Home,
            headerContent = {
                HeaderBar(
                    title = AppStrings.App.Name,
                    subtitle = AppStrings.App.CompanyName,
                    demoMode = demoMode,
                    dummyData = useDummyData,
                )
            },
            footerContent = {
                FooterBar(
                    statusText = AppStrings.App.OfflineStatus,
                    hintText = AppStrings.App.footerPage(currentRoute.label),
                )
            },
        ) {
            when (currentRoute) {
                AppRoute.Home ->
                    HomeScreen(
                        recentRecords = inspectionRecords,
                        lines = lines,
                        dailySummaries = dailySummaries,
                        loadDailyDetail = loadDailyDetail,
                        loadMonthlyDefectSummary = loadMonthlyDefectSummary,
                        loadMonthlyEntries = loadMonthlyEntries,
                        resetData = container.resetDataUseCase,
                        onNavigateToInspection = { currentRoute = AppRoute.Inspection },
                        onRefreshData = refreshData,
                    )

                AppRoute.Inspection ->
                    InspectionScreen(
                        dependencies =
                            InspectionScreenDependencies(
                                defaults =
                                    InspectionDefaultsUseCases(
                                        getDefaults = container.getInspectionDefaultsUseCase,
                                        saveDefaults = container.saveInspectionDefaultsUseCase,
                                    ),
                                createInspectionUseCase = container.createInspectionUseCase,
                                createBatchInspectionUseCase = container.createBatchInspectionUseCase,
                                policies =
                                    InspectionPolicyUseCases(
                                        getAllowDuplicate = container.getAllowDuplicateInspectionUseCase,
                                    ),
                                masterData =
                                    MasterDataUseCaseBundle(
                                        getLines = container.getLinesUseCase,
                                        getShifts = container.getShiftsUseCase,
                                        getParts = container.getPartsUseCase,
                                        getDefectTypes = container.getDefectTypesUseCase,
                                    ),
                            ),
                        onRecordsSaved = { refreshData() },
                    )

                AppRoute.Abnormal -> AbnormalScreen()
                AppRoute.Reports ->
                    ReportsScreen(
                        lines = lines,
                        dailySummaries = dailySummaries,
                        loadDailyDetail = loadDailyDetail,
                        loadManualHolidays = { container.getManualHolidayDatesUseCase.execute() },
                        saveManualHolidays = { container.saveManualHolidayDatesUseCase.execute(it) },
                    )
                AppRoute.ReportsMonthly ->
                    ReportsMonthlyScreen(
                        lines = lines,
                        loadMonthlyReportDocument = loadMonthlyReportDocument,
                        loadManualHolidays = { container.getManualHolidayDatesUseCase.execute() },
                    )
                AppRoute.Settings ->
                    SettingsScreen(
                        dependencies =
                            SettingsScreenDependencies(
                                getAllowDuplicateInspection = container.getAllowDuplicateInspectionUseCase,
                                setAllowDuplicateInspection = container.setAllowDuplicateInspectionUseCase,
                                resetData = container.resetDataUseCase,
                                getLines = container.getLinesUseCase,
                                getDevQcLine = container.getDevQcLineUseCase,
                                setDevQcLine = container.setDevQcLineUseCase,
                                getDevDemoMode = container.getDevDemoModeUseCase,
                                setDevDemoMode = container.setDevDemoModeUseCase,
                                getDevDummyData = container.getDevDummyDataUseCase,
                                setDevDummyData = container.setDevDummyDataUseCase,
                                backupDatabase = container.backupDatabaseUseCase,
                                restoreDatabase = container.restoreDatabaseUseCase,
                                onResetCompleted = refreshData,
                                onDummyDataChanged = { enabled ->
                                    useDummyData = enabled
                                    refreshData()
                                },
                                onDemoModeChanged = { enabled -> demoMode = enabled },
                                onRestoreCompleted = {
                                    container.close()
                                    container = AppContainer()
                                },
                            ),
                    )
            }
        }
    }
}
