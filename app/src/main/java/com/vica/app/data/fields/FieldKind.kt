package com.vica.app.data.fields

enum class FieldKind(val rawValue: String) {
    TEXT("text"),
    EMAIL("email"),
    PHONE("phone"),
    URL("url"),
    HANDLE("handle"),
    PICKER("picker");

    companion object {
        fun fromRaw(raw: String): FieldKind {
            return entries.firstOrNull { it.rawValue == raw } ?: TEXT
        }
    }
}
