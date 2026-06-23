package com.vica.app.data.fields

import com.vica.app.data.model.CardType

object FieldCatalog {
    fun fields(type: CardType): List<FieldDefinition> {
        return when (type) {
            CardType.PERSONAL -> listOf(
                FieldDefinition("fullName", "Name", "Your name", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("pronouns", "Pronouns", "She/Her", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("phone", "Phone", "+1 ...", FieldKind.PHONE, true, FieldDefinition.KeyboardType.PHONE),
                FieldDefinition("email", "Email", "name@email.com", FieldKind.EMAIL, false, FieldDefinition.KeyboardType.EMAIL),
                FieldDefinition("instagram", "Instagram", "@username", FieldKind.HANDLE, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("whatsapp", "WhatsApp", "Phone number", FieldKind.PHONE, false, FieldDefinition.KeyboardType.PHONE),
                FieldDefinition("snapchat", "Snapchat", "@username", FieldKind.HANDLE, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("bio", "Short Bio", "I love design + travel", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("locationCity", "Location (City)", "San Francisco", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("intent", "Intent", "Open to meet people", FieldKind.PICKER, false, FieldDefinition.KeyboardType.NORMAL)
            )
            CardType.BUSINESS -> listOf(
                FieldDefinition("fullName", "Name", "Your name", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("company", "Company", "EcoCard Inc.", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("title", "Job Title", "Product Designer", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("email", "Email", "you@company.com", FieldKind.EMAIL, true, FieldDefinition.KeyboardType.EMAIL),
                FieldDefinition("phone", "Phone", "+1 ...", FieldKind.PHONE, true, FieldDefinition.KeyboardType.PHONE),
                FieldDefinition("website", "Website", "yourdomain.com", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("linkedin", "LinkedIn", "linkedin.com/in/...", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("officeLocation", "Office Location", "123 Business St, NY", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("intent", "Intent", "Looking for...", FieldKind.PICKER, false, FieldDefinition.KeyboardType.NORMAL)
            )
            CardType.SOCIAL -> listOf(
                FieldDefinition("nickname", "Display Name / Nickname", "Riya / @riya", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("instagram", "Instagram", "@username", FieldKind.HANDLE, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("snapchat", "Snapchat", "@username", FieldKind.HANDLE, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("spotify", "Spotify", "Profile link", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("whatsapp", "WhatsApp", "Phone number", FieldKind.PHONE, false, FieldDefinition.KeyboardType.PHONE),
                FieldDefinition("bio", "Vibe Bio", "What are you about?", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("emojiTags", "Emoji Tags", "🎨 ✈️ 🍕", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("intent", "Intent", "What's the vibe?", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL)
            )
            CardType.EVENT -> listOf(
                FieldDefinition("fullName", "Name", "Your name", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("eventBadge", "Event Badge", "WWDC Student / Attendee", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("skillsTags", "Skills Tags", "Swift, ML, UIKit", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("linkedin", "LinkedIn", "linkedin.com/in/...", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("github", "GitHub", "github.com/...", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("bio", "About Me", "Working on ML apps...", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("intent", "Intent", "Looking for teammates", FieldKind.PICKER, false, FieldDefinition.KeyboardType.NORMAL)
            )
            CardType.BLANK -> listOf(
                FieldDefinition("fullName", "Name", "Your name", FieldKind.TEXT, true, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("title", "Section Title", "My Project", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("bio", "Text Block", "Description here...", FieldKind.TEXT, false, FieldDefinition.KeyboardType.NORMAL),
                FieldDefinition("website", "Icon + Text Link", "yourlink.com", FieldKind.URL, false, FieldDefinition.KeyboardType.URL),
                FieldDefinition("instagram", "Social Handle", "@username", FieldKind.HANDLE, false, FieldDefinition.KeyboardType.NORMAL)
            )
        }
    }

    fun intents(type: CardType): List<String> {
        return when (type) {
            CardType.PERSONAL -> listOf("Open to meet new people", "Collaborate", "Connect")
            CardType.BUSINESS -> listOf("Open to internships", "Open to freelance work", "Hiring", "Looking for collaboration")
            CardType.EVENT -> listOf("Looking for team members", "Open to ideas", "Seeking partners")
            else -> emptyList()
        }
    }
}
