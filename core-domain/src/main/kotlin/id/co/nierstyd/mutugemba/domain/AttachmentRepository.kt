package id.co.nierstyd.mutugemba.domain

data class AttachmentMetadata(
    val id: Long,
    val inspectionId: Long,
    val filename: String,
    val storedPath: String,
    val createdAt: String,
)

interface AttachmentRepository {
    fun insertMetadata(
        inspectionId: Long,
        filename: String,
        storedPath: String,
        createdAt: String,
    ): AttachmentMetadata

    fun listByInspection(inspectionId: Long): List<AttachmentMetadata>
}
