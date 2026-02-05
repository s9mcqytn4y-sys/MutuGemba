package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.SettingsRepository
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class FileSettingsRepository(
    private val filePath: Path,
) : SettingsRepository {
    private val cache: MutableMap<String, String> = mutableMapOf()
    private var loaded = false

    override fun getString(key: String): String? {
        ensureLoaded()
        return cache[key]
    }

    override fun putString(
        key: String,
        value: String,
    ) {
        ensureLoaded()
        cache[key] = value
        persist()
    }

    private fun ensureLoaded() {
        if (loaded) {
            return
        }

        if (Files.exists(filePath)) {
            val props = Properties()
            Files.newInputStream(filePath).use { props.load(it) }
            props.forEach { entry ->
                val name = entry.key?.toString() ?: return@forEach
                val value = entry.value?.toString() ?: return@forEach
                cache[name] = value
            }
        }

        loaded = true
    }

    private fun persist() {
        Files.createDirectories(filePath.parent)
        val props = Properties()
        cache.forEach { (key, value) -> props.setProperty(key, value) }
        Files.newOutputStream(filePath).use { props.store(it, "MutuGemba settings") }
    }
}
