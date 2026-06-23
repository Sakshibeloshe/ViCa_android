package com.vica.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.data.model.CardModel
import com.vica.app.data.model.CardTheme
import com.vica.app.ui.theme.*

// ─── Card theme → exact iOS color mapping ─────────────────────────────────────

private fun cardThemeColor(theme: CardTheme): Color = when (theme) {
    CardTheme.PINK     -> SoftRose
    CardTheme.LIME     -> FreshLime
    CardTheme.SKY      -> SkyBlue
    CardTheme.LAVENDER -> LavenderPurple
    CardTheme.PEACH    -> SoftTerracotta
}

// ─── PremiumCardPattern ────────────────────────────────────────────────────────
//
// iOS reference — PremiumCardPattern.swift:
//   1. Base colour fill
//   2. Radial white highlight at top-right (radius 600)
//   3. Dot grid: 1.6pt dots on 10pt spacing, black.opacity(0.06)
//   4. Noise overlay: black.opacity(0.03)

@Composable
private fun PremiumCardPattern(
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1 — base color
        Box(Modifier.fillMaxSize().background(backgroundColor))

        // Layer 2 — radial gradient highlight at top-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 600f
                    )
                )
        )

        // Layer 3 — dot grid (matches iOS Canvas dot loop)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = 0.8f * density
            val spacing   = 10.dp.toPx()
            var x = spacing / 2
            while (x < size.width) {
                var y = spacing / 2
                while (y < size.height) {
                    drawCircle(
                        color  = Color.Black.copy(alpha = 0.06f),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += spacing
                }
                x += spacing
            }
        }

        // Layer 4 — subtle noise overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.03f))
        )
    }
}

// ─── PremiumCardView (= iOS CardFrontView) ─────────────────────────────────────
//
// iOS reference — CardFrontView.swift:
//   aspectRatio(1.5), cornerRadius 36, padding 32
//   Top row: name (size 36, bold) + subtitle (size 11, black weight, tracking 2, opacity 0.35)
//   Photo slot: 84×84, cornerRadius 24, charcoalGrey.opacity(0.06)
//   Bottom row: org (size 12, black weight, tracking 1.5, opacity 0.7) + email/website (size 13, opacity 0.5)
//   Send button: Circle white.opacity(0.3) 48×48
//   isSelected: scaleEffect 1.02

@Composable
fun PremiumCardView(
    card: CardModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    isSelected: Boolean = false,
    onShareClick: (() -> Unit)? = null
) {
    val themeColor = cardThemeColor(card.theme)

    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label         = "cardScale"
    )

    val email   = card.fields.firstOrNull { it.key == "email"   }?.value
    val website = card.fields.firstOrNull { it.key == "website" || it.key == "linkedin" || it.key == "instagram" }?.value

    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .scale(scale)
            .shadow(
                elevation    = if (isSelected) 20.dp else 10.dp,
                shape        = RoundedCornerShape(ViCaRadius.card),
                ambientColor = themeColor.copy(alpha = 0.15f),
                spotColor    = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(ViCaRadius.card))
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(ViCaRadius.card))
    ) {
        // Pattern background
        PremiumCardPattern(backgroundColor = themeColor)

        // Content layer — padding 32dp all sides (20dp in compact mode)
        val contentPadding = if (isCompact) 16.dp else 32.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top row: name/subtitle + photo
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Name — size 36 in normal mode, 20 in compact
                    Text(
                        text       = card.displayName,
                        fontSize   = if (isCompact) 20.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = CharcoalGrey,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = if (isCompact) 24.sp else 40.sp
                    )
                    // Subtitle — size 11, black weight, tracking 2, opacity 0.35
                    if (!card.subtitle.isNullOrBlank()) {
                        Text(
                            text          = card.subtitle.uppercase(),
                            fontSize      = if (isCompact) 9.sp else 11.sp,
                            fontWeight    = FontWeight.Black,
                            color         = CharcoalGrey.copy(alpha = 0.35f),
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Photo slot
                CardPhotoSlot(
                    photoData = card.photoData,
                    size      = if (isCompact) 52.dp else 84.dp,
                    radius    = if (isCompact) 14.dp else 24.dp
                )
            }

            // ── Bottom row: org/contact info + send button
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Org — size 12, black weight, tracking 1.5, opacity 0.7
                    if (!card.org.isNullOrBlank()) {
                        Text(
                            text          = card.org.uppercase(),
                            fontSize      = if (isCompact) 9.sp else 12.sp,
                            fontWeight    = FontWeight.Black,
                            color         = CharcoalGrey.copy(alpha = 0.7f),
                            letterSpacing = 1.5.sp
                        )
                    }
                    // Email
                    if (!email.isNullOrBlank()) {
                        Text(
                            text       = email,
                            fontSize   = if (isCompact) 9.sp else 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = CharcoalGrey.copy(alpha = 0.5f),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                    // Website / Social
                    if (!website.isNullOrBlank()) {
                        Text(
                            text       = website,
                            fontSize   = if (isCompact) 9.sp else 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = CharcoalGrey.copy(alpha = 0.5f),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }

                // Send button — Circle white.opacity(0.3), 48dp (36dp compact)
                if (onShareClick != null) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 36.dp else 48.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(0.08f))
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                            .clickable { onShareClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Send,
                            contentDescription = "Share",
                            tint               = CharcoalGrey,
                            modifier           = Modifier.size(if (isCompact) 14.dp else 18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Card photo slot ───────────────────────────────────────────────────────────

@Composable
private fun CardPhotoSlot(
    photoData: ByteArray?,
    size: androidx.compose.ui.unit.Dp = 84.dp,
    radius: androidx.compose.ui.unit.Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(radius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(CharcoalGrey.copy(alpha = 0.06f))
            .border(1.dp, Color.Black.copy(alpha = 0.04f), shape)
    ) {
        if (photoData != null) {
            val bitmap = remember(photoData) {
                try { BitmapFactory.decodeByteArray(photoData, 0, photoData.size)?.asImageBitmap() }
                catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(
                    bitmap             = bitmap,
                    contentDescription = "Profile photo",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(shape)
                )
            }
        }
    }
}

// ─── Animated shimmer override for card selection state ─────────────────────────
//
// Use animatedShimmer() in a @Composable context for the full sweep animation.
// The plain shimmer() from ShimmerModifier.kt is used for loading placeholders.

@Composable
fun Modifier.animatedShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue  = -1200f,
        targetValue   = 1200f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                start = Offset(translateX, 0f),
                end   = Offset(translateX + size.width, size.height)
            )
        )
    }
}
