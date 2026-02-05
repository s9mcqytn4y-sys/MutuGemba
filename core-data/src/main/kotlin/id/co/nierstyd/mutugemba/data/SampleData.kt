package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.InspectionRecord

object SampleData {
    val inspectionRecords: List<InspectionRecord> =
        listOf(
            InspectionRecord(
                id = 1,
                type = "Cacat",
                lineName = "Line A",
                createdAt = "2026-02-05T08:00:00",
            ),
            InspectionRecord(
                id = 2,
                type = "Parameter Proses",
                lineName = "Line A",
                createdAt = "2026-02-05T09:00:00",
            ),
        )
}
