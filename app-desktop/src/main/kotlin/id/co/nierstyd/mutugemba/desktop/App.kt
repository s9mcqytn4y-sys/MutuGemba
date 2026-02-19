package id.co.nierstyd.mutugemba.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.di.AppContainer
import id.co.nierstyd.mutugemba.desktop.navigation.AppRoute
import id.co.nierstyd.mutugemba.desktop.ui.components.FooterBar
import id.co.nierstyd.mutugemba.desktop.ui.components.HeaderBar
import id.co.nierstyd.mutugemba.desktop.ui.layout.AppLayout
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.resources.classpathPainterResource
import id.co.nierstyd.mutugemba.desktop.ui.screens.AbnormalScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.HomeScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionDefaultsUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionDefectLayoutUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionPolicyUseCases
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.MasterDataUseCaseBundle
import id.co.nierstyd.mutugemba.desktop.ui.screens.PartMappingScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.PartMappingScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.PartMappingViewMode
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsMonthlyScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.theme.MutuGembaTheme
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun MutuGembaApp() {
    var container by remember { mutableStateOf(AppContainer()) }
    var bootLoading by remember { mutableStateOf(true) }

    var currentRoute by remember(container) {
        mutableStateOf(
            AppRoute.fromKey(container.getLastPageUseCase.execute()) ?: AppRoute.Home,
        )
    }
    var inspectionRecords by remember(container) { mutableStateOf<List<InspectionRecord>>(emptyList()) }
    var lines by remember(container) { mutableStateOf<List<Line>>(emptyList()) }
    var dailySummaries by remember(container) { mutableStateOf<List<DailyChecksheetSummary>>(emptyList()) }

    suspend fun loadData() {
        val (records, lineItems, summaries) =
            withContext(Dispatchers.IO) {
                Triple(
                    container.getRecentInspectionsUseCase.execute(),
                    container.getLinesUseCase.execute(),
                    container.getDailySummariesUseCase.execute(),
                )
            }
        inspectionRecords = records
        lines = lineItems
        dailySummaries = summaries
        bootLoading = false
    }

    var refreshTick by remember(container) { mutableStateOf(0) }
    val refreshData: () -> Unit = { refreshTick += 1 }

    val loadDailyDetail: (Long, LocalDate) -> DailyChecksheetDetail? = { lineId, date ->
        container.getDailyDetailUseCase.execute(lineId, date)
    }
    val loadMonthlyDefectSummary: (YearMonth) -> List<DefectSummary> = { month ->
        container.getMonthlyDefectSummaryUseCase.execute(month)
    }
    val loadMonthlyEntries: (Long, YearMonth) -> List<ChecksheetEntry> = { lineId, month ->
        container.getMonthlyEntriesUseCase.execute(lineId, month)
    }
    val loadMonthlyReportDocument: (Long, YearMonth) -> MonthlyReportDocument? = { lineId, month ->
        container.getMonthlyReportDocumentUseCase.execute(lineId, month)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            container.attachmentStore.ensureBaseDir()
        }
    }

    LaunchedEffect(container, refreshTick) {
        loadData()
    }

    LaunchedEffect(currentRoute) {
        container.setLastPageUseCase.execute(currentRoute.key)
    }

    MutuGembaTheme {
        if (bootLoading) {
            AppWelcomeLoading()
            return@MutuGembaTheme
        }
        AppLayout(
            routes = AppRoute.values().toList(),
            currentRoute = currentRoute,
            onRouteSelected = { currentRoute = it },
            scrollableContent = false,
            headerContent = {
                HeaderBar(
                    title = AppStrings.App.Name,
                    subtitle = AppStrings.App.DepartmentName,
                    demoMode = false,
                    dummyData = false,
                )
            },
            footerContent = {
                FooterBar(
                    statusText = AppStrings.App.OfflineStatus,
                    hintText = AppStrings.App.footerPage(currentRoute.label),
                )
            },
        ) {
            Crossfade(targetState = currentRoute, animationSpec = tween(durationMillis = 220)) { route ->
                when (route) {
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

                    AppRoute.PartCatalog ->
                        PartMappingScreen(
                            dependencies =
                                PartMappingScreenDependencies(
                                    observeParts = container.observePartsUseCase,
                                    getPartDetail = container.getPartDetailUseCase,
                                    listPartMasters = container.listPartMastersUseCase,
                                    getPartMasterDetail = container.getPartMasterDetailUseCase,
                                    savePartMaster = container.savePartMasterUseCase,
                                    replacePartMaterials = container.replacePartMaterialsUseCase,
                                    replacePartDefects = container.replacePartDefectsUseCase,
                                    listMaterialMasters = container.listMaterialMastersUseCase,
                                    saveMaterialMaster = container.saveMaterialMasterUseCase,
                                    deleteMaterialMaster = container.deleteMaterialMasterUseCase,
                                    listSupplierMasters = container.listSupplierMastersUseCase,
                                    saveSupplierMaster = container.saveSupplierMasterUseCase,
                                    deleteSupplierMaster = container.deleteSupplierMasterUseCase,
                                    listDefectMasters = container.listDefectMastersUseCase,
                                    saveDefectMaster = container.saveDefectMasterUseCase,
                                    deleteDefectMaster = container.deleteDefectMasterUseCase,
                                    getTopDefects = container.getTopDefectsPerModelMonthlyUseCase,
                                    getDefectHeatmap = container.getDefectHeatmapUseCase,
                                    getActiveImageRef = container.getActiveImageRefUseCase,
                                    loadImageBytes = container.loadImageBytesUseCase,
                                ),
                            viewMode = PartMappingViewMode.CATALOG_ONLY,
                        )

                    AppRoute.PartMaster ->
                        PartMappingScreen(
                            dependencies =
                                PartMappingScreenDependencies(
                                    observeParts = container.observePartsUseCase,
                                    getPartDetail = container.getPartDetailUseCase,
                                    listPartMasters = container.listPartMastersUseCase,
                                    getPartMasterDetail = container.getPartMasterDetailUseCase,
                                    savePartMaster = container.savePartMasterUseCase,
                                    replacePartMaterials = container.replacePartMaterialsUseCase,
                                    replacePartDefects = container.replacePartDefectsUseCase,
                                    listMaterialMasters = container.listMaterialMastersUseCase,
                                    saveMaterialMaster = container.saveMaterialMasterUseCase,
                                    deleteMaterialMaster = container.deleteMaterialMasterUseCase,
                                    listSupplierMasters = container.listSupplierMastersUseCase,
                                    saveSupplierMaster = container.saveSupplierMasterUseCase,
                                    deleteSupplierMaster = container.deleteSupplierMasterUseCase,
                                    listDefectMasters = container.listDefectMastersUseCase,
                                    saveDefectMaster = container.saveDefectMasterUseCase,
                                    deleteDefectMaster = container.deleteDefectMasterUseCase,
                                    getTopDefects = container.getTopDefectsPerModelMonthlyUseCase,
                                    getDefectHeatmap = container.getDefectHeatmapUseCase,
                                    getActiveImageRef = container.getActiveImageRefUseCase,
                                    loadImageBytes = container.loadImageBytesUseCase,
                                ),
                            viewMode = PartMappingViewMode.MASTER_ONLY,
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
                                    defectLayout =
                                        InspectionDefectLayoutUseCases(
                                            getLayout = container.getInspectionPartDefectLayoutUseCase,
                                            saveLayout = container.saveInspectionPartDefectLayoutUseCase,
                                        ),
                                    createInspectionUseCase = container.createInspectionUseCase,
                                    createBatchInspectionUseCase = container.createBatchInspectionUseCase,
                                    policies =
                                        InspectionPolicyUseCases(
                                            getAllowDuplicate = container.getAllowDuplicateInspectionUseCase,
                                            checkLineAlreadyInput = container.checkLineAlreadyInputUseCase,
                                        ),
                                    masterData =
                                        MasterDataUseCaseBundle(
                                            getLines = container.getLinesUseCase,
                                            getShifts = container.getShiftsUseCase,
                                            getParts = container.getPartsUseCase,
                                            getDefectTypes = container.getDefectTypesUseCase,
                                            upsertDefectType = container.upsertDefectTypeUseCase,
                                        ),
                                ),
                            onRecordsSaved = { refreshData() },
                        )

                    AppRoute.Abnormal ->
                        ScrollableRouteContainer {
                            AbnormalScreen()
                        }

                    AppRoute.Reports ->
                        ScrollableRouteContainer {
                            ReportsScreen(
                                lines = lines,
                                dailySummaries = dailySummaries,
                                loadDailyDetail = loadDailyDetail,
                                loadManualHolidays = { container.getManualHolidayDatesUseCase.execute() },
                                saveManualHolidays = { container.saveManualHolidayDatesUseCase.execute(it) },
                            )
                        }

                    AppRoute.ReportsMonthly ->
                        ScrollableRouteContainer {
                            ReportsMonthlyScreen(
                                lines = lines,
                                dailySummaries = dailySummaries,
                                loadMonthlyReportDocument = loadMonthlyReportDocument,
                                loadManualHolidays = { container.getManualHolidayDatesUseCase.execute() },
                                recentArchiveEntries = { container.getReportArchiveEntriesUseCase.execute() },
                                appendArchiveEntry = { action, line, period, path, createdAt ->
                                    container.appendReportArchiveEntryUseCase.execute(
                                        reportType = "MONTHLY",
                                        action = action,
                                        line = line,
                                        period = period,
                                        filePath = path,
                                        createdAt = createdAt,
                                    )
                                },
                            )
                        }

                    AppRoute.Settings ->
                        ScrollableRouteContainer {
                            SettingsScreen(
                                dependencies =
                                    SettingsScreenDependencies(
                                        getAllowDuplicateInspection = container.getAllowDuplicateInspectionUseCase,
                                        setAllowDuplicateInspection = container.setAllowDuplicateInspectionUseCase,
                                        resetData = container.resetDataUseCase,
                                        getLines = container.getLinesUseCase,
                                        getDevQcLine = container.getDevQcLineUseCase,
                                        setDevQcLine = container.setDevQcLineUseCase,
                                        backupDatabase = container.backupDatabaseUseCase,
                                        restoreDatabase = container.restoreDatabaseUseCase,
                                        runLoadSimulation = { container.generateHighVolumeSimulation() },
                                        clearCaches = { container.clearAppCaches() },
                                        onResetCompleted = refreshData,
                                        onRestoreCompleted = {
                                            bootLoading = true
                                            container.close()
                                            container = AppContainer()
                                        },
                                    ),
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun ScrollableRouteContainer(content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportMaxHeight = if (maxHeight != Dp.Infinity) maxHeight else 1000.dp
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = viewportMaxHeight)
                    .verticalScroll(scrollState),
        ) {
            content()
        }
    }
}

@Composable
private fun AppWelcomeLoading() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NeutralSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                painter = classpathPainterResource("branding/pt_prima_logo.png"),
                contentDescription = "PT Primaraya Logo",
                modifier = Modifier.height(52.dp),
                contentScale = ContentScale.Fit,
            )
            Text(text = AppStrings.App.Name, style = MaterialTheme.typography.h3)
            Text(
                text = AppStrings.App.IdentityTagline,
                style = MaterialTheme.typography.body1,
                color = NeutralTextMuted,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
            )
            Surface(
                color = NeutralLight,
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                elevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(text = "Menyiapkan data aplikasi...", style = MaterialTheme.typography.body2)
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.28f)),
                    )
                }
            }
        }
    }
}
