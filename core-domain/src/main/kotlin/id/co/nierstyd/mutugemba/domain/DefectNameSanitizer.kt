package id.co.nierstyd.mutugemba.domain

object DefectNameSanitizer {
    private val leadingCodePattern = Regex("^\\([^)]+\\)\\s*")
    private val trailingVariantPattern = Regex("\\s+[A-Z]$")
    private val splitPattern = Regex("[,;/\\n]+")

    fun normalizeDisplay(raw: String): String {
        val cleaned =
            raw
                .trim()
                .replace(leadingCodePattern, "")
                .replace("\\s+".toRegex(), " ")
                .uppercase()
        if (cleaned.isBlank()) return ""
        return cleaned.replace(trailingVariantPattern, "").trim()
    }

    fun expandProblemItems(raw: String): List<String> =
        splitPattern
            .split(raw)
            .map(::normalizeDisplay)
            .filter { it.isNotBlank() }
            .distinct()
}
