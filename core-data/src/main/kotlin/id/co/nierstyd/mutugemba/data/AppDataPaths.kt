package id.co.nierstyd.mutugemba.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object AppDataPaths {
    private const val APP_DIR_NAME = ".mutugemba"

    private fun dataRoot(): Path {
        val userHome = System.getProperty("user.home").orEmpty()
        val base = if (userHome.isBlank()) Paths.get("data") else Paths.get(userHome, APP_DIR_NAME, "data")
        return base.toAbsolutePath().normalize()
    }

    fun dataDir(): Path = dataRoot()

    fun databaseFile(): Path = dataRoot().resolve("mutugemba.db")

    fun attachmentsDir(): Path = dataRoot().resolve("attachments")

    fun settingsFile(): Path = dataRoot().resolve("settings.properties")

    fun exportsDir(): Path = dataRoot().resolve("exports")

    fun assetsStoreDir(): Path = dataRoot().resolve("assets_store")

    fun importLogsDir(): Path = dataRoot().resolve("import_logs")

    fun projectPartAssetsDir(): Path =
        Paths
            .get(System.getProperty("user.dir", "."), "data", "part_assets")
            .toAbsolutePath()
            .normalize()

    fun defaultPartAssetsExtractedDir(): Path {
        val cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
        val candidates = mutableListOf<Path>()
        var cursor: Path? = cwd
        repeat(8) {
            val base = cursor ?: return@repeat
            candidates.add(
                base
                    .resolve("data")
                    .resolve("part_assets")
                    .resolve("extracted")
                    .normalize(),
            )
            cursor = base.parent
        }
        return candidates.firstOrNull { candidate ->
            Files.exists(candidate.resolve("mappings").resolve("mapping.json"))
        } ?: cwd
            .resolve("data")
            .resolve("part_assets")
            .resolve("extracted")
            .normalize()
    }
}
