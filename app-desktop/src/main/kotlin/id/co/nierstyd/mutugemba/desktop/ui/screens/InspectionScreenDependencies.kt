package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.usecase.CheckLineAlreadyInputUseCase
import id.co.nierstyd.mutugemba.usecase.CreateBatchInspectionRecordsUseCase
import id.co.nierstyd.mutugemba.usecase.CreateInspectionRecordUseCase
import id.co.nierstyd.mutugemba.usecase.GetAllowDuplicateInspectionUseCase
import id.co.nierstyd.mutugemba.usecase.GetDefectTypesUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.GetInspectionPartDefectLayoutUseCase
import id.co.nierstyd.mutugemba.usecase.GetLinesUseCase
import id.co.nierstyd.mutugemba.usecase.GetPartsUseCase
import id.co.nierstyd.mutugemba.usecase.GetShiftsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionDefaultsUseCase
import id.co.nierstyd.mutugemba.usecase.SaveInspectionPartDefectLayoutUseCase
import id.co.nierstyd.mutugemba.usecase.UpsertDefectTypeUseCase

data class InspectionDefaultsUseCases(
    val getDefaults: GetInspectionDefaultsUseCase,
    val saveDefaults: SaveInspectionDefaultsUseCase,
)

data class InspectionDefectLayoutUseCases(
    val getLayout: GetInspectionPartDefectLayoutUseCase,
    val saveLayout: SaveInspectionPartDefectLayoutUseCase,
)

data class InspectionPolicyUseCases(
    val getAllowDuplicate: GetAllowDuplicateInspectionUseCase,
    val checkLineAlreadyInput: CheckLineAlreadyInputUseCase,
)

data class MasterDataUseCaseBundle(
    val getLines: GetLinesUseCase,
    val getShifts: GetShiftsUseCase,
    val getParts: GetPartsUseCase,
    val getDefectTypes: GetDefectTypesUseCase,
    val upsertDefectType: UpsertDefectTypeUseCase,
)

data class InspectionScreenDependencies(
    val defaults: InspectionDefaultsUseCases,
    val defectLayout: InspectionDefectLayoutUseCases,
    val createInspectionUseCase: CreateInspectionRecordUseCase,
    val createBatchInspectionUseCase: CreateBatchInspectionRecordsUseCase,
    val policies: InspectionPolicyUseCases,
    val masterData: MasterDataUseCaseBundle,
)
