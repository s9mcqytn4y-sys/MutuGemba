package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord

object SampleData {
    val inspectionRecords: List<InspectionRecord> =
        listOf(
            InspectionRecord(
                id = 1,
                kind = InspectionKind.DEFECT,
                lineName = "Press",
                shiftName = "Shift 1 (08:00-17:00 WIB)",
                partName = "Housing Assy",
                partNumber = "PN-1001",
                createdAt = "2026-02-05T08:00:00",
            ),
        )
}
