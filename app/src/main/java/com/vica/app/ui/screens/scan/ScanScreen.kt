package com.vica.app.ui.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.ui.theme.*

/**
 * ScanScreen — high-fidelity simulated camera viewfinder screen.
 * Replicates the elegant iOS QR scanner view with animated laser scanner line and brackets.
 */
@Composable
fun ScanScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VicaBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text       = "Scan QR Code",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = VicaWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Hold viewfinder over a ViCa QR code",
                    style = MaterialTheme.typography.bodySmall,
                    color = VicaGrey
                )
            }

            // Viewfinder Simulator
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, VicaBorder, RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                // Brackets in the corners
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Simulated QR Laser Scan Sweep
                    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
                    val laserYOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue  = 232.dp.value, // sweeps length
                        animationSpec = infiniteRepeatable(
                            animation  = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_y"
                    )

                    // The animated green/gold laser sweep line
                    Box(
                        modifier = Modifier
                            .offset(y = laserYOffset.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        VicaGold.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Center hint text
                Text(
                    "Simulating Camera...",
                    color = VicaGrey.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "Align the QR code within the frame to automatically scan and import card details.",
                style = MaterialTheme.typography.bodyMedium,
                color = VicaGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
