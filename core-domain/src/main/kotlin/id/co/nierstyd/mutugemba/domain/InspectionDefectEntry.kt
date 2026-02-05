package id.co.nierstyd.mutugemba.domain

data class InspectionDefectEntry(
    val defectTypeId: Long,
    val quantity: Int,
    val slots: List<InspectionDefectSlot> = emptyList(),
) {
    val totalQuantity: Int
        get() = if (slots.isNotEmpty()) slots.sumOf { it.quantity } else quantity
}
