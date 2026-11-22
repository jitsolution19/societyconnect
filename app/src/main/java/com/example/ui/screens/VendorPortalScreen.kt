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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.OfferCategory
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
import com.example.data.model.VendorProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricTile
import kotlin.math.roundToInt

@Composable
fun VendorPortalScreen(
    myVendorProfile: VendorProfile?,
    vendorQuotations: List<Quotation>,
    isBusy: Boolean,
    onSaveVendorProfile: (
        businessName: String,
        category: OfferCategory,
        city: String,
        description: String
    ) -> Unit,
    onSubmitQuotation: (
        quotation: Quotation,
        quotedPrice: Double,
        availability: String,
        estimatedDelivery: String,
        proposalNotes: String,
        vendorReply: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileEditor by remember { mutableStateOf(false) }
    var quoteToSubmit by remember { mutableStateOf<Quotation?>(null) }

    val invited = remember(vendorQuotations) {
        vendorQuotations.filter { it.statusEnum() == QuotationStatus.INVITED }
    }
    val submitted = remember(vendorQuotations) {
        vendorQuotations.filter { it.statusEnum() == QuotationStatus.SUBMITTED }
    }
    val accepted = remember(vendorQuotations) {
        vendorQuotations.filter { it.statusEnum() == QuotationStatus.ACCEPTED }
    }
    val rejected = remember(vendorQuotations) {
        vendorQuotations.filter { it.statusEnum() == QuotationStatus.REJECTED }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                if (myVendorProfile == null) {
                    VendorRegistrationCard(
                        isBusy = isBusy,
                        initialProfile = null,
                        onSave = onSaveVendorProfile
                    )
                } else {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = myVendorProfile.businessName,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = myVendorProfile.businessName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${myVendorProfile.categoryEnum().label} · ${myVendorProfile.city}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showProfileEditor = true },
                                    modifier = Modifier.testTag("edit_vendor_profile_button")
                                ) {
                                    Text("Edit")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = myVendorProfile.description,
                                style = MaterialTheme.typography.bodySmall
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
                        label = "Invited",
                        value = invited.size.toString(),
                        subtitle = "New RFQs",
                        icon = Icons.Filled.RequestQuote,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        label = "Pending",
                        value = submitted.size.toString(),
                        subtitle = "Awaiting admin",
                        icon = Icons.Filled.PendingActions,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        label = "Selected",
                        value = accepted.size.toString(),
                        subtitle = "${rejected.size} closed",
                        icon = Icons.Filled.AssignmentTurnedIn,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Privacy Isolation: Vendors view only aggregated society demand (total units & member count) for explicitly shared quotation requests. Individual resident PII is never exposed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (vendorQuotations.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No quotation invitations yet",
                        subtitle = if (myVendorProfile == null) {
                            "Register your vendor business profile above so Society Admins can discover and invite you to bid on bulk offers."
                        } else {
                            "Your business is live in the SocietyConnect vendor catalog. When a Society Admin invites you to bid, it will appear here in real time."
                        }
                    )
                }
            } else {
                if (invited.isNotEmpty()) {
                    item {
                        Text(
                            text = "New Quotation Requests (${invited.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(invited, key = { it.id }) { quote ->
                        VendorQuotationCard(
                            quotation = quote,
                            onActionClick = { quoteToSubmit = quote }
                        )
                    }
                }

                if (submitted.isNotEmpty()) {
                    item {
                        Text(
                            text = "Submitted Bids (${submitted.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(submitted, key = { it.id }) { quote ->
                        VendorQuotationCard(
                            quotation = quote,
                            onActionClick = { quoteToSubmit = quote }
                        )
                    }
                }

                if (accepted.isNotEmpty() || rejected.isNotEmpty()) {
                    item {
                        Text(
                            text = "Commitments & History (${accepted.size + rejected.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(accepted + rejected, key = { it.id }) { quote ->
                        VendorQuotationCard(
                            quotation = quote,
                            onActionClick = null
                        )
                    }
                }
            }
        }
    }

    if (showProfileEditor && myVendorProfile != null) {
        AlertDialog(
            onDismissRequest = { showProfileEditor = false },
            title = { Text("Update Vendor Profile") },
            text = {
                VendorRegistrationFields(
                    initialProfile = myVendorProfile,
                    isBusy = isBusy,
                    onSave = { name, cat, city, desc ->
                        onSaveVendorProfile(name, cat, city, desc)
                        showProfileEditor = false
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProfileEditor = false }) { Text("Cancel") }
            }
        )
    }

    quoteToSubmit?.let { q ->
        SubmitQuotationDialog(
            quotation = q,
            isBusy = isBusy,
            onDismiss = { quoteToSubmit = null },
            onSubmit = { price, avail, delivery, notes, reply ->
                onSubmitQuotation(q, price, avail, delivery, notes, reply)
                quoteToSubmit = null
            }
        )
    }
}

@Composable
fun VendorCatalogScreen(
    vendors: List<VendorProfile>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Registered Vendor Directory",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Verified service providers & bulk suppliers available to society admins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vendors.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No vendors listed yet",
                        subtitle = "Vendors registered through the Vendor Hub appear here for Society Admins to invite."
                    )
                }
            } else {
                items(vendors, key = { it.id }) { vendor ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vendor.businessName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = vendor.categoryEnum().label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = vendor.city,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = vendor.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorRegistrationCard(
    initialProfile: VendorProfile?,
    isBusy: Boolean,
    onSave: (String, OfferCategory, String, String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Register Vendor Business",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Publish your service capabilities so residential societies can invite you to bid on aggregated bulk deals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            VendorRegistrationFields(
                initialProfile = initialProfile,
                isBusy = isBusy,
                onSave = onSave
            )
        }
    }
}

