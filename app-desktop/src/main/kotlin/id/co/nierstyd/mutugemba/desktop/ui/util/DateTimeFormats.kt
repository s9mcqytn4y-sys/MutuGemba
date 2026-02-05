package id.co.nierstyd.mutugemba.desktop.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {
    private val locale = Locale("id", "ID")
    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", locale)

    fun formatTimestamp(rawIso: String): String =
        runCatching {
            LocalDateTime.parse(rawIso).format(displayFormatter)
        }.getOrElse { rawIso }

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)
}
