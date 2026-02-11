package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.BackupRepository
import id.co.nierstyd.mutugemba.domain.BackupSnapshot
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalBackupRepository(
    private val baseDir: Path,
    private val databaseFile: Path,
    private val settingsFile: Path,
    private val attachmentsDir: Path,
    private val assetsStoreDir: Path,
) : BackupRepository {
    override fun createBackup(): Result<BackupSnapshot> =
        runCatching {
            val id = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val backupRoot = baseDir.resolve("backup").resolve(id)
            Files.createDirectories(backupRoot)

            copyIfExists(databaseFile, backupRoot.resolve(databaseFile.fileName.toString()))
            copyIfExists(settingsFile, backupRoot.resolve(settingsFile.fileName.toString()))
            copyDirectoryIfExists(attachmentsDir, backupRoot.resolve("attachments"))
            copyDirectoryIfExists(assetsStoreDir, backupRoot.resolve("assets_store"))

            BackupSnapshot(
                id = id,
                createdAt = LocalDateTime.now(),
                location = backupRoot.toAbsolutePath().toString(),
            )
        }

    override fun restoreLatest(): Result<BackupSnapshot> =
        runCatching {
            val backupBase = baseDir.resolve("backup")
            val latest =
                Files.list(backupBase).use { stream ->
                    stream.filter { Files.isDirectory(it) }.max(compareBy { it.fileName.toString() }).orElseThrow {
                        IllegalStateException("Belum ada backup yang bisa di-restore.")
                    }
                }

            copyIfExists(latest.resolve(databaseFile.fileName.toString()), databaseFile)
            copyIfExists(latest.resolve(settingsFile.fileName.toString()), settingsFile)
            copyDirectoryIfExists(latest.resolve("attachments"), attachmentsDir)
            copyDirectoryIfExists(latest.resolve("assets_store"), assetsStoreDir)

            BackupSnapshot(
                id = latest.fileName.toString(),
                createdAt = LocalDateTime.now(),
                location = latest.toAbsolutePath().toString(),
            )
        }

    private fun copyIfExists(
        source: Path,
        target: Path,
    ) {
        if (!Files.exists(source)) return
        Files.createDirectories(target.parent)
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun copyDirectoryIfExists(
        sourceDir: Path,
        targetDir: Path,
    ) {
        if (!Files.exists(sourceDir)) return
        Files.createDirectories(targetDir)
        Files.walk(sourceDir).use { stream ->
            stream.forEach { source ->
                val relative = sourceDir.relativize(source)
                val target = targetDir.resolve(relative.toString())
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
