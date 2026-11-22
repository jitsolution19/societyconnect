package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Offer
import com.example.data.model.OfferCategory
import com.example.data.model.OfferStatus
import com.example.data.model.Participation
import com.example.data.model.ParticipationStage
import com.example.data.model.Society
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricTile
import com.example.ui.components.OfferCard
import kotlin.math.roundToInt

@Composable
fun MemberDealsFeedScreen(
    society: Society,
    userProfile: UserProfile?,
    offers: List<Offer>,
    myParticipations: List<Participation>,
    isSocietyAdmin: Boolean,
    onOfferClick: (Offer) -> Unit,
    onSwitchToAdminCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<OfferCategory?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String>("ALL") }

    val participationByOffer = remember(myParticipations) {
        myParticipations.associateBy { it.offerId }
    }

    val filteredOffers = remember(offers, selectedCategory, selectedStatusFilter) {
        offers.filter { offer ->
            val catOk = selectedCategory == null || offer.categoryEnum() == selectedCategory
            val statusOk = when (selectedStatusFilter) {
                "COLLECTING" -> offer.statusEnum() == OfferStatus.COLLECTING_INTEREST
                "THRESHOLD" -> offer.statusEnum() == OfferStatus.THRESHOLD_REACHED || offer.statusEnum() == OfferStatus.VENDOR_NEGOTIATION
                "FINAL" -> offer.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED
                else -> offer.statusEnum() != OfferStatus.CANCELLED
            }
            catOk && statusOk
        }
    }

    val collectingCount = offers.count { it.statusEnum() == OfferStatus.COLLECTING_INTEREST }
    val thresholdMetCount = offers.count {
        it.statusEnum() == OfferStatus.THRESHOLD_REACHED || it.statusEnum() == OfferStatus.VENDOR_NEGOTIATION
    }
    val finalDealsCount = offers.count { it.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_community),
                        contentDescription = society.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xD90E2B22), Color(0x660E2B22))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Apartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "${society.city} · Flat ${userProfile?.flatNumber ?: "Resident"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = society.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${society.memberCount} neighbors connected · Bulk deals & society services",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricTile(
                        label = "Collecting",
                        value = collectingCount.toString(),
                        subtitle = "Open interest",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        label = "Threshold Met",
                        value = thresholdMetCount.toString(),
                        subtitle = "With vendors",
                        icon = Icons.Filled.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        label = "Final Deals",
                        value = finalDealsCount.toString(),
                        subtitle = "Ready to order",
                        icon = Icons.Filled.LocalOffer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val stages = listOf(
                            "ALL" to "All Deals",
                            "COLLECTING" to "Collecting Interest",
                            "THRESHOLD" to "Threshold / Quotes",
                            "FINAL" to "Final Negotiated"
                        )
                        stages.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedStatusFilter == key,
                                onClick = { selectedStatusFilter = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("stage_filter_$key")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Categories") },
                            modifier = Modifier.testTag("cat_filter_ALL")
                        )
                        OfferCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat.label) },
                                modifier = Modifier.testTag("cat_filter_${cat.name}")
                            )
                        }
                    }
                }
            }

            if (filteredOffers.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (offers.isEmpty()) "No society deals published yet" else "No deals match these filters",
                        subtitle = if (isSocietyAdmin) {
                            "Publish your society's first bulk grocery or service campaign from the Admin Hub so residents can express interest."
                        } else {
                            "Check back soon or ask your Society Admin (${society.adminName}) to launch a bulk deal for ${society.name}."
                        },
                        actionLabel = if (isSocietyAdmin) "Publish First Offer" else null,
                        onAction = if (isSocietyAdmin) onSwitchToAdminCreate else null,
                        actionTestTag = "empty_deals_create_offer"
                    )
                }
            } else {
                items(filteredOffers, key = { it.id }) { offer ->
                    OfferCard(
                        offer = offer,
                        myParticipation = participationByOffer[offer.id],
                        onClick = { onOfferClick(offer) }
                    )
                }
            }
        }
    }
}

@Composable
fun MemberActivityScreen(
    offers: List<Offer>,
    myParticipations: List<Participation>,
    onOfferClick: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    val offersById = remember(offers) { offers.associateBy { it.id } }
    val confirmed = remember(myParticipations) {
        myParticipations.filter { it.stageEnum() == ParticipationStage.CONFIRMED }
    }
    val interested = remember(myParticipations) {
        myParticipations.filter { it.stageEnum() == ParticipationStage.INTERESTED }
    }
    val history = remember(myParticipations) {
        myParticipations.filter {
            it.stageEnum() == ParticipationStage.WITHDRAWN || it.stageEnum() == ParticipationStage.CANCELLED
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "My Participation & Orders",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Interest collection (Stage 1) stays separate from confirmed final orders (Stage 2) until negotiated vendor terms are published.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (myParticipations.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No activity yet",
                        subtitle = "Browse active bulk deals in your society and tap 'Express Interest' to start saving with your neighbors."
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = "Stage 2 · Confirmed Orders (${confirmed.size})",
                        subtitle = "Committed final orders at negotiated society rates"
                    )
                }
                if (confirmed.isEmpty()) {
                    item {
                        MutedInfoCard("No confirmed final orders yet. Once a vendor is selected and final terms are published, confirm your units on the deal page.")
                    }
                } else {
                    items(confirmed, key = { "conf_${it.id}" }) { part ->
                        ParticipationCard(
                            participation = part,
                            offer = offersById[part.offerId],
                            onClick = { offersById[part.offerId]?.let(onOfferClick) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "Stage 1 · Interested Offers (${interested.size})",
                        subtitle = "Non-binding interest helping reach community thresholds"
                    )
                }
                if (interested.isEmpty()) {
                    item {
                        MutedInfoCard("No active Stage 1 interest entries.")
                    }
                } else {
                    items(interested, key = { "int_${it.id}" }) { part ->
                        ParticipationCard(
                            participation = part,
                            offer = offersById[part.offerId],
                            onClick = { offersById[part.offerId]?.let(onOfferClick) }
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionHeader(
                            title = "Participation History (${history.size})",
                            subtitle = "Withdrawn interests and cancelled orders"
                        )
                    }
                    items(history, key = { "hist_${it.id}" }) { part ->
                        ParticipationCard(
                            participation = part,
                            offer = offersById[part.offerId],
                            onClick = { offersById[part.offerId]?.let(onOfferClick) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MutedInfoCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun ParticipationCard(
    participation: Participation,
    offer: Offer?,
    onClick: () -> Unit
) {
    val stage = participation.stageEnum()
    val qty = if (stage == ParticipationStage.CONFIRMED) participation.confirmedQty else participation.requestedQty
    val totalVal = (qty * participation.unitPrice).roundToInt()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("participation_card_${participation.id}")
            .clickable(enabled = offer != null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (stage == ParticipationStage.CONFIRMED) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = stage.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participation.offerTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stage.label} · Flat ${participation.flatNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (participation.preferredOption.isNotBlank()) {
                    Text(
                        text = "Option: ${participation.preferredOption}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Qty: $qty",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (totalVal > 0) {
                    Text(
                        text = "₹$totalVal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
