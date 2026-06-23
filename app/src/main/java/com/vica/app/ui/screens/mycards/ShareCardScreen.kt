package com.vica.app.ui.screens.mycards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.data.model.CardModel
import com.vica.app.proximity.ProximityManager
import com.vica.app.ui.components.PremiumCardView
import com.vica.app.ui.theme.*

/**
 * ShareCardScreen — Sender-side NFC share sheet.
 *
 * Matches the iOS design:
 *  • Card preview at the top
 *  • NEARBY / QR toggle
 *  • Large animated NFC wave ring in the centre
 *  • "Searching…" / "HOLD DEVICES CLOSE TOGETHER" status copy
 *  • Inline state feedback (confirmed ✓, failed ✗)
 */
@Composable
fun ShareCardScreen(
    card: CardModel,
    proximityState: ProximityManager.ProximityState,
    onDismiss: () -> Unit
) {
    var shareMode by remember { mutableStateOf(ShareMode.NEARBY) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top bar: Cancel  •  NEARBY | QR  •  Share ─────────────────
            ShareTopBar(
                mode       = shareMode,
                onModeChange = { shareMode = it },
                onDismiss  = onDismiss
            )

            Spacer(Modifier.height(24.dp))

            // ── Card preview ──────────────────────────────────────────────
            PremiumCardView(
                card     = card,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ── Content switches between NEARBY ring and QR placeholder ───
            AnimatedContent(
                targetState = shareMode,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                },
                label = "mode_switch"
            ) { mode ->
                when (mode) {
                    ShareMode.NEARBY -> NearbyContent(state = proximityState)
                    ShareMode.QR     -> QrContent()
                }
            }
        }
    }
}

// ─── Top navigation bar ───────────────────────────────────────────────────────

private enum class ShareMode { NEARBY, QR }

@Composable
private fun ShareTopBar(
    mode: ShareMode,
    onModeChange: (ShareMode) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel
        Text(
            text       = "Cancel",
            color      = VicaGrey,
            fontSize   = 17.sp,
            fontWeight = FontWeight.Normal,
            modifier   = Modifier
                .clickable(onClick = onDismiss)
                .padding(4.dp)
        )

        Spacer(Modifier.weight(1f))

        // NEARBY | QR pill toggle — exact copy of iOS segmented control
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(VicaCard)
                .border(
                    width  = 1.dp,
                    color  = VicaBorder,
                    shape  = RoundedCornerShape(100.dp)
                )
                .padding(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModeTab(
                    label     = "NEARBY",
                    selected  = mode == ShareMode.NEARBY,
                    onClick   = { onModeChange(ShareMode.NEARBY) }
                )
                Spacer(Modifier.width(2.dp))
                ModeTab(
                    label     = "QR",
                    selected  = mode == ShareMode.QR,
                    onClick   = { onModeChange(ShareMode.QR) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Share / upload icon (right side — matches iOS)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VicaCard)
                .border(1.dp, VicaBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.Share,
                contentDescription = "Share via other methods",
                tint               = VicaGrey,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (selected) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(VicaGold.copy(alpha = 0.18f), VicaGold.copy(alpha = 0.08f))
                    )
                ) else Modifier
            )
            .border(
                width  = if (selected) 1.dp else 0.dp,
                color  = if (selected) VicaGold.copy(alpha = 0.35f) else Color.Transparent,
                shape  = RoundedCornerShape(100.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color      = if (selected) VicaGold else VicaGrey,
            letterSpacing = 0.8.sp
        )
    }
}

// ─── NEARBY content ───────────────────────────────────────────────────────────

