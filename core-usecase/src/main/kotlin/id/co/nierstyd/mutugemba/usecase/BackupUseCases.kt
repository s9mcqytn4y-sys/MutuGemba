package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.BackupRepository
import id.co.nierstyd.mutugemba.domain.BackupSnapshot

class BackupDatabaseUseCase(
    private val repository: BackupRepository,
) {
    fun execute(): Result<BackupSnapshot> = repository.createBackup()
}

class RestoreDatabaseUseCase(
    private val repository: BackupRepository,
) {
    fun execute(): Result<BackupSnapshot> = repository.restoreLatest()
}
