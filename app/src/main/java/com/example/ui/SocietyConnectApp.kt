package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.UserRole
import com.example.data.remote.SocietyRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AlertsAndAccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MemberActivityScreen
import com.example.ui.screens.MemberDealsFeedScreen
import com.example.ui.screens.OfferDetailScreen
import com.example.ui.screens.SocietyOnboardingPane
import com.example.ui.screens.VendorCatalogScreen
import com.example.ui.screens.VendorPortalScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_OFFER_DETAIL = "offer_detail"
const val ROUTE_SOCIETY_PICKER = "society_picker"

private data class NavTab(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives navigation */ })
    } else {
        key(user.uid) {
            AuthenticatedShell(currentUserId = user.uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(currentUserId: String) {
    val viewModel: SocietyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SocietyViewModel(SocietyRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val societiesState by viewModel.discoverableSocietiesState.collectAsStateWithLifecycle()
    val activeSocietyState by viewModel.activeSocietyState.collectAsStateWithLifecycle()
    val offersState by viewModel.societyOffersState.collectAsStateWithLifecycle()
    val membersState by viewModel.societyMembersState.collectAsStateWithLifecycle()
    val myParticipationsState by viewModel.myParticipationsState.collectAsStateWithLifecycle()
    val societyParticipationsState by viewModel.societyParticipationsState.collectAsStateWithLifecycle()
    val activeVendorsState by viewModel.activeVendorsState.collectAsStateWithLifecycle()
    val myVendorState by viewModel.myVendorProfileState.collectAsStateWithLifecycle()
    val vendorQuotesState by viewModel.vendorQuotationsState.collectAsStateWithLifecycle()
    val societyQuotesState by viewModel.societyQuotationsState.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val userProfile = (profileState as? UiState.Success)?.data
    val societies = (societiesState as? UiState.Success)?.data.orEmpty()
    val activeSociety = (activeSocietyState as? UiState.Success)?.data
    val offers = (offersState as? UiState.Success)?.data.orEmpty()
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val myParticipations = (myParticipationsState as? UiState.Success)?.data.orEmpty()
    val societyParticipations = (societyParticipationsState as? UiState.Success)?.data.orEmpty()
    val activeVendors = (activeVendorsState as? UiState.Success)?.data.orEmpty()
    val myVendorProfile = (myVendorState as? UiState.Success)?.data
    val vendorQuotations = (vendorQuotesState as? UiState.Success)?.data.orEmpty()
    val societyQuotations = (societyQuotesState as? UiState.Success)?.data.orEmpty()

    val activeRole = userProfile?.roleEnum() ?: UserRole.MEMBER
    var selectedTabIndex by remember(activeRole) { mutableIntStateOf(0) }
    var selectedOfferId by remember { mutableStateOf<String?>(null) }
    var showSocietySwitcher by remember { mutableStateOf(false) }
    var openAdminCreateOnNavigate by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    val selectedOffer = remember(offers, selectedOfferId) {
        offers.firstOrNull { it.id == selectedOfferId }
    }

    if (selectedOffer != null) {
        val myPart = myParticipations.firstOrNull { it.offerId == selectedOffer.id }
        val offerParts = societyParticipations.filter { it.offerId == selectedOffer.id }
        val offerQuotes = societyQuotations.filter { it.offerId == selectedOffer.id }
        val isAdmin = activeSociety?.adminUid == currentUserId

        OfferDetailScreen(
            offer = selectedOffer,
            myParticipation = myPart,
            isSocietyAdmin = isAdmin,
            offerParticipations = offerParts,
            offerQuotations = offerQuotes,
            activeVendors = activeVendors,
            isBusy = isBusy,
            onBack = { selectedOfferId = null },
            onExpressInterest = { qty, opt ->
                viewModel.expressInterest(selectedOffer, qty, opt, myPart)
            },
            onWithdrawInterest = {
                if (myPart != null) viewModel.withdrawInterest(selectedOffer, myPart)
            },
            onConfirmFinalOrder = { qty, opt ->
                viewModel.confirmFinalOrder(selectedOffer, qty, opt, myPart)
            },
            onCancelFinalOrder = {
                if (myPart != null) viewModel.cancelFinalOrder(selectedOffer, myPart)
            },
            onMoveToNegotiation = { viewModel.moveOfferToNegotiation(selectedOffer) },
            onInviteVendor = { vendor -> viewModel.inviteVendor(selectedOffer, vendor) },
            onAskVendorQuery = { quote, q -> viewModel.askVendorQuery(quote, q) },
            onAcceptQuotationAndPublish = { quote, price, terms ->
                viewModel.acceptQuotationAndPublishFinalOffer(
                    offer = selectedOffer,
                    selectedQuotation = quote,
                    otherQuotations = offerQuotes,
                    finalNegotiatedPrice = price,
                    finalTerms = terms
                )
            },
            onCloseOffer = { viewModel.closeOffer(selectedOffer) }
        )
        return
    }

    if (showSocietySwitcher) {
        BackHandler { showSocietySwitcher = false }
    }

    val tabs = remember(activeRole, notifications.size) {
        when (activeRole) {
            UserRole.MEMBER -> listOf(
                NavTab("deals", "Deals", Icons.Filled.LocalOffer, Icons.Outlined.LocalOffer),
                NavTab("orders", "My Orders", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
                NavTab("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, notifications.size)
            )
            UserRole.ADMIN -> listOf(
                NavTab("admin", "Admin Hub", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
                NavTab("deals", "Deals", Icons.Filled.LocalOffer, Icons.Outlined.LocalOffer),
                NavTab("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, notifications.size)
            )
            UserRole.VENDOR -> listOf(
                NavTab("vendor", "Vendor Hub", Icons.Filled.Storefront, Icons.Outlined.Storefront),
                NavTab("catalog", "Catalog", Icons.Filled.LocalOffer, Icons.Outlined.LocalOffer),
                NavTab("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, notifications.size)
            )
        }
    }

    val safeTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (activeRole) {
                                    UserRole.VENDOR -> myVendorProfile?.businessName ?: "Vendor Portal"
                                    else -> activeSociety?.name ?: "Select your society"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        Box {
                            AssistChip(
                                onClick = { showRoleMenu = true },
                                label = { Text(activeRole.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = "Switch Role",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("top_role_switcher_chip")
                            )
                            DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                UserRole.entries.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text("${role.label} View") },
                                        onClick = {
                                            showRoleMenu = false
                                            showSocietySwitcher = false
                                            viewModel.switchRole(role)
                                        },
                                        modifier = Modifier.testTag("menu_role_${role.name}")
                                    )
                                }
                            }
                        }
                        if (activeRole != UserRole.VENDOR) {
                            TextButton(
                                onClick = { showSocietySwitcher = !showSocietySwitcher },
                                modifier = Modifier.testTag("top_society_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Apartment,
                                    contentDescription = "Society",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showSocietySwitcher) "Done" else "Society")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useRail && !showSocietySwitcher) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = safeTabIndex == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.badgeCount > 0) {
                                                Badge { Text(tab.badgeCount.coerceAtMost(99).toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.id}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !showSocietySwitcher) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = safeTabIndex == index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.id}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        profileState is UiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        profileState is UiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = (profileState as UiState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        showSocietySwitcher || (activeRole != UserRole.VENDOR && activeSociety == null) -> {
                            SocietyOnboardingPane(
                                societies = societies,
                                isBusy = isBusy,
                                onCreateSociety = { name, city, addr, units, flat ->
                                    viewModel.createSociety(name, city, addr, units, flat) {
                                        showSocietySwitcher = false
                                    }
                                },
                                onJoinSociety = { soc, flat ->
                                    viewModel.joinSociety(soc, flat) {
                                        showSocietySwitcher = false
                                    }
                                },
                                onSwitchToVendor = {
                                    showSocietySwitcher = false
                                    viewModel.switchRole(UserRole.VENDOR)
                                }
                            )
                        }

                        else -> {
                            val currentTabId = tabs[safeTabIndex].id
                            when (currentTabId) {
                                "deals" -> {
                                    val soc = activeSociety
                                    if (soc != null) {
                                        MemberDealsFeedScreen(
                                            society = soc,
                                            userProfile = userProfile,
                                            offers = offers,
                                            myParticipations = myParticipations,
                                            isSocietyAdmin = soc.adminUid == currentUserId,
                                            onOfferClick = { offer -> selectedOfferId = offer.id },
                                            onSwitchToAdminCreate = {
                                                openAdminCreateOnNavigate = true
                                                viewModel.switchRole(UserRole.ADMIN)
                                            }
                                        )
                                    }
                                }
                                "orders" -> {
                                    MemberActivityScreen(
                                        offers = offers,
                                        myParticipations = myParticipations,
                                        onOfferClick = { offer -> selectedOfferId = offer.id }
                                    )
                                }
                                "admin" -> {
                                    val soc = activeSociety
                                    if (soc != null) {
                                        AdminDashboardScreen(
                                            society = soc,
                                            isCurrentUserAdmin = soc.adminUid == currentUserId,
                                            offers = offers,
                                            members = members,
                                            participations = societyParticipations,
                                            quotations = societyQuotations,
                                            isBusy = isBusy,
                                            openCreateInitially = openAdminCreateOnNavigate,
                                            onConsumeOpenCreate = { openAdminCreateOnNavigate = false },
                                            onCreateOffer = { title, desc, cat, price, unit, thr, type, maxL, deadline, details ->
                                                viewModel.createOffer(
                                                    society = soc,
                                                    title = title,
                                                    description = desc,
                                                    category = cat,
                                                    estimatedPrice = price,
                                                    unitLabel = unit,
                                                    minThreshold = thr,
                                                    thresholdType = type,
                                                    maxLimit = maxL,
                                                    deadlineLabel = deadline,
                                                    serviceDetails = details
                                                )
                                            },
                                            onOfferClick = { offer -> selectedOfferId = offer.id },
                                            onCreateNewSociety = { showSocietySwitcher = true }
                                        )
                                    }
                                }
                                "vendor" -> {
                                    VendorPortalScreen(
                                        myVendorProfile = myVendorProfile,
                                        vendorQuotations = vendorQuotations,
                                        isBusy = isBusy,
                                        onSaveVendorProfile = { name, cat, city, desc ->
                                            viewModel.registerVendorProfile(name, cat, city, desc, myVendorProfile)
                                        },
                                        onSubmitQuotation = { quote, price, avail, delivery, notes, reply ->
                                            viewModel.submitVendorQuotation(quote, price, avail, delivery, notes, reply)
                                        }
                                    )
                                }
                                "catalog" -> {
                                    VendorCatalogScreen(vendors = activeVendors)
                                }
                                "alerts" -> {
                                    AlertsAndAccountScreen(
                                        userProfile = userProfile,
                                        activeSociety = activeSociety,
                                        notifications = notifications,
                                        onSwitchRole = { role -> viewModel.switchRole(role) },
                                        onSwitchSocietyClick = { showSocietySwitcher = true },
                                        onNotificationOfferClick = { offerId -> selectedOfferId = offerId },
                                        onSignOut = {
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
