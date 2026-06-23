package com.vica.app.ui.screens.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.vica.app.data.model.CardModel
import com.vica.app.data.model.CardType
import com.vica.app.ui.components.*
import com.vica.app.ui.theme.*

// ─── InboxScreen ───────────────────────────────────────────────────────────────
//
// iOS ref — InboxView.swift:
//   ZStack obsidianBlack
//   VStack: TopNavBar | "Connections" header | FilterPills | SearchBar
//   Card list with CardFrontView previews
//   Empty state: person.slash icon + message

@Composable
fun InboxScreen(
    cards: List<CardModel>
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Personal", "Business", "Social", "Event")

    val filteredCards = remember(cards, filter, search) {
        val typeFiltered = when (filter) {
            "Personal" -> cards.filter { it.type == CardType.PERSONAL }
            "Business" -> cards.filter { it.type == CardType.BUSINESS }
            "Social"   -> cards.filter { it.type == CardType.SOCIAL   }
            "Event"    -> cards.filter { it.type == CardType.EVENT     }
            else       -> cards
        }
        if (search.isEmpty()) typeFiltered
        else typeFiltered.filter { it.displayName.contains(search, ignoreCase = true) }
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
            // ── TopNavBar
            item {
                ViCaTopNavBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                )
            }

            // ── "Connections" header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 4.dp)
                ) {
                    Text(
                        text       = "Connections",
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.Black,
                        color      = Color.White
                    )
                    Text(
                        text       = "Cards people shared with you",
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
                    selected   = filter,
                    onSelected = { filter = it },
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Search bar
            item {
                ViCaSearchBar(
                    value         = search,
                    onValueChange = { search = it },
                    placeholder   = "Search connections",
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Empty state
            if (filteredCards.isEmpty()) {
                item { InboxEmptyState() }
            }

            // ── Card list
            items(filteredCards, key = { it.id.toString() }) { card ->
                InboxCardRow(
                    card     = card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp)
                )
            }
        }
    }
}

// ─── InboxCardRow ─────────────────────────────────────────────────────────────

@Composable
private fun InboxCardRow(
    card: CardModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Card visual
        PremiumCardView(
            card     = card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(14.dp))

        // Name + type row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = card.displayName,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text          = card.type.displayName.uppercase(),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.8.sp
                )
            }
            Icon(
                imageVector        = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favourite",
                tint               = if (card.isFavorite) Color(0xFFFFD700) else Color.White.copy(0.15f),
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun InboxEmptyState() {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                tint               = SkyBlue.copy(0.7f),
                modifier           = Modifier.size(40.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No Connections Yet",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Cards shared with you will\nappear here",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White.copy(0.35f),
                textAlign  = TextAlign.Center
            )
        }
    }
}
