package com.vica.app.data.model

enum class CardTheme(val rawValue: String) {
    PINK("pink"),
    LIME("lime"),
    SKY("sky"),
    LAVENDER("lavender"),
    PEACH("peach");

    companion object {
        fun fromRaw(raw: String): CardTheme {
            return entries.firstOrNull { it.rawValue == raw } ?: PINK
        }
    }
}
