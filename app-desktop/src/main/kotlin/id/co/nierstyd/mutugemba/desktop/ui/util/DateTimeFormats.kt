package id.co.nierstyd.mutugemba.desktop.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {
    private val locale = Locale("id", "ID")
    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val fallbackFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", locale)

    fun formatTimestamp(rawIso: String): String =
        runCatching {
            LocalDateTime.parse(rawIso).format(displayFormatter)
        }.getOrElse {
            runCatching { LocalDateTime.parse(rawIso, fallbackFormatter).format(displayFormatter) }
                .getOrElse { rawIso }
        }

    fun formatTimestampWithZone(rawIso: String): String = "${formatTimestamp(rawIso)} WIB"

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatMonth(month: YearMonth): String = month.atDay(1).format(monthFormatter)

    fun parseLocalDate(rawIso: String): LocalDate? =
        runCatching { LocalDateTime.parse(rawIso).toLocalDate() }
            .getOrElse { runCatching { LocalDateTime.parse(rawIso, fallbackFormatter).toLocalDate() }.getOrNull() }

    fun parseLocalDateTime(rawIso: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(rawIso) }
            .getOrElse { runCatching { LocalDateTime.parse(rawIso, fallbackFormatter) }.getOrNull() }

    fun formatTime(rawIso: String): String? = parseLocalDateTime(rawIso)?.format(timeFormatter)
}
