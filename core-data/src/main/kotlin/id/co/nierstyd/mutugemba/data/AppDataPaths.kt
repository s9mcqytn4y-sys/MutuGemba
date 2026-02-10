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

    fun defaultPartAssetsExtractedDir(): Path = projectPartAssetsDir().resolve("extracted")

    fun defaultPartAssetsZip(): Path =
        projectPartAssetsDir().resolve("Part_Assets_Export.zip").let { projectZip ->
            if (Files.exists(projectZip)) {
                projectZip
            } else {
                Paths
                    .get(System.getProperty("user.home", "."), "Downloads", "Part_Assets_Export.zip")
                    .toAbsolutePath()
                    .normalize()
            }
        }
}
