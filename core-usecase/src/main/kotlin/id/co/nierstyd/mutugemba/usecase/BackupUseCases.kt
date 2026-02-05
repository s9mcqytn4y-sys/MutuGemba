package id.co.nierstyd.mutugemba.usecase

class BackupDatabaseUseCase {
    fun execute(): Result<Unit> = Result.failure(UnsupportedOperationException("Backup belum tersedia"))
}

class RestoreDatabaseUseCase {
    fun execute(): Result<Unit> = Result.failure(UnsupportedOperationException("Restore belum tersedia"))
}
