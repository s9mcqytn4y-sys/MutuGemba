package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.usecase.CreateBatchInspectionRecordsUseCase
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetCtqParametersUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase

data class InspectionDefaultsUseCases(
    val getDefaults: GetInspectionDefaultsUseCase,
    val saveDefaults: SaveInspectionDefaultsUseCase,
)

data class MasterDataUseCaseBundle(
    val getLines: GetLinesUseCase,
    val getShifts: GetShiftsUseCase,
    val getParts: GetPartsUseCase,
    val getDefectTypes: GetDefectTypesUseCase,
    val getCtqParameters: GetCtqParametersUseCase,
)

data class InspectionScreenDependencies(
    val defaults: InspectionDefaultsUseCases,
    val createInspectionUseCase: CreateInspectionRecordUseCase,
    val createBatchInspectionUseCase: CreateBatchInspectionRecordsUseCase,
    val masterData: MasterDataUseCaseBundle,
)
