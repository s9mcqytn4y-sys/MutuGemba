package id.co.nierstyd.mutugemba.data

import java.nio.file.Path
import java.nio.file.Paths

object AppDataPaths {
    fun settingsFile(): Path =
        Paths.get(
            System.getProperty("user.home"),
            ".mutugemba",
            "settings.properties",
        )
}
