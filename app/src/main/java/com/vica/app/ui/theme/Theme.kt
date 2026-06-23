package com.vica.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ─── MaterialTheme wrapper — mirrors iOS ViCaTheme ────────────────────────────

private val ViCaDarkColorScheme = darkColorScheme(
    background          = ObsidianBlack,
    surface             = CharcoalGrey,
    primary             = FreshLime,
    secondary           = SkyBlue,
    tertiary            = SoftRose,
    onBackground        = ViCaWhite,
    onSurface           = ViCaWhite,
    onPrimary           = CharcoalGrey,
    primaryContainer    = VicaGoldDim,
    onPrimaryContainer  = VicaGoldLight,
    surfaceVariant      = VicaCard,
    onSurfaceVariant    = VicaGrey,
    outline             = VicaBorder,
    error               = VicaError,
    onError             = ViCaWhite,
    onSecondary         = CharcoalGrey,
)

@Composable
fun ViCaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ViCaDarkColorScheme,
        typography  = ViCaTypography,
        content     = content
    )
}

// ─── Spacing constants ────────────────────────────────────────────────────────

object ViCaSpacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 20.dp
    val xl  = 24.dp
    val xxl = 32.dp
}

// ─── Corner radius constants ──────────────────────────────────────────────────

object ViCaRadius {
    val card        = 36.dp
    val folder      = 32.dp
    val pill        = 100.dp
    val templateRow = 32.dp
    val field       = 16.dp
    val icon        = 18.dp
    val button      = 18.dp
}