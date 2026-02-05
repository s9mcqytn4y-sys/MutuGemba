package id.co.nierstyd.mutugemba.domain

interface InspectionRepository {
    fun insert(input: InspectionInput): InspectionRecord

    fun getRecent(limit: Long): List<InspectionRecord>
}
