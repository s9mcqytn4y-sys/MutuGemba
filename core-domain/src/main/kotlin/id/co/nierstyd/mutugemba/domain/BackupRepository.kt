package id.co.nierstyd.mutugemba.domain

import java.time.LocalDateTime

data class BackupSnapshot(
    val id: String,
    val createdAt: LocalDateTime,
    val location: String,
)

interface BackupRepository {
    fun createBackup(): Result<BackupSnapshot>

    fun restoreLatest(): Result<BackupSnapshot>
}
