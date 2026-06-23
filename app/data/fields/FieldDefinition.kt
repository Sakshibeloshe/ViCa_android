package com.vica.app.data.fields

data class FieldDefinition(
    val key: String,
    val label: String,
    val placeholder: String,
    val kind: FieldKind,
    val required: Boolean,
    val keyboard: KeyboardType
) {
    enum class KeyboardType {
        NORMAL, EMAIL, PHONE, URL
    }
}
