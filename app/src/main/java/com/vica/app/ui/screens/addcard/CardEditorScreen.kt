package com.vica.app.ui.screens.addcard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.data.fields.FieldDefinition
import com.vica.app.data.fields.FieldKind
import com.vica.app.data.model.CardField
import com.vica.app.data.model.CardModel
import com.vica.app.data.model.CardTheme
import com.vica.app.data.model.CardType
import com.vica.app.ui.components.PremiumCardView
import com.vica.app.ui.components.ViCaTopNavBar
import com.vica.app.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    selectedType: CardType,
    selectedTheme: CardTheme,
    fieldValues: Map<String, String>,
    fieldDefinitions: List<FieldDefinition>,
    isSaving: Boolean,
    onTypeSelected: (CardType) -> Unit,
    onThemeSelected: (CardTheme) -> Unit,
    onFieldChanged: (String, String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isEditing,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "editor_flow"
    ) { editing ->
        if (!editing) {
            // STATE 1: iOS-faithful AddCardView with staggered template rows
            var animateItems by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animateItems = true }

            val templateRows = listOf(
                Triple(Icons.Default.CreditCard,        "Personal Card",  "SOCIAL & BASIC INFO")       to Pair(PersonalAccent, CardType.PERSONAL),
                Triple(Icons.Default.BusinessCenter,    "Business Card",  "CORPORATE & PROFESSIONAL")  to Pair(BusinessAccent, CardType.BUSINESS),
                Triple(Icons.Default.AutoAwesome,       "Social Profile", "EXPRESSIVE & BOLD")         to Pair(SocialAccent,   CardType.SOCIAL),
                Triple(Icons.Default.ConfirmationNumber,"Event Badge",    "CONFERENCES & SKILLS")      to Pair(EventAccent,    CardType.EVENT),
                Triple(Icons.Default.Add,               "Custom Blank",   "BUILD FROM SCRATCH")        to Pair(CustomAccent,   CardType.PERSONAL)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // iOS-faithful TopNavBar
                    ViCaTopNavBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp)
                    )

                    // "Create Card" large title — iOS size 46, bold
                    val titleAlpha  by animateFloatAsState(if (animateItems) 1f else 0f, tween(400), label = "ta")
                    val titleOffset by animateFloatAsState(if (animateItems) 0f else 10f, tween(400), label = "to")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text       = "Create Card",
                            fontSize   = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White.copy(alpha = titleAlpha),
                            lineHeight = 52.sp,
                            modifier   = Modifier.offset(y = titleOffset.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text       = "Auto-filling details from your profile",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.35f * titleAlpha),
                            modifier   = Modifier.offset(y = titleOffset.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Staggered template rows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        templateRows.forEachIndexed { index, (iconTitleSub, colorType) ->
                            val (icon, title, subtitle) = iconTitleSub
                            val (color, type) = colorType
                            item {
                                val delayMs = 100 + index * 100L
                                val appeared by produceState(false) {
                                    kotlinx.coroutines.delay(delayMs)
                                    value = animateItems
                                }
                                val rowScale by animateFloatAsState(
                                    targetValue   = if (appeared) 1f else 0.9f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f),
                                    label         = "rs$index"
                                )
                                val rowAlpha by animateFloatAsState(
                                    targetValue   = if (appeared) 1f else 0f,
                                    animationSpec = tween(300),
                                    label         = "ra$index"
                                )
                                ViCaTemplateRow(
                                    icon     = icon,
                                    title    = title,
                                    subtitle = subtitle,
                                    color    = color,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(rowScale)
                                        .graphicsLayer { alpha = rowAlpha }
                                        .clickable {
                                            onTypeSelected(type)
                                            isEditing = true
                                        }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(120.dp)) }
                    }
                }
            }
        } else {
            // STATE 2: The elegant fields inputs editor form
            val liveCard = remember(selectedType, selectedTheme, fieldValues) {
                val displayName = fieldValues["fullName"] ?: fieldValues["nickname"] ?: "Your Name"
                val subtitle = fieldValues["title"] ?: fieldValues["eventBadge"] ?: "Your Subtitle"
                val org = fieldValues["company"] ?: fieldValues["eventName"] ?: ""
                val fieldsList = fieldValues.map { (k, v) ->
                    CardField(
                        id    = UUID.randomUUID(),
                        key   = k,
                        label = k,
                        value = v,
                        kind  = "TEXT"
                    )
                }
                CardModel(
                    id          = UUID.randomUUID(),
                    type        = selectedType,
                    displayName = displayName,
                    subtitle    = subtitle,
                    org         = org,
                    bio         = null,
                    themeHex    = selectedTheme.rawValue,
                    theme       = selectedTheme,
                    photoData   = null,
                    fields      = fieldsList,
                    isFavorite  = false,
                    isReceived  = false,
                    note        = null,
                    createdAt   = java.util.Date()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VicaBlack)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with Back chevron and Save Text Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Back",
                                tint = VicaWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = when (selectedType) {
                                CardType.PERSONAL -> "Personal Card"
                                CardType.BUSINESS -> "Business Card"
                                CardType.SOCIAL -> "Social Profile"
                                CardType.EVENT -> "Event Badge"
                                else -> "New Card"
                            },
                            color = VicaWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        TextButton(
                            onClick   = onSave,
                            enabled   = !isSaving && (fieldValues["fullName"] ?: fieldValues["nickname"] ?: "").isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VicaGold, strokeWidth = 2.dp)
                            } else {
                                Text("Save", color = VicaGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // Form content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Real-time Live Preview
                        item {
                            PremiumCardView(
                                card         = liveCard,
                                modifier     = Modifier.fillMaxWidth()
                            )
                        }

                        // Theme selection dots
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "CARD THEME",
                                    color = VicaGrey,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(CardTheme.entries) { theme ->
                                        val color = when (theme) {
                                            CardTheme.PINK -> ThemePink
                                            CardTheme.LIME -> ThemeLime
                                            CardTheme.SKY -> ThemeSky
                                            CardTheme.LAVENDER -> ThemeLavender
                                            CardTheme.PEACH -> ThemePeach
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width  = if (selectedTheme == theme) 3.dp else 0.dp,
                                                    color  = if (selectedTheme == theme) VicaWhite else Color.Transparent,
                                                    shape  = CircleShape
                                                )
                                                .clickable { onThemeSelected(theme) }
                                        )
                                    }
                                }
                            }
                        }

                        // Input fields
                        items(fieldDefinitions) { field ->
                            val value = fieldValues[field.key] ?: ""
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text          = field.label.uppercase(),
                                    color         = VicaGrey,
                                    style         = MaterialTheme.typography.labelSmall,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value         = value,
                                    onValueChange = { onFieldChanged(field.key, it) },
                                    placeholder   = { Text(field.placeholder, color = VicaGrey.copy(alpha = 0.5f)) },
                                    singleLine    = field.kind != FieldKind.TEXT || field.key != "bio",
                                    modifier      = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor      = VicaBorder,
                                        unfocusedBorderColor    = VicaBorder.copy(alpha = 0.5f),
                                        focusedTextColor        = VicaWhite,
                                        unfocusedTextColor      = VicaWhite,
                                        focusedContainerColor   = VicaCard.copy(alpha = 0.4f),
                                        unfocusedContainerColor = VicaCard.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = when (field.keyboard) {
                                            FieldDefinition.KeyboardType.PHONE -> KeyboardType.Phone
                                            FieldDefinition.KeyboardType.EMAIL -> KeyboardType.Email
                                            FieldDefinition.KeyboardType.URL   -> KeyboardType.Uri
                                            else -> KeyboardType.Text
                                        }
                                    )
                                )
                            }
                        }

                        // Primary Save Button matching onboarding style
                        item {
                            val isValid = (fieldValues["fullName"] ?: fieldValues["nickname"] ?: "").isNotBlank()
                            Button(
                                onClick  = onSave,
                                enabled  = !isSaving && isValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isValid) VicaGold else VicaCard,
                                    disabledContainerColor = VicaCard
                                ),
                                shape  = RoundedCornerShape(26.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VicaBlack)
                                } else {
                                    Text(
                                        "Save Card",
                                        color = if (isValid) VicaBlack else VicaGrey,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // Extra space at bottom to scroll past FloatingTabBar
                        item { Spacer(modifier = Modifier.height(140.dp)) }
                    }
                }
            }
        }
    }
}

// ─── ViCaTemplateRow ──────────────────────────────────────────────────────────
//
// iOS ref — TemplateRow.swift:
//   HStack spacing 18: icon box 56×56 | VStack title/subtitle | chevron
//   Background: white.opacity(0.04), cornerRadius 32, border white.opacity(0.06)

@Composable
fun ViCaTemplateRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(32.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Icon box — 56×56, cornerRadius 18
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(22.dp)
            )
        }

        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text          = subtitle,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Black,
                color         = Color.White.copy(alpha = 0.35f),
                letterSpacing = 1.4.sp
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.15f),
            modifier           = Modifier.size(14.dp)
        )
    }
}
