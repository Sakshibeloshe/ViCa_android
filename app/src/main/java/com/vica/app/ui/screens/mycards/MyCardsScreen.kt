package com.vica.app.ui.screens.mycards

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vica.app.data.model.CardModel
import com.vica.app.data.model.CardType
import com.vica.app.ui.components.*
import com.vica.app.ui.theme.*

@Composable
fun MyCardsScreen(
    cards: List<CardModel>,
    isLoading: Boolean = false,
    onCardClick: (CardModel) -> Unit,
    onShareCard: (CardModel) -> Unit,
    onAddCard: () -> Unit
) {
    var activeFilter by remember { mutableStateOf("All") }
    var searchQuery  by remember { mutableStateOf("") }

    val filters = listOf("All", "Personal", "Business", "Social", "Event")

    val filteredCards = remember(cards, activeFilter, searchQuery) {
        cards.filter { card ->
            val matchesFilter = when (activeFilter) {
                "Personal" -> card.type == CardType.PERSONAL
                "Business" -> card.type == CardType.BUSINESS
                "Social"   -> card.type == CardType.SOCIAL
                "Event"    -> card.type == CardType.EVENT
                else       -> true
            }
            val matchesSearch = searchQuery.isEmpty() ||
                    card.displayName.contains(searchQuery, ignoreCase = true) ||
                    (card.subtitle?.contains(searchQuery, ignoreCase = true) == true) ||
                    (card.org?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── TopNavBar (iOS-faithful)
            item {
                ViCaTopNavBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                )
            }

            // ── Header — "My Stack"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text       = "My Stack",
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.Black,
                        color      = Color.White
                    )
                    Text(
                        text       = "Your digital identity",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            // ── Filter pills
            item {
                ViCaFilterPills(
                    items      = filters,
                    selected   = activeFilter,
                    onSelected = { activeFilter = it },
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Search bar
            item {
                ViCaSearchBar(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = "Search cards",
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Content
            if (isLoading) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(ViCaRadius.card))
                            .background(CharcoalGrey.copy(alpha = 0.4f))
                    )
                }
            } else if (filteredCards.isEmpty()) {
                item { MyCardsEmptyState(onAddCard = onAddCard) }
            } else {
                items(filteredCards, key = { it.id.toString() }) { card ->
                    PremiumCardView(
                        card         = card,
                        onShareClick = { onShareCard(card) },
                        modifier     = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onCardClick(card) }
                            .animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun MyCardsEmptyState(onAddCard: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.CreditCard,
                contentDescription = null,
                tint               = FreshLime.copy(0.7f),
                modifier           = Modifier.size(40.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No Cards Yet",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Create your first card to get started",
                fontSize  = 15.sp,
                fontWeight = FontWeight.Medium,
                color     = Color.White.copy(0.35f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(FreshLime)
                .clickable { onAddCard() }
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(
                "Create Card",
                fontWeight = FontWeight.Bold,
                color      = CharcoalGrey,
                fontSize   = 15.sp
            )
        }
    }
}
