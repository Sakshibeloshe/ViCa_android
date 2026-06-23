package com.vica.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.ui.theme.CharcoalGrey
import com.vica.app.ui.theme.FreshLime
import com.vica.app.ui.theme.SkyBlue
import com.vica.app.ui.theme.SoftRose

// ─── Tab enum — mirrors iOS Tab enum in RootTabView.swift ─────────────────────

enum class ViCaTab(val label: String) {
    MY_CARDS("My Cards"),
    ADD_CARD("Add Card"),
    INBOX("Inbox"),
    SCAN("Scan")
}

// ─── FloatingTabBar — pixel-faithful replica of iOS FloatingTabBar.swift ──────
//
// Capsule background with CharcoalGrey.opacity(0.85)
// Tab buttons: width 80, height 58, capsule shape, active = activeColor background
// Active tab glow: shadow(color: activeColor.opacity(0.55), radius: 10)
// Divider: Rectangle 1pt wide, 28pt tall, white.opacity(0.12)
// QR button: 58x58, white.opacity(0.7)
// Shadow: black.opacity(0.5) radius 28
// Bottom padding: 10dp
// Spring animation: dampingFraction 0.65

@Composable
fun FloatingTabBar(
    selectedTab: ViCaTab,
    onTabSelected: (ViCaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .shadow(
                    elevation     = 28.dp,
                    shape         = CircleShape,
                    ambientColor  = Color.Black.copy(alpha = 0.5f),
                    spotColor     = Color.Black.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(CharcoalGrey.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // My Cards tab — active color: FreshLime
            ViCaTabButton(
                icon        = Icons.Default.CreditCard,
                label       = "My Cards",
                isSelected  = selectedTab == ViCaTab.MY_CARDS,
                activeColor = FreshLime,
                onClick     = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(ViCaTab.MY_CARDS)
                }
            )

            // Add Card tab — active color: SoftRose
            ViCaTabButton(
                icon        = Icons.Default.Add,
                label       = "Add Card",
                isSelected  = selectedTab == ViCaTab.ADD_CARD,
                activeColor = SoftRose,
                onClick     = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(ViCaTab.ADD_CARD)
                }
            )

            // Inbox tab — active color: SkyBlue
            ViCaTabButton(
                icon        = Icons.Default.Group,
                label       = "Inbox",
                isSelected  = selectedTab == ViCaTab.INBOX,
                activeColor = SkyBlue,
                onClick     = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(ViCaTab.INBOX)
                }
            )

            // Divider — 1pt × 28pt, white.opacity(0.12)
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // QR Scan button — 58×58, icon + label
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedTab == ViCaTab.SCAN) Color.White.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(ViCaTab.SCAN)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR",
                    tint               = Color.White.copy(alpha = if (selectedTab == ViCaTab.SCAN) 1f else 0.7f),
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "Scan",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = if (selectedTab == ViCaTab.SCAN) 1f else 0.7f),
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ─── TabButton — mirrors iOS TabButton struct ──────────────────────────────────
//
// width 80, height 58, capsule clip, activeColor background when selected
// Glow: shadow(color: activeColor.opacity(0.55), radius: 10)
// Scale: 1.05 when selected, animated spring(response:0.3, dampingFraction:0.6)

@Composable
private fun ViCaTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label         = "tabScale"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .width(80.dp)
            .height(58.dp)
            .then(
                if (isSelected) Modifier.shadow(
                    elevation    = 0.dp,
                    shape        = CircleShape,
                    ambientColor = activeColor.copy(alpha = 0.55f),
                    spotColor    = activeColor.copy(alpha = 0.55f)
                ) else Modifier
            )
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (isSelected) CharcoalGrey else Color.White.copy(alpha = 0.4f),
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = if (isSelected) CharcoalGrey else Color.White.copy(alpha = 0.4f),
            textAlign  = TextAlign.Center
        )
    }
}
