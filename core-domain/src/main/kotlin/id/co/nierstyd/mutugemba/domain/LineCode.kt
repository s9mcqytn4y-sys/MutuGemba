package id.co.nierstyd.mutugemba.domain

enum class LineCode(
    val label: String,
) {
    PRESS("Press"),
    SEWING("Sewing"),
    ;

    companion object {
        fun fromStorage(value: String?): LineCode = values().firstOrNull { it.name == value } ?: PRESS
    }
}