@Composable
private fun NearbyContent(state: ProximityManager.ProximityState) {
    val isListening = state == ProximityManager.ProximityState.LISTENING
    val confirmed   = state == ProximityManager.ProximityState.CONFIRMED
    val failed      = state == ProximityManager.ProximityState.FAILED

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── NFC Wave Ring ─────────────────────────────────────────────────
        NfcWaveRing(
            isListening = isListening,
            confirmed   = confirmed,
            failed      = failed
        )

        Spacer(Modifier.height(28.dp))

        // ── Status headline ───────────────────────────────────────────────
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "status_text"
        ) { s ->
            when (s) {
                ProximityManager.ProximityState.IDLE -> StatusBlock(
                    headline  = "Preparing…",
                    sub       = "Arming NFC transfer",
                    color     = VicaGrey
                )
                ProximityManager.ProximityState.LISTENING -> StatusBlock(
                    headline  = "Searching…",
                    sub       = "HOLD DEVICES CLOSE TOGETHER",
                    color     = VicaWhite
                )
                ProximityManager.ProximityState.CONFIRMED -> StatusBlock(
                    headline  = "Card Sent!",
                    sub       = "TRANSFER COMPLETE",
                    color     = VicaSuccess
                )
                ProximityManager.ProximityState.FAILED -> StatusBlock(
                    headline  = "Not Found",
                    sub       = "MOVE DEVICES CLOSER AND TRY AGAIN",
                    color     = VicaError
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── "No connection? Use QR instead" hint ──────────────────────────
        if (isListening) {
            Text(
                text       = "No connection? Use QR instead →",
                color      = VicaGrey,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatusBlock(headline: String, sub: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = headline,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text          = sub,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            color         = VicaGrey,
            letterSpacing = 1.5.sp,
            textAlign     = TextAlign.Center
        )
    }
}

// ─── NFC Wave Ring ────────────────────────────────────────────────────────────

/**
 * Three-ring radiating wave animation that mirrors the iOS CoreNFC "proximity" UI.
 * Each ring is staggered so they ripple outward continuously while [isListening].
 */
@Composable
private fun NfcWaveRing(
    isListening: Boolean,
    confirmed: Boolean,
    failed: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_waves")

    // Three ripple rings staggered by 500ms each
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 0.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_alpha"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 0.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_alpha"
    )

    val ring3Scale by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, delayMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3_scale"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 0.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3_alpha"
    )

    // Centre icon gentle pulse
    val iconScale by infiniteTransition.animateFloat(
        initialValue  = 0.94f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    val ringColor = when {
        confirmed -> VicaSuccess
        failed    -> VicaError
        else      -> VicaGold
    }

    Box(
        modifier         = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ripple rings — animate while listening, subtle static ring in IDLE
        if (isListening) {
            // Ring 3 (largest, outermost)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(ring3Scale)
                    .alpha(ring3Alpha * 0.4f)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
            )
            // Ring 2
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(ring2Scale)
                    .alpha(ring2Alpha * 0.55f)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
            )
            // Ring 1 (innermost ripple)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(ring1Scale)
                    .alpha(ring1Alpha * 0.7f)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
            )
        }

        // Static outer guide ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (confirmed || failed)
                        ringColor.copy(alpha = 0.5f)
                    else
                        VicaBorder,
                    shape = CircleShape
                )
        )

        // Inner filled circle with icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = if (isListening) 0.22f else 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = ringColor.copy(alpha = if (isListening) 0.45f else 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconVector = when {
                confirmed -> Icons.Rounded.CheckCircle
                failed    -> Icons.Rounded.Close
                else      -> Icons.Rounded.Nfc
            }
            Icon(
                imageVector        = iconVector,
                contentDescription = "NFC status",
                tint               = ringColor,
                modifier           = Modifier
                    .size(44.dp)
                    .scale(if (isListening) iconScale else 1f)
            )
        }
    }
}

// ─── QR content (placeholder) ─────────────────────────────────────────────────

@Composable
private fun QrContent() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QR code placeholder box
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(VicaCard)
                .border(1.dp, VicaBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.QrCode2,
                contentDescription = "QR Code",
                tint               = VicaGrey,
                modifier           = Modifier.size(80.dp)
            )
        }
        Text(
            "QR sharing coming soon",
            color     = VicaGrey,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
