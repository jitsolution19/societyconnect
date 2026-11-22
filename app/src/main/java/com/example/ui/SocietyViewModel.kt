package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationItem
import com.example.data.model.Offer
import com.example.data.model.OfferCategory
import com.example.data.model.OfferStatus
import com.example.data.model.Participation
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
import com.example.data.model.Society
import com.example.data.model.SocietyMember
import com.example.data.model.ThresholdType
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VendorProfile
import com.example.data.remote.SocietyRepository
import com.example.data.remote.toFriendlyFirestoreMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocietyViewModel(
    private val repository: SocietyRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, fallbackError, rootError)
                emit(UiState.Error(rootError.toFriendlyFirestoreMessage()))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow(TAG, "Failed to load profile")

    val discoverableSocietiesState: StateFlow<UiState<List<Society>>> =
        repository.observeDiscoverableSocieties()
            .toUiStateFlow(TAG, "Failed to load societies")

    private val activeSocietyIdFlow: Flow<String?> =
        userProfileState.map { (it as? UiState.Success)?.data?.activeSocietyId }

    val activeSocietyState: StateFlow<UiState<Society?>> =
        activeSocietyIdFlow.flatMapLatest { socId ->
            if (socId.isNullOrBlank()) flowOf(null) else repository.observeSociety(socId)
        }.toUiStateFlow(TAG, "Failed to load active society")

    val myMembershipState: StateFlow<UiState<SocietyMember?>> =
        activeSocietyIdFlow.flatMapLatest { socId ->
            if (socId.isNullOrBlank()) flowOf(null) else repository.observeMyMembership(socId, currentUserId)
        }.toUiStateFlow(TAG, "Failed to load membership")

    val societyOffersState: StateFlow<UiState<List<Offer>>> =
        combine(activeSocietyIdFlow, myMembershipState) { socId, memState ->
            val hasMember = (memState as? UiState.Success)?.data != null
            if (!socId.isNullOrBlank() && hasMember) socId else null
        }.flatMapLatest { verifiedSocId ->
            if (verifiedSocId == null) flowOf(emptyList())
            else repository.observeSocietyOffers(verifiedSocId)
        }.toUiStateFlow(TAG, "Failed to load society offers")

    val societyMembersState: StateFlow<UiState<List<SocietyMember>>> =
        activeSocietyState.flatMapLatest { socState ->
            val society = (socState as? UiState.Success)?.data
            if (society != null && society.adminUid == currentUserId) {
                repository.observeSocietyMembers(society.id)
            } else {
                flowOf(emptyList())
            }
        }.toUiStateFlow(TAG, "Failed to load society members")

    val myParticipationsState: StateFlow<UiState<List<Participation>>> =
        repository.observeUserParticipations(currentUserId)
            .toUiStateFlow(TAG, "Failed to load your participations")

    val societyParticipationsState: StateFlow<UiState<List<Participation>>> =
        activeSocietyState.flatMapLatest { socState ->
            val society = (socState as? UiState.Success)?.data
            if (society != null && society.adminUid == currentUserId) {
                repository.observeSocietyParticipations(society.id, currentUserId)
            } else {
                flowOf(emptyList())
            }
        }.toUiStateFlow(TAG, "Failed to load society participations")

    val activeVendorsState: StateFlow<UiState<List<VendorProfile>>> =
        repository.observeActiveVendors()
            .toUiStateFlow(TAG, "Failed to load vendor catalog")

    val myVendorProfileState: StateFlow<UiState<VendorProfile?>> =
        repository.observeMyVendorProfile(currentUserId)
            .toUiStateFlow(TAG, "Failed to load vendor profile")

    val vendorQuotationsState: StateFlow<UiState<List<Quotation>>> =
        repository.observeVendorQuotations(currentUserId)
            .toUiStateFlow(TAG, "Failed to load vendor quotations")

    val societyQuotationsState: StateFlow<UiState<List<Quotation>>> =
        activeSocietyState.flatMapLatest { socState ->
            val society = (socState as? UiState.Success)?.data
            if (society != null && society.adminUid == currentUserId) {
                repository.observeSocietyQuotations(society.id, currentUserId)
            } else {
                flowOf(emptyList())
            }
        }.toUiStateFlow(TAG, "Failed to load society quotations")

    val notificationsState: StateFlow<List<NotificationItem>> =
        combine(
            societyOffersState,
            vendorQuotationsState,
            societyQuotationsState
        ) { offersUi, vendorQuotesUi, socQuotesUi ->
            val alerts = mutableListOf<NotificationItem>()
            val offers = (offersUi as? UiState.Success)?.data.orEmpty()
            offers.forEach { offer ->
                when (offer.statusEnum()) {
                    OfferStatus.COLLECTING_INTEREST -> alerts.add(
                        NotificationItem(
                            id = "off_new_${offer.id}",
                            title = "New Offer: ${offer.title}",
                            message = "${offer.currentThresholdProgress()} of ${offer.minThreshold} needed · Est. ₹${offer.estimatedPrice.toInt()} ${offer.unitLabel}",
                            badge = "Collecting Interest",
                            timestamp = offer.createdAt,
                            targetOfferId = offer.id
                        )
                    )
                    OfferStatus.THRESHOLD_REACHED -> alerts.add(
                        NotificationItem(
                            id = "off_thr_${offer.id}",
                            title = "Threshold Reached: ${offer.title}",
                            message = "${offer.interestedCount} neighbors joined (${offer.totalRequestedQty} units). Ready for vendor quotes!",
                            badge = "Threshold Met",
                            timestamp = offer.updatedAt ?: offer.createdAt,
                            targetOfferId = offer.id
                        )
                    )
                    OfferStatus.FINAL_OFFER_PUBLISHED -> alerts.add(
                        NotificationItem(
                            id = "off_fin_${offer.id}",
                            title = "Final Deal Ready: ${offer.title}",
                            message = "Negotiated rate ₹${offer.negotiatedPrice.toInt()} with ${offer.selectedVendorName ?: "selected vendor"}. Confirm your order now!",
                            badge = "Final Offer",
                            timestamp = offer.updatedAt ?: offer.createdAt,
                            targetOfferId = offer.id
                        )
                    )
                    else -> Unit
                }
            }
            val vendorQuotes = (vendorQuotesUi as? UiState.Success)?.data.orEmpty()
            vendorQuotes.forEach { quote ->
                when (quote.statusEnum()) {
                    QuotationStatus.INVITED -> alerts.add(
                        NotificationItem(
                            id = "vq_inv_${quote.id}",
                            title = "Quotation Request: ${quote.offerTitle}",
                            message = "${quote.societyName} requested a quote for ${quote.aggregatedMemberCount} members (${quote.aggregatedQty} units).",
                            badge = "Vendor Invite",
                            timestamp = quote.createdAt
                        )
                    )
                    QuotationStatus.ACCEPTED -> alerts.add(
                        NotificationItem(
                            id = "vq_acc_${quote.id}",
                            title = "Proposal Selected: ${quote.offerTitle}",
                            message = "${quote.societyName} accepted your rate of ₹${quote.quotedPrice.toInt()} ${quote.unitLabel}.",
                            badge = "Deal Won",
                            timestamp = quote.updatedAt
                        )
                    )
                    else -> Unit
                }
            }
            val adminQuotes = (socQuotesUi as? UiState.Success)?.data.orEmpty()
            adminQuotes.filter { it.statusEnum() == QuotationStatus.SUBMITTED }.forEach { quote ->
                alerts.add(
                    NotificationItem(
                        id = "aq_sub_${quote.id}",
                        title = "Quote Submitted by ${quote.vendorName}",
                        message = "₹${quote.quotedPrice.toInt()} ${quote.unitLabel} for ${quote.offerTitle}. Tap offer to compare.",
                        badge = "Vendor Bid",
                        timestamp = quote.updatedAt,
                        targetOfferId = quote.offerId
                    )
                )
            }
            alerts.sortedByDescending { it.timestamp?.seconds ?: 0L }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun launchMutation(
        successMsg: String? = null,
        block: suspend () -> Result<*>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.onSuccess {
                if (successMsg != null) _actionMessage.value = successMsg
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage()
            }
        }
    }

    fun switchRole(role: UserRole) {
        launchMutation("Switched to ${role.label} workspace") {
            repository.switchActiveRole(role)
        }
    }

    fun createSociety(
        name: String,
        city: String,
        address: String,
        totalUnits: Long,
        flatNumber: String,
        onCreated: () -> Unit = {}
    ) {
        val displayName = (userProfileState.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Society Admin"
        launchMutation("Society created! You are the Society Admin.") {
            repository.createSociety(name, city, address, totalUnits, flatNumber, displayName)
                .also { if (it.isSuccess) onCreated() }
        }
    }

    fun joinSociety(
        society: Society,
        flatNumber: String,
        onJoined: () -> Unit = {}
    ) {
        val displayName = (userProfileState.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Resident"
        launchMutation("Joined ${society.name}!") {
            repository.joinSociety(society, flatNumber, displayName)
                .also { if (it.isSuccess) onJoined() }
        }
    }

    fun createOffer(
        society: Society,
        title: String,
        description: String,
        category: OfferCategory,
        estimatedPrice: Double,
        unitLabel: String,
        minThreshold: Long,
        thresholdType: ThresholdType,
        maxLimit: Long,
        deadlineLabel: String,
        serviceDetails: String,
        onCreated: () -> Unit = {}
    ) {
        launchMutation("Offer published to ${society.name}!") {
            repository.createOffer(
                society = society,
                title = title,
                description = description,
                category = category,
                estimatedPrice = estimatedPrice,
                unitLabel = unitLabel,
                minThreshold = minThreshold,
                thresholdType = thresholdType,
                maxLimit = maxLimit,
                deadlineLabel = deadlineLabel,
                serviceDetails = serviceDetails
            ).also { if (it.isSuccess) onCreated() }
        }
    }

    fun expressInterest(
        offer: Offer,
        requestedQty: Long,
        preferredOption: String,
        existingParticipation: Participation?
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val memberName = profile?.displayName ?: Firebase.auth.currentUser?.displayName ?: "Member"
        val flat = profile?.flatNumber ?: "Resident"
        launchMutation("Interest recorded! Tracking threshold progress.") {
            repository.expressOrUpdateInterest(
                offer = offer,
                memberName = memberName,
                flatNumber = flat,
                requestedQty = requestedQty,
                preferredOption = preferredOption,
                existingParticipation = existingParticipation
            )
        }
    }

    fun withdrawInterest(offer: Offer, participation: Participation) {
        launchMutation("Interest withdrawn.") {
            repository.withdrawInterest(offer, participation)
        }
    }

    fun confirmFinalOrder(
        offer: Offer,
        confirmedQty: Long,
        preferredOption: String,
        existingParticipation: Participation?
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val memberName = profile?.displayName ?: Firebase.auth.currentUser?.displayName ?: "Member"
        val flat = profile?.flatNumber ?: "Resident"
        launchMutation("Final order confirmed! Added to your active orders.") {
            repository.confirmFinalOrder(
                offer = offer,
                memberName = memberName,
                flatNumber = flat,
                confirmedQty = confirmedQty,
                preferredOption = preferredOption,
                existingParticipation = existingParticipation
            )
        }
    }

    fun cancelFinalOrder(offer: Offer, participation: Participation) {
        launchMutation("Order cancelled within window.") {
            repository.cancelFinalOrder(offer, participation)
        }
    }

    fun moveOfferToNegotiation(offer: Offer) {
        launchMutation("Offer moved to Vendor Negotiation.") {
            repository.moveOfferToNegotiation(offer)
        }
    }

    fun registerVendorProfile(
        businessName: String,
        category: OfferCategory,
        city: String,
        description: String,
        existingVendor: VendorProfile?,
        onSaved: () -> Unit = {}
    ) {
        launchMutation("Vendor profile saved to catalog!") {
            repository.registerOrUpdateVendorProfile(
                businessName = businessName,
                category = category,
                city = city,
                description = description,
                existingVendor = existingVendor
            ).also { if (it.isSuccess) onSaved() }
        }
    }

    fun inviteVendor(offer: Offer, vendor: VendorProfile) {
        launchMutation("Quotation request sent to ${vendor.businessName}.") {
            repository.inviteVendorForQuotation(offer, vendor)
        }
    }

    fun submitVendorQuotation(
        quotation: Quotation,
        quotedPrice: Double,
        availability: String,
        estimatedDelivery: String,
        proposalNotes: String,
        vendorReply: String,
        onSubmitted: () -> Unit = {}
    ) {
        launchMutation("Quotation submitted to ${quotation.societyName}!") {
            repository.submitVendorQuotation(
                quotation = quotation,
                quotedPrice = quotedPrice,
                availability = availability,
                estimatedDelivery = estimatedDelivery,
                proposalNotes = proposalNotes,
                vendorReply = vendorReply
            ).also { if (it.isSuccess) onSubmitted() }
        }
    }

    fun askVendorQuery(quotation: Quotation, query: String) {
        launchMutation("Query sent to ${quotation.vendorName}.") {
            repository.askVendorQuery(quotation, query)
        }
    }

    fun acceptQuotationAndPublishFinalOffer(
        offer: Offer,
        selectedQuotation: Quotation,
        otherQuotations: List<Quotation>,
        finalNegotiatedPrice: Double,
        finalTerms: String
    ) {
        launchMutation("Vendor selected & Final Deal published to society members!") {
            repository.acceptQuotationAndPublishFinalOffer(
                offer = offer,
                selectedQuotation = selectedQuotation,
                otherQuotations = otherQuotations,
                finalNegotiatedPrice = finalNegotiatedPrice,
                finalTerms = finalTerms
            )
        }
    }

    fun closeOffer(offer: Offer) {
        launchMutation("Campaign closed.") {
            repository.closeOffer(offer)
        }
    }

    companion object {
        private const val TAG = "SocietyVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