@Composable
private fun VendorRegistrationFields(
    initialProfile: VendorProfile?,
    isBusy: Boolean,
    onSave: (String, OfferCategory, String, String) -> Unit
) {
    var businessName by remember(initialProfile?.vendorUid) { mutableStateOf(initialProfile?.businessName ?: "") }
    var category by remember(initialProfile?.vendorUid) { mutableStateOf(initialProfile?.categoryEnum() ?: OfferCategory.GROCERY) }
    var city by remember(initialProfile?.vendorUid) { mutableStateOf(initialProfile?.city ?: "") }
    var description by remember(initialProfile?.vendorUid) { mutableStateOf(initialProfile?.description ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business / Service Name") },
            placeholder = { Text("e.g. FreshValley Dairy Co.") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vendor_name_input")
        )
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
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Operating City") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vendor_city_input")
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Service Capabilities & Quality Highlights") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vendor_desc_input")
        )
        Button(
            onClick = { onSave(businessName, category, city, description) },
            enabled = !isBusy && businessName.trim().length >= 2 && city.trim().length >= 2 && description.trim().length >= 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_vendor_profile_button")
        ) {
            Text(if (initialProfile == null) "Register in Vendor Catalog" else "Save Profile")
        }
    }
}

@Composable
private fun VendorQuotationCard(
    quotation: Quotation,
    onActionClick: (() -> Unit)?
) {
    val status = quotation.statusEnum()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (status == QuotationStatus.ACCEPTED) 1.5.dp else 1.dp,
            color = if (status == QuotationStatus.ACCEPTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quotation.societyName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == QuotationStatus.ACCEPTED) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = quotation.offerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Aggregated Demand: ${quotation.aggregatedMemberCount} members · ${quotation.aggregatedQty} units",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Society Target Price: ₹${quotation.estimatedPrice.roundToInt()} ${quotation.unitLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (quotation.quotedPrice > 0.0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your Quote: ₹${quotation.quotedPrice.roundToInt()} ${quotation.unitLabel} · ${quotation.estimatedDelivery}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (quotation.adminQuery.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Admin Query: ${quotation.adminQuery}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (onActionClick != null && (status == QuotationStatus.INVITED || status == QuotationStatus.SUBMITTED)) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("vendor_submit_quote_${quotation.id}")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (status == QuotationStatus.INVITED) "Submit Quotation" else "Update Quotation / Reply")
                }
            }
        }
    }
}

@Composable
private fun SubmitQuotationDialog(
    quotation: Quotation,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, String, String) -> Unit
) {
    var priceText by remember {
        mutableStateOf(
            if (quotation.quotedPrice > 0.0) quotation.quotedPrice.roundToInt().toString()
            else (quotation.estimatedPrice * 0.85).roundToInt().coerceAtLeast(1).toString()
        )
    }
    var availability by remember { mutableStateOf(quotation.availability.ifBlank { "Mon–Sun, 7 AM – 6 PM" }) }
    var delivery by remember { mutableStateOf(quotation.estimatedDelivery.ifBlank { "Within 48 hours of confirmation" }) }
    var notes by remember { mutableStateOf(quotation.proposalNotes) }
    var reply by remember { mutableStateOf(quotation.vendorReply) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Quotation") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${quotation.offerTitle} (${quotation.aggregatedQty} units)",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Quoted Unit Price (₹ ${quotation.unitLabel})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quote_price_input")
                )
                OutlinedTextField(
                    value = availability,
                    onValueChange = { availability = it },
                    label = { Text("Service Availability") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = delivery,
                    onValueChange = { delivery = it },
                    label = { Text("Estimated Delivery / Completion") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Proposal Terms & Inclusions") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (quotation.adminQuery.isNotBlank()) {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        label = { Text("Reply to Admin: \"${quotation.adminQuery}\"") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val price = priceText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSubmit(price, availability, delivery, notes, reply) },
                enabled = !isBusy && price > 0.0 && availability.isNotBlank() && delivery.isNotBlank(),
                modifier = Modifier.testTag("confirm_submit_quote_button")
            ) {
                Text("Send Quotation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
