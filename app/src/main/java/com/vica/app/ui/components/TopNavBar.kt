package com.vica.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.ui.theme.CharcoalGrey
import com.vica.app.ui.theme.SkyBlue
import com.vica.app.ui.theme.SoftRose

// ─── TopNavBar ─────────────────────────────────────────────────────────────────
//
// iOS reference — TopNavBar.swift:
//   HStack: ReceiverModeButton | EventModeButton | Spacer | settings gear
//   Conditional banners: event status (softRose) OR receiver status (skyBlue)

@Composable
fun ViCaTopNavBar(
    isReceiverActive: Boolean = false,
    isEventLive: Boolean = false,
    eventCode: String = "",
    connectedPeerCount: Int = 0,
    receivedCardCount: Int = 0,
    onReceiverToggle: () -> Unit = {},
    onEventToggle: () -> Unit = {},
    onStopEvent: () -> Unit = {},
    onSettingsTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // ── Button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Receiver mode button
            ReceiverModeButton(
                isLive  = isReceiverActive,
                onClick = onReceiverToggle
            )

            // Event mode button
            EventModeButton(
                isLive  = isEventLive,
                onClick = onEventToggle
            )

            Spacer(Modifier.weight(1f))

            // Settings gear
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable { onSettingsTapped() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint               = Color.White.copy(alpha = 0.8f),
                    modifier           = Modifier.size(16.dp)
                )
            }
        }

        // ── Status banners (animated)
        AnimatedVisibility(
            visible = isEventLive,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut()
        ) {
            EventStatusBanner(
                eventCode          = eventCode,
                connectedPeerCount = connectedPeerCount,
                receivedCardCount  = receivedCardCount,
                onStop             = onStopEvent
            )
        }

        AnimatedVisibility(
            visible = isReceiverActive && !isEventLive,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut()
        ) {
            ReceiverStatusBanner(isConnected = false)
        }
    }
}

// ─── ReceiverModeButton ────────────────────────────────────────────────────────

@Composable
fun ReceiverModeButton(
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (isLive) SkyBlue.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                1.dp,
                if (isLive) SkyBlue.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(100.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = null,
            tint               = if (isLive) SkyBlue else Color.White.copy(0.6f),
            modifier           = Modifier.size(12.dp)
        )
        Text(
            text          = "RECEIVER MODE",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Black,
            color         = if (isLive) SkyBlue else Color.White.copy(0.6f),
            letterSpacing = 0.5.sp
        )
    }
}

// ─── EventModeButton ───────────────────────────────────────────────────────────

@Composable
fun EventModeButton(
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (isLive) SoftRose.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                1.dp,
                if (isLive) SoftRose.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(100.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.FlashOn,
            contentDescription = null,
            tint               = if (isLive) SoftRose else Color.White.copy(0.6f),
            modifier           = Modifier.size(12.dp)
        )
        Text(
            text          = "EVENT MODE",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Black,
            color         = if (isLive) SoftRose else Color.White.copy(0.6f),
            letterSpacing = 0.5.sp
        )
    }
}

// ─── EventStatusBanner ────────────────────────────────────────────────────────

@Composable
fun EventStatusBanner(
    eventCode: String,
    connectedPeerCount: Int,
    receivedCardCount: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(SoftRose.copy(alpha = 0.08f))
            .border(1.dp, SoftRose.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
            .padding(horizontal = 16.dp)
            .defaultMinSize(minHeight = 52.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlowingDot(color = SoftRose)

        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "EVENT LIVE",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Black,
                    color         = SoftRose,
                    letterSpacing = 1.5.sp
                )
                Text("·", color = Color.White.copy(0.3f), fontSize = 10.sp)
                Text(
                    eventCode,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Black,
                    color         = Color.White.copy(0.7f),
                    letterSpacing = 2.sp,
                    fontFamily    = FontFamily.Monospace
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "👥 $connectedPeerCount",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(0.5f)
                )
                Text(
                    "🃏 $receivedCardCount",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(0.5f)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(SoftRose)
                .clickable { onStop() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "STOP",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Black,
                color         = Color.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

// ─── ReceiverStatusBanner ─────────────────────────────────────────────────────

@Composable
fun ReceiverStatusBanner(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(SkyBlue.copy(alpha = if (isConnected) 0.12f else 0.05f))
            .border(
                1.dp,
                SkyBlue.copy(alpha = if (isConnected) 0.35f else 0.15f),
                RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlowingDot(color = SkyBlue)
        Text(
            text          = if (isConnected) "CONNECTED — RECEIVING CARD"
                            else "YOUR DEVICE IS VISIBLE TO OTHERS",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Black,
            color         = SkyBlue.copy(0.9f),
            letterSpacing = 1.5.sp
        )
    }
}

// ─── GlowingDot ───────────────────────────────────────────────────────────────

@Composable
fun GlowingDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 2.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ─── FilterPills ──────────────────────────────────────────────────────────────

@Composable
fun ViCaFilterPills(
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(100.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(item)
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text          = item.uppercase(),
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = if (isSelected) Color.Black else Color.White.copy(alpha = 0.35f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── SearchBar ────────────────────────────────────────────────────────────────

@Composable
fun ViCaSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.35f),
            modifier           = Modifier.size(16.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            textStyle     = androidx.compose.ui.text.TextStyle(
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color      = Color.White.copy(alpha = 0.3f),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                innerTextField()
            }
        )
    }
}
