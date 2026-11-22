package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Offer
import com.example.data.model.OfferCategory
import com.example.data.model.OfferStatus
import com.example.data.model.Participation
import com.example.data.model.ParticipationStage
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
import com.example.data.model.Society
import com.example.data.model.SocietyMember
import com.example.data.model.ThresholdType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricTile
import com.example.ui.components.OfferCard
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    society: Society,
    isCurrentUserAdmin: Boolean,
    offers: List<Offer>,
    members: List<SocietyMember>,
    participations: List<Participation>,
    quotations: List<Quotation>,
    isBusy: Boolean,
    openCreateInitially: Boolean = false,
    onConsumeOpenCreate: () -> Unit = {},
    onCreateOffer: (
        title: String,
        description: String,
        category: OfferCategory,
        estimatedPrice: Double,
        unitLabel: String,
        minThreshold: Long,
        thresholdType: ThresholdType,
        maxLimit: Long,
        deadlineLabel: String,
        serviceDetails: String
    ) -> Unit,
    onOfferClick: (Offer) -> Unit,
    onCreateNewSociety: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateOfferDialog by remember(openCreateInitially) {
        mutableStateOf(openCreateInitially)
    }

    if (openCreateInitially) {
        onConsumeOpenCreate()
    }

    if (!isCurrentUserAdmin) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                EmptyStateCard(
                    title = "Admin Access Scoped to Society Admin",
                    subtitle = "You joined ${society.name} as a Member (Admin is ${society.adminName}). To manage campaigns as an Admin, create your own residential society.",
                    actionLabel = "Create a Society as Admin",
                    onAction = onCreateNewSociety,
                    actionTestTag = "admin_create_own_society_button"
                )
            }
        }
        return
    }

    val activeOffersCount = offers.count {
        it.statusEnum() != OfferStatus.CLOSED && it.statusEnum() != OfferStatus.CANCELLED
    }
    val thresholdMetCount = offers.count { it.statusEnum() == OfferStatus.THRESHOLD_REACHED }
    val pendingQuotesCount = quotations.count {
        it.statusEnum() == QuotationStatus.INVITED || it.statusEnum() == QuotationStatus.SUBMITTED
    }
    val confirmedOrdersCount = participations.count { it.stageEnum() == ParticipationStage.CONFIRMED }
    val cancelledCount = participations.count {
        it.stageEnum() == ParticipationStage.CANCELLED || it.stageEnum() == ParticipationStage.WITHDRAWN
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateOfferDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("admin_create_offer_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Offer")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${society.name} · Admin Hub",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${society.city} · ${society.totalUnits} total flats",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VpnKey,
                                            contentDescription = "Invite Code",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = society.inviteCode,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricTile(
                                label = "Members",
                                value = maxOf(society.memberCount, members.size.toLong()).toString(),
                                subtitle = "Registered residents",
                                icon = Icons.Filled.Groups,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                label = "Active Offers",
                                value = activeOffersCount.toString(),
                                subtitle = "$thresholdMetCount threshold met",
                                icon = Icons.Filled.Analytics,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricTile(
                                label = "Vendor Quotes",
                                value = pendingQuotesCount.toString(),
                                subtitle = "Pending / submitted",
                                icon = Icons.Filled.RequestQuote,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                label = "Confirmed",
                                value = confirmedOrdersCount.toString(),
                                subtitle = "$cancelledCount withdrawn/cancelled",
                                icon = Icons.Filled.ShoppingCartCheckout,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (thresholdMetCount > 0) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Action needed",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "$thresholdMetCount offer(s) reached minimum participation! Open an offer below to invite vendors and negotiate pricing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Society Campaigns (${offers.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showCreateOfferDialog = true },
                            modifier = Modifier.testTag("admin_inline_create_offer")
                        ) {
                            Text("+ New Offer")
                        }
                    }
                }

                if (offers.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Launch Your First Community Deal",
                            subtitle = "Create a bulk grocery or service offer, set a participation threshold, and collect resident interest.",
                            actionLabel = "Create Offer",
                            onAction = { showCreateOfferDialog = true },
                            actionTestTag = "admin_empty_create_offer"
                        )
                    }
                } else {
                    items(offers, key = { it.id }) { offer ->
                        OfferCard(
                            offer = offer,
                            myParticipation = null,
                            onClick = { onOfferClick(offer) }
                        )
                    }
                }

                if (members.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Registered Society Members (${members.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                members.take(10).forEach { member ->
                                    Text(
                                        text = "• Flat ${member.flatNumber} — ${member.displayName} (${member.role})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateOfferDialog) {
        CreateOfferDialog(
            isBusy = isBusy,
            onDismiss = { showCreateOfferDialog = false },
            onSubmit = { title, desc, cat, price, unit, threshold, type, maxLimit, deadline, details ->
                onCreateOffer(title, desc, cat, price, unit, threshold, type, maxLimit, deadline, details)
                showCreateOfferDialog = false
            }
        )
    }
}

private data class OfferTemplate(
    val chipLabel: String,
    val title: String,
    val description: String,
    val category: OfferCategory,
    val price: String,
    val unit: String,
    val threshold: String,
    val thresholdType: ThresholdType,
    val deadline: String,
    val details: String
)

@Composable
private fun CreateOfferDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        category: OfferCategory,
        estimatedPrice: Double,
        unitLabel: String,
        minThreshold: Long,
        thresholdType: ThresholdType,
        maxLimit: Long,
        deadlineLabel: String,
        serviceDetails: String
    ) -> Unit
) {
    val templates = remember {
        listOf(
            OfferTemplate(
                chipLabel = "A2 Milk",
                title = "Farm-Fresh A2 Cow Milk Subscription",
                description = "Daily morning glass-bottle A2 milk supply for society residents at bulk group pricing.",
                category = OfferCategory.DAILY_ESSENTIALS,
                price = "75",
                unit = "per 1L bottle",
                threshold = "25",
                thresholdType = ThresholdType.MEMBERS,
                deadline = "Sunday 8 PM",
                details = "Doorstep delivery between 6:00 AM - 7:30 AM"
            ),
            OfferTemplate(
                chipLabel = "Deep Clean",
                title = "2BHK / 3BHK Festive Deep Cleaning",
                description = "Complete kitchen degreasing, bathroom descaling, and balcony pressure wash by trained crew.",
                category = OfferCategory.CLEANING,
                price = "2400",
                unit = "per 2BHK",
                threshold = "12",
                thresholdType = ThresholdType.MEMBERS,
                deadline = "Friday 6 PM",
                details = "Weekend slots available · Eco-safe supplies included"
            ),
            OfferTemplate(
                chipLabel = "AC Service",
                title = "Split AC Foam-Jet Servicing Bundle",
                description = "Pre-summer foam-jet indoor & outdoor coil wash, filter clean, and gas pressure check.",
                category = OfferCategory.APPLIANCE,
                price = "550",
                unit = "per AC unit",
                threshold = "20",
                thresholdType = ThresholdType.QUANTITY,
                deadline = "Wednesday 9 PM",
                details = "30-day service warranty included"
            ),
            OfferTemplate(
                chipLabel = "Bulk Rice",
                title = "Aged Sona Masoori Rice (25kg Sack)",
                description = "Direct mill-to-society bulk order of 12-month aged Sona Masoori rice.",
                category = OfferCategory.GROCERY,
                price = "1380",
                unit = "per 25kg bag",
                threshold = "30",
                thresholdType = ThresholdType.QUANTITY,
                deadline = "Saturday 7 PM",
                details = "Weekend lobby distribution by tower"
            )
        )
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OfferCategory.GROCERY) }
    var priceText by remember { mutableStateOf("") }
    var unitLabel by remember { mutableStateOf("per unit") }
    var thresholdText by remember { mutableStateOf("15") }
    var thresholdType by remember { mutableStateOf(ThresholdType.MEMBERS) }
    var maxLimitText by remember { mutableStateOf("0") }
    var deadlineLabel by remember { mutableStateOf("This Sunday") }
    var serviceDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Community Offer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Quick-fill template:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { tpl ->
                        SuggestionChip(
                            onClick = {
                                title = tpl.title
                                description = tpl.description
                                category = tpl.category
                                priceText = tpl.price
                                unitLabel = tpl.unit
                                thresholdText = tpl.threshold
                                thresholdType = tpl.thresholdType
                                deadlineLabel = tpl.deadline
                                serviceDetails = tpl.details
                            },
                            label = { Text(tpl.chipLabel) },
                            modifier = Modifier.testTag("tpl_${tpl.chipLabel}")
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Offer Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offer_desc_input")
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OfferCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Est. Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("offer_price_input")
                    )
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text("Unit Label") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("offer_unit_input")
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThresholdType.entries.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = thresholdType == type,
                            onClick = { thresholdType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = ThresholdType.entries.size)
                        ) {
                            Text(if (type == ThresholdType.MEMBERS) "By Members" else "By Quantity")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min Threshold") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("offer_threshold_input")
                    )
                    OutlinedTextField(
                        value = maxLimitText,
                        onValueChange = { maxLimitText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max Limit (0=∞)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = deadlineLabel,
                    onValueChange = { deadlineLabel = it },
                    label = { Text("Interest Collection Deadline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serviceDetails,
                    onValueChange = { serviceDetails = it },
                    label = { Text("Service / Delivery Details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
            val parsedThreshold = thresholdText.toLongOrNull() ?: 0L
            Button(
                onClick = {
                    onSubmit(
                        title,
                        description,
                        category,
                        parsedPrice,
                        unitLabel,
                        parsedThreshold,
                        thresholdType,
                        maxLimitText.toLongOrNull() ?: 0L,
                        deadlineLabel,
                        serviceDetails
                    )
                },
                enabled = !isBusy && title.trim().length >= 2 && description.trim().length >= 2 && parsedPrice > 0.0 && parsedThreshold >= 1L,
                modifier = Modifier.testTag("submit_create_offer_button")
            ) {
                Text("Publish Offer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
