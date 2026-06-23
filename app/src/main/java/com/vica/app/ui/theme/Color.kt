package com.vica.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Exact hex palette from iOS Color+AppPalette.swift ───────────────────────

// Background
val ObsidianBlack   = Color(0xFF0D0D0D)
val CharcoalGrey    = Color(0xFF2C2C2C)

// Card themes — exact match to iOS CardTheme.swift
val SoftRose        = Color(0xFFFFB3BA)   // .pink
val FreshLime       = Color(0xFFC8F59A)   // .lime
val SkyBlue         = Color(0xFF99D6F5)   // .sky
val LavenderPurple  = Color(0xFFCFB8F5)   // .lavender
val SoftTerracotta  = Color(0xFFE8A898)   // .peach

// Onboarding slide tint colours
val SlideRose       = Color(0xFFFFB3C0)
val SlideLime       = Color(0xFFC7F399)
val SlideSky        = Color(0xFF99D9F2)

// Template row accent colours — from AddCardView.swift
val PersonalAccent  = Color(0xFFFFB3C0)
val BusinessAccent  = Color(0xFFF2E699)
val SocialAccent    = Color(0xFF99D9F2)
val EventAccent     = Color(0xFFCCB3F2)
val CustomAccent    = Color(0xFF808080)

// Utility
val ViCaWhite       = Color(0xFFFFFFFF)
val ViCaBlack       = Color(0xFF000000)

// ─── Legacy aliases (used in older screens, kept for compatibility) ────────────
val VicaBlack       = ObsidianBlack
val VicaDark        = Color(0xFF12121A)
val VicaSurface     = CharcoalGrey
val VicaCard        = Color(0xFF232334)
val VicaBorder      = Color(0xFF2E2E44)
val VicaGold        = Color(0xFFD4AF37)
val VicaGoldLight   = Color(0xFFF2D991)
val VicaGoldDim     = Color(0xFF8A6E27)

val VicaWhite       = Color(0xFFF4F4F8)
val VicaGrey        = Color(0xFF8E8E9F)
val VicaGreyLight   = Color(0xFFC7C7D4)

// Card Theme aliases
val ThemePink       = SoftRose
val ThemePinkLight  = Color(0xFFFFF0F2)
val ThemeLime       = FreshLime
val ThemeLimeLight  = Color(0xFFF1FCE9)
val ThemeSky        = SkyBlue
val ThemeSkyLight   = Color(0xFFE5F5FD)
val ThemeLavender   = LavenderPurple
val ThemeLavenderL  = Color(0xFFF4EFFF)
val ThemePeach      = SoftTerracotta
val ThemePeachLight = Color(0xFFFDF1EE)

// Semantic
val VicaError       = Color(0xFFFA6E6E)
val VicaSuccess     = Color(0xFF85F5B9)