package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.AttachmentMetadata
import id.co.nierstyd.mutugemba.domain.AttachmentRepository
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class AttachmentStore(
    private val rootDir: Path,
) : AttachmentRepository {
    private val idCounter = AtomicLong(0)
    private val items: MutableList<AttachmentMetadata> = mutableListOf()

    override fun insertMetadata(
        inspectionId: Long,
        filename: String,
        storedPath: String,
        createdAt: String,
    ): AttachmentMetadata {
        Files.createDirectories(rootDir)
        val resolvedPath = rootDir.resolve(storedPath).normalize()
        Files.createDirectories(resolvedPath.parent ?: rootDir)
        val metadata =
            AttachmentMetadata(
                id = idCounter.incrementAndGet(),
                inspectionId = inspectionId,
                filename = filename,
                storedPath = resolvedPath.toString(),
                createdAt = createdAt.ifBlank { LocalDateTime.now().toString() },
            )
        items += metadata
        return metadata
    }

    override fun listByInspection(inspectionId: Long): List<AttachmentMetadata> =
        items.filter { it.inspectionId == inspectionId }

    fun ensureBaseDir() {
        Files.createDirectories(rootDir)
    }
}
