package id.co.nierstyd.mutugemba.desktop.ui.components

data class FieldSpec(
    val label: String,
    val placeholder: String? = null,
    val helperText: String? = null,
    val isError: Boolean = false,
    val enabled: Boolean = true,
)
