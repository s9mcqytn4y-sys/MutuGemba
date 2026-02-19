package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppSettingsKeys
import id.co.nierstyd.mutugemba.domain.SettingsRepository

data class ReportArchiveEntry(
    val id: String,
    val reportType: String,
    val action: String,
    val line: String,
    val period: String,
    val filePath: String,
    val createdAt: String,
)

class GetReportArchiveEntriesUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(limit: Int = 30): List<ReportArchiveEntry> {
        val raw = settingsRepository.getString(AppSettingsKeys.REPORT_ARCHIVE_ENTRIES).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw
            .lineSequence()
            .mapNotNull(::parseEntry)
            .sortedByDescending { it.createdAt }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    private fun parseEntry(line: String): ReportArchiveEntry? {
        val parts = line.split("\t")
        if (parts.size != 7) return null
        return ReportArchiveEntry(
            id = parts[0],
            reportType = parts[1],
            action = parts[2],
            line = parts[3],
            period = parts[4],
            filePath = parts[5],
            createdAt = parts[6],
        )
    }
}

class AppendReportArchiveEntryUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(
        reportType: String,
        action: String,
        line: String,
        period: String,
        filePath: String,
        createdAt: String,
    ) {
        val id = "$reportType-$action-$createdAt"
        val encoded = listOf(id, reportType, action, line, period, filePath, createdAt).joinToString("\t")
        val existing = settingsRepository.getString(AppSettingsKeys.REPORT_ARCHIVE_ENTRIES).orEmpty()
        val merged =
            buildList {
                add(encoded)
                existing
                    .lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("$id\t") }
                    .take(99)
                    .forEach { add(it) }
            }
        settingsRepository.putString(
            AppSettingsKeys.REPORT_ARCHIVE_ENTRIES,
            merged.joinToString("\n"),
        )
    }
}
