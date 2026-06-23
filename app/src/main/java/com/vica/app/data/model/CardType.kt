package com.vica.app.data.model

enum class CardType(val rawValue: String) {
    PERSONAL("Personal"),
    BUSINESS("Business"),
    SOCIAL("Social"),
    EVENT("Event"),
    BLANK("Custom Blank");

    val displayName: String
        get() = when (this) {
            PERSONAL -> "Personal"
            BUSINESS -> "Business"
            SOCIAL -> "Social"
            EVENT -> "Event"
            BLANK -> "Custom"
        }

    val emoji: String
        get() = when (this) {
            PERSONAL -> "🌱"
            BUSINESS -> "💼"
            SOCIAL -> "✨"
            EVENT -> "🎟️"
            BLANK -> "🧩"
        }

    companion object {
        fun fromRaw(raw: String): CardType {
            return entries.firstOrNull { it.rawValue == raw } ?: PERSONAL
        }
    }
}
