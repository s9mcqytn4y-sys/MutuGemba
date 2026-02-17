package id.co.nierstyd.mutugemba.domain

object DefectNameSanitizer {
    private val leadingCodePattern = Regex("^(\\([^)]+\\)|[-–•]+)\\s*")
    private val trailingVariantPattern = Regex("\\s+[A-Z]$")
    private val splitPattern = Regex("[,;/\\n]+")
    private val compactWhitespacePattern = Regex("\\s+")
    private val nonWordPattern = Regex("[^A-Z0-9/ ]")
    private val leadingNoiseTokenPattern =
        Regex(
            "(^\\d{2,}[A-Z0-9-]*$)|(^[A-Z]{1,3}\\d+[A-Z0-9-]*$)|(^[A-Z0-9]+-[A-Z0-9-]+$)",
        )
    private val prefixNoiseWords = setOf("DEFECT", "ITEM", "PROBLEM", "NG", "TYPE")
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
        val aliased =
            if (normalized.isBlank()) {
                ""
            } else {
                aliasReplacements.entries.fold(normalized) { acc, (from, to) ->
                    acc.replace("\\b$from\\b".toRegex(), to)
                }
            }
        val compacted =
            aliased
                .replace(nonWordPattern, " ")
                .replace(compactWhitespacePattern, " ")
                .trim()
        val cleanedTokens = stripLeadingNoiseTokens(compacted.split(" ").filter { it.isNotBlank() })
        return cleanedTokens
            .let(::deduplicateAdjacentTokens)
            .joinToString(" ")
            .ifBlank { compacted }
    }

    fun expandProblemItems(raw: String): List<String> =
        splitPattern
            .split(raw)
            .map(::canonicalKey)
            .filter { it.isNotBlank() }
            .distinct()

    private fun stripLeadingNoiseTokens(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return tokens
        val mutable = tokens.toMutableList()
        while (mutable.size > 1) {
            val head = mutable.first()
            val shouldDrop =
                head in prefixNoiseWords ||
                    leadingNoiseTokenPattern.matches(head) ||
                    (head.length == 1 && head.any(Char::isDigit))
            if (!shouldDrop) break
            mutable.removeAt(0)
        }
        return mutable
    }

    private fun deduplicateAdjacentTokens(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return tokens
        val merged = mutableListOf<String>()
        tokens.forEach { token ->
            if (merged.lastOrNull() != token) {
                merged += token
            }
        }
        return merged
    }
}
