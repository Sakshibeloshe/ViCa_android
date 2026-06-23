package com.vica.app.ui.screens.mycards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.data.model.CardModel
import com.vica.app.ui.components.PremiumCardView
import com.vica.app.ui.theme.*

/**
 * Full-screen overlay shown on the receiver's phone after a successful NFC tap.
 *
 * Shows:
 *   1. Animated "NFC received" burst at the top
 *   2. Card preview flying in from bottom
 *   3. Sender name + "Received via NFC"
 *   4. "Save to Inbox" + "Dismiss" action buttons
 */
@Composable
fun ReceivedCardScreen(
    card: CardModel,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VicaBlack,
                        VicaDark.copy(alpha = 0.97f),
                        VicaBlack
                    )
                )
            )
    ) {
        // ── Dismiss / close button ──────────────────────────────────────────
        IconButton(
            onClick  = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(VicaCard)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = VicaGrey)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── NFC burst icon ──────────────────────────────────────────────
            NfcBurstIcon()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Card Received!",
                style = MaterialTheme.typography.headlineMedium,
                color = VicaWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "via NFC tap",
                style = MaterialTheme.typography.bodySmall,
                color = VicaGold,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Card fly-in ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                PremiumCardView(
                    card     = card,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Sender info ─────────────────────────────────────────────────
            Text(
                text  = "From ${card.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = VicaWhite,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (card.org != null) {
                Text(
                    text  = card.org,
                    style = MaterialTheme.typography.bodySmall,
                    color = VicaGrey,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Action buttons ──────────────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dismiss
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VicaGrey
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, VicaBorder
                    )
                ) {
                    Text("Dismiss", fontWeight = FontWeight.SemiBold)
                }

                // Save
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1.6f),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VicaGold,
                        contentColor   = VicaBlack
                    )
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Inbox", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── NFC Burst Animation ──────────────────────────────────────────────────────

@Composable
private fun NfcBurstIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_burst")

    val outerScale by infiniteTransition.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )
    val outerAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_alpha"
    )
    val innerScale by infiniteTransition.animateFloat(
        initialValue  = 0.9f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_scale"
    )

    Box(
        modifier        = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(outerScale)
                .clip(CircleShape)
                .background(VicaSuccess.copy(alpha = outerAlpha))
        )
        // Inner ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(innerScale)
                .clip(CircleShape)
                .background(VicaSuccess.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector       = Icons.Rounded.Nfc,
                contentDescription = "NFC",
                tint              = VicaSuccess,
                modifier          = Modifier.size(34.dp)
            )
        }
    }
}
