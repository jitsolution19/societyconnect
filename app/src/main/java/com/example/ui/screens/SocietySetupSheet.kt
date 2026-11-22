package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Society
import com.example.data.model.UserRole

@Composable
fun SocietyOnboardingPane(
    societies: List<Society>,
    isBusy: Boolean,
    onCreateSociety: (name: String, city: String, address: String, totalUnits: Long, flatNumber: String) -> Unit,
    onJoinSociety: (society: Society, flatNumber: String) -> Unit,
    onSwitchToVendor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(if (societies.isEmpty()) 1 else 0) }
    var inviteCodeQuery by remember { mutableStateOf("") }
    var joinFlatNumber by remember { mutableStateOf("A-101") }
    var selectedSociety by remember(societies) { mutableStateOf(societies.firstOrNull()) }

    var socName by remember { mutableStateOf("") }
    var socCity by remember { mutableStateOf("") }
    var socAddress by remember { mutableStateOf("") }
    var socUnits by remember { mutableStateOf("120") }
    var adminFlat by remember { mutableStateOf("Admin Office") }

    val filteredSocieties = remember(societies, inviteCodeQuery) {
        val q = inviteCodeQuery.trim()
        if (q.isBlank()) societies
        else societies.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.inviteCode.contains(q, ignoreCase = true) ||
                it.city.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 580.dp)) {
            Text(
                text = "Connect Your Community",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Join your residential society as a Member, create a new society as Admin, or open the Vendor Portal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_join_society")
                ) {
                    Text("Join Society")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_create_society")
                ) {
                    Text("Create Society")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = inviteCodeQuery,
                    onValueChange = { inviteCodeQuery = it },
                    label = { Text("Search society name, city, or invite code") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_society_input")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = joinFlatNumber,
                    onValueChange = { joinFlatNumber = it },
                    label = { Text("Your Flat / Apartment Number (e.g. B-402)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_flat_input")
                )
                Spacer(Modifier.height(16.dp))

                if (filteredSocieties.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No matching societies found yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Create Society' to register your residential community and generate an invite code for neighbors.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    filteredSocieties.forEach { society ->
                        val isSelected = selectedSociety?.id == society.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("society_option_${society.id}")
                                .clickable { selectedSociety = society }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Apartment,
                                    contentDescription = society.name,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = society.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${society.city} · ${society.memberCount} members · Code: ${society.inviteCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            selectedSociety?.let { onJoinSociety(it, joinFlatNumber) }
                        },
                        enabled = !isBusy && selectedSociety != null && joinFlatNumber.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_join_society_button")
                    ) {
                        Text("Join ${selectedSociety?.name ?: "Selected Society"}")
                    }
                }
            } else {
                OutlinedTextField(
                    value = socName,
                    onValueChange = { socName = it },
                    label = { Text("Society / Apartment Name") },
                    placeholder = { Text("e.g. Greenwood Heights") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_society_name_input")
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = socCity,
                        onValueChange = { socCity = it },
                        label = { Text("City") },
                        placeholder = { Text("Bengaluru") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_society_city_input")
                    )
                    OutlinedTextField(
                        value = socUnits,
                        onValueChange = { socUnits = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Flats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("create_society_units_input")
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = socAddress,
                    onValueChange = { socAddress = it },
                    label = { Text("Neighborhood / Street Address") },
                    placeholder = { Text("Whitefield Main Road") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_society_address_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = adminFlat,
                    onValueChange = { adminFlat = it },
                    label = { Text("Your Flat / Office Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_society_flat_input")
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val units = socUnits.toLongOrNull() ?: 100L
                        onCreateSociety(socName, socCity, socAddress, units, adminFlat)
                    },
                    enabled = !isBusy && socName.trim().length >= 2 && socCity.trim().length >= 2 && socAddress.trim().length >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_create_society_button")
                ) {
                    Icon(Icons.Filled.LocationCity, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Society as Admin")
                }
            }

            Spacer(Modifier.height(24.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Vendor portal",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Are you a Service Vendor?",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Register your business to receive bulk quotation requests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onSwitchToVendor,
                        modifier = Modifier.testTag("switch_to_vendor_onboarding")
                    ) {
                        Text("Vendor")
                    }
                }
            }
        }
    }
}
