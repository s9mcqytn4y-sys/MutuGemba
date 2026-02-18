package id.co.nierstyd.mutugemba.domain

object DefectNameSanitizer {
    private val leadingCodePattern = Regex("^\\([^)]+\\)\\s*")
    private val trailingVariantPattern = Regex("\\s+[A-Z]$")
    private val splitPattern = Regex("[,;/\\n]+")
    private val nonAlphaNumericPattern = Regex("[^A-Z0-9 ]")
    private val ignoredTokens =
        setOf(
            "-",
            "--",
            ".",
            "A",
            "TOTAL",
            "TOTAL NG",
            "GRAND TOTAL",
            "SUB TOTAL",
            "SUBTOTAL",
            "JUMLAH",
            "JUMLAH NG",
            "NG",
        )

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

    fun canonicalKey(raw: String): String =
        normalizeDisplay(raw)
            .replace(nonAlphaNumericPattern, " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    fun isMeaningfulItem(raw: String): Boolean {
        val normalized = canonicalKey(raw)
        return normalized.isNotBlank() &&
            normalized !in ignoredTokens &&
            normalized.length >= 2 &&
            normalized.any { it in 'A'..'Z' }
    }

    fun expandProblemItems(raw: String): List<String> =
        splitPattern
            .split(raw)
            .map(::normalizeDisplay)
            .filter(::isMeaningfulItem)
            .distinct()
}
