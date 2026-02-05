package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord

object SampleData {
    val inspectionRecords: List<InspectionRecord> =
        listOf(
            InspectionRecord(
                id = 1,
                kind = InspectionKind.DEFECT,
                lineName = "Line A",
                shiftName = "Shift 1",
                partName = "Housing Assy",
                partNumber = "PN-1001",
                createdAt = "2026-02-05T08:00:00",
            ),
            InspectionRecord(
                id = 2,
                kind = InspectionKind.CTQ,
                lineName = "Line A",
                shiftName = "Shift 2",
                partName = "Bracket Support",
                partNumber = "PN-2002",
                createdAt = "2026-02-05T09:00:00",
            ),
        )
}
