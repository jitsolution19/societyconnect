package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.model.OfferStatus
import com.example.data.model.Participation
import com.example.data.model.ParticipationStage
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
import com.example.data.model.VendorProfile
import com.example.ui.components.OfferStatusPill
import com.example.ui.components.ThresholdProgressSection
import com.example.ui.components.icon
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailScreen(
    offer: Offer,
    myParticipation: Participation?,
    isSocietyAdmin: Boolean,
    offerParticipations: List<Participation>,
    offerQuotations: List<Quotation>,
    activeVendors: List<VendorProfile>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onExpressInterest: (qty: Long, option: String) -> Unit,
    onWithdrawInterest: () -> Unit,
    onConfirmFinalOrder: (qty: Long, option: String) -> Unit,
    onCancelFinalOrder: () -> Unit,
    onMoveToNegotiation: () -> Unit,
    onInviteVendor: (VendorProfile) -> Unit,
    onAskVendorQuery: (Quotation, String) -> Unit,
    onAcceptQuotationAndPublish: (Quotation, Double, String) -> Unit,
    onCloseOffer: () -> Unit
) {
    BackHandler(onBack = onBack)

    var qty by remember(offer.id, myParticipation?.id) {
        mutableLongStateOf(
            when (myParticipation?.stageEnum()) {
                ParticipationStage.CONFIRMED -> myParticipation.confirmedQty.coerceAtLeast(1L)
                ParticipationStage.INTERESTED -> myParticipation.requestedQty.coerceAtLeast(1L)
                else -> 1L
            }
        )
    }
    var preferredOption by remember(offer.id, myParticipation?.id) {
        mutableStateOf(myParticipation?.preferredOption ?: "")
    }

    var showInviteVendorDialog by remember { mutableStateOf(false) }
    var quoteToAccept by remember { mutableStateOf<Quotation?>(null) }
    var queryTargetQuote by remember { mutableStateOf<Quotation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offer Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("offer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WorkflowStepperCard(status = offer.statusEnum())
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = offer.categoryEnum().icon(),
                                        contentDescription = offer.categoryEnum().label,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = offer.categoryEnum().label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                OfferStatusPill(status = offer.statusEnum())
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = offer.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = offer.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (offer.serviceDetails.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Service / Delivery: ${offer.serviceDetails}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (offer.negotiatedPrice > 0.0) "Negotiated Society Price" else "Estimated Price",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₹${offer.effectivePrice().roundToInt()} ${offer.unitLabel}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Interest Deadline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = offer.deadlineLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            ThresholdProgressSection(offer = offer)

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "${offer.interestedCount} interested members · ${offer.totalRequestedQty} total units requested",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // MEMBER ACTION SECTION
                if (offer.statusEnum() != OfferStatus.CLOSED && offer.statusEnum() != OfferStatus.CANCELLED) {
                    item {
                        val isFinalStage = offer.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED
                        val currentStage = myParticipation?.stageEnum()
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFinalStage) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isFinalStage) 1.5.dp else 1.dp,
                                color = if (isFinalStage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                if (isFinalStage) {
                                    Text(
                                        text = "Stage 2 · Final Negotiated Offer",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Vendor: ${offer.selectedVendorName ?: "Selected Vendor"}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (!offer.finalTerms.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = offer.finalTerms,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    QuantityAndOptionControls(
                                        qty = qty,
                                        onQtyChange = { qty = it },
                                        preferredOption = preferredOption,
                                        onOptionChange = { preferredOption = it }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { onConfirmFinalOrder(qty, preferredOption) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("confirm_final_order_button")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (currentStage == ParticipationStage.CONFIRMED) "Update Confirmed Order"
                                            else "Confirm Final Order (₹${(qty * offer.effectivePrice()).roundToInt()})"
                                        )
                                    }
                                    if (currentStage == ParticipationStage.CONFIRMED) {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onCancelFinalOrder,
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("cancel_final_order_button")
                                        ) {
                                            Text("Cancel Confirmed Order")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Stage 1 · Express Community Interest",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Expressing interest is non-binding. Once the threshold is met, your Admin negotiates final vendor pricing before you confirm.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    QuantityAndOptionControls(
                                        qty = qty,
                                        onQtyChange = { qty = it },
                                        preferredOption = preferredOption,
                                        onOptionChange = { preferredOption = it }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { onExpressInterest(qty, preferredOption) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("express_interest_button")
                                    ) {
                                        Text(
                                            if (currentStage == ParticipationStage.INTERESTED) "Update My Interest"
                                            else "I'm Interested"
                                        )
                                    }
                                    if (currentStage == ParticipationStage.INTERESTED) {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onWithdrawInterest,
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("withdraw_interest_button")
                                        ) {
                                            Text("Withdraw Interest")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ADMIN WORKFLOW SECTION
                if (isSocietyAdmin) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Society Admin · Demand & Vendor Control",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(10.dp))

                                val activeInterested = offerParticipations.filter { it.stageEnum() == ParticipationStage.INTERESTED }
                                val activeConfirmed = offerParticipations.filter { it.stageEnum() == ParticipationStage.CONFIRMED }
                                val estOrderValue = (offer.totalRequestedQty * offer.effectivePrice()).roundToInt()

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Consolidated Demand Report",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Interested: ${offer.interestedCount} members (${offer.totalRequestedQty} units)\n" +
                                                "Confirmed: ${offer.confirmedCount} members (${offer.confirmedQty} units)\n" +
                                                "Estimated Order Value: ₹$estOrderValue",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        val breakdown = if (activeConfirmed.isNotEmpty()) activeConfirmed else activeInterested
                                        if (breakdown.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            HorizontalDivider()
                                            Spacer(Modifier.height(8.dp))
                                            breakdown.take(8).forEach { p ->
                                                val pQty = if (p.stageEnum() == ParticipationStage.CONFIRMED) p.confirmedQty else p.requestedQty
                                                Text(
                                                    text = "• Flat ${p.flatNumber} (${p.memberName}) — Qty: $pQty ${if (p.preferredOption.isNotBlank()) "· ${p.preferredOption}" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                if (offer.statusEnum() == OfferStatus.COLLECTING_INTEREST ||
                                    offer.statusEnum() == OfferStatus.THRESHOLD_REACHED
                                ) {
                                    Button(
                                        onClick = onMoveToNegotiation,
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("start_vendor_negotiation_button")
                                    ) {
                                        Icon(Icons.Filled.Handshake, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Move to Vendor Negotiation")
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                OutlinedButton(
                                    onClick = { showInviteVendorDialog = true },
                                    enabled = !isBusy && offer.statusEnum() != OfferStatus.CLOSED,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("invite_vendor_button")
                                ) {
                                    Icon(Icons.Filled.Storefront, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Request Quotation from Vendor")
                                }

                                if (offerQuotations.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Vendor Quotations (${offerQuotations.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    offerQuotations.forEach { quote ->
                                        QuotationComparisonCard(
                                            quotation = quote,
                                            isOfferPublished = offer.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED,
                                            onAskQuery = { queryTargetQuote = quote },
                                            onSelectVendor = { quoteToAccept = quote }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                if (offer.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED) {
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(
                                        onClick = onCloseOffer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("close_offer_button")
                                    ) {
                                        Text("Close Campaign")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteVendorDialog) {
        InviteVendorDialog(
            vendors = activeVendors,
            alreadyInvitedUids = offerQuotations.map { it.vendorUid }.toSet(),
            onDismiss = { showInviteVendorDialog = false },
            onInvite = { vendor ->
                onInviteVendor(vendor)
                showInviteVendorDialog = false
            }
        )
    }

    quoteToAccept?.let { selected ->
        AcceptQuotationDialog(
            quotation = selected,
            onDismiss = { quoteToAccept = null },
            onConfirm = { price, terms ->
                onAcceptQuotationAndPublish(selected, price, terms)
                quoteToAccept = null
            }
        )
    }

    queryTargetQuote?.let { target ->
        AskVendorQueryDialog(
            vendorName = target.vendorName,
            onDismiss = { queryTargetQuote = null },
            onSend = { q ->
                onAskVendorQuery(target, q)
                queryTargetQuote = null
            }
        )
    }
}

@Composable
private fun WorkflowStepperCard(status: OfferStatus) {
    val activeStep = when (status) {
        OfferStatus.COLLECTING_INTEREST -> 0
        OfferStatus.THRESHOLD_REACHED -> 1
        OfferStatus.VENDOR_NEGOTIATION -> 2
        OfferStatus.FINAL_OFFER_PUBLISHED, OfferStatus.CLOSED -> 3
        OfferStatus.CANCELLED -> 0
    }
    val steps = listOf("Interest", "Threshold", "Vendors", "Final Deal")
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, label ->
                val done = index <= activeStep
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityAndOptionControls(
    qty: Long,
    onQtyChange: (Long) -> Unit,
    preferredOption: String,
    onOptionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Quantity / Units", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (qty > 1L) onQtyChange(qty - 1L) },
                    modifier = Modifier.testTag("qty_decrease_button")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                }
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = { if (qty < 1000L) onQtyChange(qty + 1L) },
                    modifier = Modifier.testTag("qty_increase_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                }
            }
        }
        OutlinedTextField(
            value = preferredOption,
            onValueChange = onOptionChange,
            label = { Text("Preferred Service Slot / Package Option (Optional)") },
            placeholder = { Text("e.g. Weekend Morning / 5kg Pack") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preferred_option_input")
        )
    }
}

@Composable
private fun QuotationComparisonCard(
    quotation: Quotation,
    isOfferPublished: Boolean,
    onAskQuery: () -> Unit,
    onSelectVendor: () -> Unit
) {
    val status = quotation.statusEnum()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quotation.vendorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            if (status == QuotationStatus.INVITED) {
                Text(
                    text = "Waiting for vendor quotation submission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Quoted: ₹${quotation.quotedPrice.roundToInt()} ${quotation.unitLabel} · ${quotation.estimatedDelivery}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (quotation.availability.isNotBlank()) {
                    Text(
                        text = "Availability: ${quotation.availability}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (quotation.proposalNotes.isNotBlank()) {
                    Text(
                        text = quotation.proposalNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (quotation.adminQuery.isNotBlank()) {
                    Text(
                        text = "Admin Query: ${quotation.adminQuery}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (quotation.vendorReply.isNotBlank()) {
                    Text(
                        text = "Vendor Reply: ${quotation.vendorReply}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!isOfferPublished && status != QuotationStatus.REJECTED) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAskQuery,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ask Query")
                    }
                    if (status == QuotationStatus.SUBMITTED) {
                        Button(
                            onClick = onSelectVendor,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("select_vendor_${quotation.id}")
                        ) {
                            Text("Accept & Publish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteVendorDialog(
    vendors: List<VendorProfile>,
    alreadyInvitedUids: Set<String>,
    onDismiss: () -> Unit,
    onInvite: (VendorProfile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Registered Vendor") },
        text = {
            if (vendors.isEmpty()) {
                Text("No vendors are registered in the catalog yet. Switch to the Vendor workspace to register a vendor profile and test the full quotation loop.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vendors.forEach { vendor ->
                        val invited = vendor.vendorUid in alreadyInvitedUids
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vendor.businessName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${vendor.categoryEnum().label} · ${vendor.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onInvite(vendor) },
                                    enabled = !invited
                                ) {
                                    Text(if (invited) "Invited" else "Invite")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun AcceptQuotationDialog(
    quotation: Quotation,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var priceText by remember { mutableStateOf(quotation.quotedPrice.roundToInt().toString()) }
    var terms by remember { mutableStateOf("Final negotiated society rate. Free cancellation within 24 hours of confirmation.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Final Deal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select ${quotation.vendorName} and notify society members to confirm Stage 2 orders.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Final Negotiated Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = terms,
                    onValueChange = { terms = it },
                    label = { Text("Final Terms & Cancellation Policy") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: quotation.quotedPrice
                    onConfirm(p, terms)
                }
            ) {
                Text("Publish Deal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AskVendorQueryDialog(
    vendorName: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Query $vendorName") },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Your question on pricing, delivery, or terms") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSend(query) }, enabled = query.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
