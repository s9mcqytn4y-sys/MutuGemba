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
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.InspectionScreenDependencies
import id.co.nierstyd.mutugemba.desktop.ui.screens.MasterDataUseCaseBundle
import id.co.nierstyd.mutugemba.desktop.ui.screens.ReportsScreen
import id.co.nierstyd.mutugemba.desktop.ui.screens.SettingsScreen
import id.co.nierstyd.mutugemba.desktop.ui.theme.MutuGembaTheme
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetCtqParametersUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetLastVisitedPageUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetRecentInspectionsUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SetLastVisitedPageUseCase

@Composable
fun MutuGembaApp() {
    val settingsRepository = remember { FileSettingsRepository(AppDataPaths.settingsFile()) }
    val getLastPageUseCase = remember { GetLastVisitedPageUseCase(settingsRepository) }
    val setLastPageUseCase = remember { SetLastVisitedPageUseCase(settingsRepository) }
    val getInspectionDefaultsUseCase = remember { GetInspectionDefaultsUseCase(settingsRepository) }
    val saveInspectionDefaultsUseCase = remember { SaveInspectionDefaultsUseCase(settingsRepository) }

    val database = remember { DatabaseFactory.createDatabase(AppDataPaths.databaseFile()) }
    val inspectionRepository = remember { SqlDelightInspectionRepository(database) }
    val masterDataRepository = remember { SqlDelightMasterDataRepository(database) }
    val createInspectionUseCase = remember { CreateInspectionRecordUseCase(inspectionRepository) }
    val getRecentInspectionsUseCase = remember { GetRecentInspectionsUseCase(inspectionRepository) }
    val getLinesUseCase = remember { GetLinesUseCase(masterDataRepository) }
    val getShiftsUseCase = remember { GetShiftsUseCase(masterDataRepository) }
    val getPartsUseCase = remember { GetPartsUseCase(masterDataRepository) }
    val getDefectTypesUseCase = remember { GetDefectTypesUseCase(masterDataRepository) }
    val getCtqParametersUseCase = remember { GetCtqParametersUseCase(masterDataRepository) }
    val inspectionDependencies =
        remember {
            InspectionScreenDependencies(
                defaults =
                    InspectionDefaultsUseCases(
                        getDefaults = getInspectionDefaultsUseCase,
                        saveDefaults = saveInspectionDefaultsUseCase,
                    ),
                createInspectionUseCase = createInspectionUseCase,
                masterData =
                    MasterDataUseCaseBundle(
                        getLines = getLinesUseCase,
                        getShifts = getShiftsUseCase,
                        getParts = getPartsUseCase,
                        getDefectTypes = getDefectTypesUseCase,
                        getCtqParameters = getCtqParametersUseCase,
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

    LaunchedEffect(Unit) {
        attachmentStore.ensureBaseDir()
        inspectionRecords = getRecentInspectionsUseCase.execute()
    }

    LaunchedEffect(currentRoute) {
        setLastPageUseCase.execute(currentRoute.key)
    }

    MutuGembaTheme {
        AppLayout(
            routes = AppRoute.values().toList(),
            currentRoute = currentRoute,
            onRouteSelected = { currentRoute = it },
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
                        onNavigateToInspection = { currentRoute = AppRoute.Inspection },
                    )

                AppRoute.Inspection ->
                    InspectionScreen(
                        dependencies = inspectionDependencies,
                        onRecordSaved = {
                            inspectionRecords = getRecentInspectionsUseCase.execute()
                        },
                    )

                AppRoute.Abnormal -> AbnormalScreen()
                AppRoute.Reports -> ReportsScreen()
                AppRoute.Settings -> SettingsScreen()
            }
        }
    }
}
