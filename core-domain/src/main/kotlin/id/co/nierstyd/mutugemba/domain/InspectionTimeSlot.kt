package id.co.nierstyd.mutugemba.domain

enum class InspectionTimeSlot(
    val code: String,
    val label: String,
) {
    MORNING("08_12", "08:00-12:00"),
    MIDDAY("12_1530", "12:00-15:30"),
    AFTERNOON("1530_1700", "15:30-17:00"),
    OVERTIME("OVERTIME", "Lembur"),
    ;

    companion object {
        fun standardSlots(): List<InspectionTimeSlot> = listOf(MORNING, MIDDAY, AFTERNOON)

        fun fromCode(code: String?): InspectionTimeSlot = values().firstOrNull { it.code == code } ?: MORNING
    }
}
