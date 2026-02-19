@file:Suppress("LongParameterList")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.runtime.Composable
import id.co.nierstyd.mutugemba.domain.model.DefectMaster
import id.co.nierstyd.mutugemba.domain.model.MaterialMaster
import id.co.nierstyd.mutugemba.domain.model.NgOriginType
import id.co.nierstyd.mutugemba.domain.model.PartMasterDetail
import id.co.nierstyd.mutugemba.domain.model.PartMasterListItem
import id.co.nierstyd.mutugemba.domain.model.SupplierMaster

@Composable
internal fun PartMasterAdminScreen(
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
    loadPartDetail: suspend (partId: Long) -> PartMasterDetail?,
) {
    PartMasterManagerPanel(
        tabIndex = tabIndex,
        onTabSelected = onTabSelected,
        parts = parts,
        materials = materials,
        suppliers = suppliers,
        defects = defects,
        infoText = infoText,
        onSavePart = onSavePart,
        onSaveSupplier = onSaveSupplier,
        onDeleteSupplier = onDeleteSupplier,
        onSaveMaterial = onSaveMaterial,
        onDeleteMaterial = onDeleteMaterial,
        onSaveDefect = onSaveDefect,
        onDeleteDefect = onDeleteDefect,
        onAssignPartMaterials = onAssignPartMaterials,
        onAssignPartDefects = onAssignPartDefects,
        loadPartDetail = loadPartDetail,
    )
}
