package id.co.nierstyd.mutugemba.domain

object DefectNameSanitizer {
    private val leadingCodePattern = Regex("^\\([^)]+\\)\\s*")
    private val trailingVariantPattern = Regex("\\s+[A-Z]$")
    private val splitPattern = Regex("[,;/\\n]+")
    private val compactWhitespacePattern = Regex("\\s+")
    private val nonWordPattern = Regex("[^A-Z0-9/ ]")
    private val aliasReplacements =
        linkedMapOf(
            "SPOUNDBOUND" to "SPUNBOND",
            "SPUNBOUND" to "SPUNBOND",
            "TDK" to "TIDAK",
            "DIMENTION" to "DIMENSION",
            "DEFFECT" to "DEFECT",
        )

    fun normalizeDisplay(raw: String): String {
        val cleaned =
            raw
                .trim()
                .replace(leadingCodePattern, "")
                .replace(compactWhitespacePattern, " ")
                .uppercase()
        if (cleaned.isBlank()) return ""
        return cleaned.replace(trailingVariantPattern, "").trim()
    }

    fun canonicalKey(raw: String): String {
        val normalized = normalizeDisplay(raw)
        if (normalized.isBlank()) return ""
        val aliased =
            aliasReplacements.entries.fold(normalized) { acc, (from, to) ->
                acc.replace("\\b$from\\b".toRegex(), to)
            }
        return aliased
            .replace(nonWordPattern, " ")
            .replace(compactWhitespacePattern, " ")
            .trim()
    }

    fun expandProblemItems(raw: String): List<String> =
        splitPattern
            .split(raw)
            .map(::canonicalKey)
            .filter { it.isNotBlank() }
            .distinct()
}
