package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.InspectionRecord

object SampleData {
    val inspectionRecords: List<InspectionRecord> =
        listOf(
            InspectionRecord(
                id = "IR-001",
                type = "Defect",
            ),
            InspectionRecord(
                id = "IR-002",
                type = "CTQ",
            ),
        )
}
