package id.co.nierstyd.mutugemba.data

import java.nio.file.Path
import java.nio.file.Paths

object AppDataPaths {
    private fun dataRoot(): Path = Paths.get("data")

    fun databaseFile(): Path = dataRoot().resolve("mutugemba.db")

    fun attachmentsDir(): Path = dataRoot().resolve("attachments")

    fun settingsFile(): Path = dataRoot().resolve("settings.properties")
}
