package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String) {
    MEMBER("Member"),
    ADMIN("Society Admin"),
    VENDOR("Vendor");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class OfferCategory(val label: String) {
    GROCERY("Bulk Grocery"),
    CLEANING("Home Cleaning"),
    DAILY_ESSENTIALS("Daily Essentials"),
    PEST_CONTROL("Pest Control"),
    APPLIANCE("AC & Appliances"),
    PLUMBING_ELECTRICAL("Plumbing & Electrical"),
    OTHER("Community Services");

    companion object {
        fun fromRaw(raw: String?): OfferCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class ThresholdType(val label: String) {
    MEMBERS("Interested Members"),
    QUANTITY("Total Requested Units");

    companion object {
        fun fromRaw(raw: String?): ThresholdType =
            entries.firstOrNull { it.name == raw } ?: MEMBERS
    }
}

enum class OfferStatus(val label: String) {
    COLLECTING_INTEREST("Collecting Interest"),
    THRESHOLD_REACHED("Threshold Reached"),
    VENDOR_NEGOTIATION("Vendor Negotiation"),
    FINAL_OFFER_PUBLISHED("Final Deal Published"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): OfferStatus =
            entries.firstOrNull { it.name == raw } ?: COLLECTING_INTEREST
    }
}

enum class ParticipationStage(val label: String) {
    INTERESTED("Interested (Stage 1)"),
    WITHDRAWN("Withdrawn"),
    CONFIRMED("Confirmed Order (Stage 2)"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): ParticipationStage =
            entries.firstOrNull { it.name == raw } ?: INTERESTED
    }
}

enum class QuotationStatus(val label: String) {
    INVITED("Quote Requested"),
    SUBMITTED("Submitted"),
    ACCEPTED("Selected Vendor"),
    REJECTED("Not Selected");

    companion object {
        fun fromRaw(raw: String?): QuotationStatus =
            entries.firstOrNull { it.name == raw } ?: INVITED
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val activeRole: String = UserRole.MEMBER.name,
    val activeSocietyId: String? = null,
    val activeSocietyName: String? = null,
    val flatNumber: String? = null,
    val businessName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromRaw(activeRole)
}

@Keep
@IgnoreExtraProperties
data class Society(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val name: String = "",
    val city: String = "",
    val address: String = "",
    val totalUnits: Long = 100L,
    val inviteCode: String = "",
    val memberCount: Long = 1L,
    val discoverable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocietyMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val societyId: String = "",
    val displayName: String = "",
    val flatNumber: String = "",
    val role: String = "MEMBER",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Offer(
    @DocumentId val id: String = "",
    val societyId: String = "",
    val societyName: String = "",
    val societyAdminUid: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = OfferCategory.GROCERY.name,
    val estimatedPrice: Double = 0.0,
    val unitLabel: String = "per unit",
    val negotiatedPrice: Double = 0.0,
    val minThreshold: Long = 10L,
    val thresholdType: String = ThresholdType.MEMBERS.name,
    val maxLimit: Long = 0L,
    val interestedCount: Long = 0L,
    val totalRequestedQty: Long = 0L,
    val confirmedCount: Long = 0L,
    val confirmedQty: Long = 0L,
    val status: String = OfferStatus.COLLECTING_INTEREST.name,
    val deadlineLabel: String = "",
    val serviceDetails: String = "",
    val selectedVendorUid: String? = null,
    val selectedVendorName: String? = null,
    val selectedQuotationId: String? = null,
    val finalTerms: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): OfferCategory = OfferCategory.fromRaw(category)
    fun thresholdTypeEnum(): ThresholdType = ThresholdType.fromRaw(thresholdType)
    fun statusEnum(): OfferStatus = OfferStatus.fromRaw(status)

    fun currentThresholdProgress(): Long =
        if (thresholdTypeEnum() == ThresholdType.QUANTITY) totalRequestedQty else interestedCount

    fun thresholdRatio(): Float {
        if (minThreshold <= 0L) return 1f
        return (currentThresholdProgress().toFloat() / minThreshold.toFloat()).coerceIn(0f, 1f)
    }

    fun remainingToThreshold(): Long =
        (minThreshold - currentThresholdProgress()).coerceAtLeast(0L)

    fun isThresholdMet(): Boolean = currentThresholdProgress() >= minThreshold

    fun effectivePrice(): Double =
        if (negotiatedPrice > 0.0) negotiatedPrice else estimatedPrice

    fun savingsPercent(): Int {
        if (negotiatedPrice <= 0.0 || estimatedPrice <= 0.0 || negotiatedPrice >= estimatedPrice) return 0
        return (((estimatedPrice - negotiatedPrice) / estimatedPrice) * 100.0).toInt()
    }
}

@Keep
@IgnoreExtraProperties
data class Participation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val flatNumber: String = "",
    val societyId: String = "",
    val societyAdminUid: String = "",
    val offerId: String = "",
    val offerTitle: String = "",
    val stage: String = ParticipationStage.INTERESTED.name,
    val requestedQty: Long = 1L,
    val confirmedQty: Long = 0L,
    val preferredOption: String = "",
    val unitPrice: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun stageEnum(): ParticipationStage = ParticipationStage.fromRaw(stage)
}

@Keep
@IgnoreExtraProperties
data class VendorProfile(
    @DocumentId val id: String = "",
    val vendorUid: String = "",
    val businessName: String = "",
    val category: String = OfferCategory.OTHER.name,
    val city: String = "",
    val description: String = "",
    val activeCatalog: Boolean = true,
    val completedDeals: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): OfferCategory = OfferCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class Quotation(
    @DocumentId val id: String = "",
    val societyId: String = "",
    val societyName: String = "",
    val societyAdminUid: String = "",
    val offerId: String = "",
    val offerTitle: String = "",
    val offerCategory: String = OfferCategory.OTHER.name,
    val aggregatedMemberCount: Long = 0L,
    val aggregatedQty: Long = 0L,
    val estimatedPrice: Double = 0.0,
    val unitLabel: String = "per unit",
    val vendorUid: String = "",
    val vendorName: String = "",
    val status: String = QuotationStatus.INVITED.name,
    val quotedPrice: Double = 0.0,
    val availability: String = "",
    val estimatedDelivery: String = "",
    val proposalNotes: String = "",
    val adminQuery: String = "",
    val vendorReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): QuotationStatus = QuotationStatus.fromRaw(status)
    fun categoryEnum(): OfferCategory = OfferCategory.fromRaw(offerCategory)
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val badge: String,
    val timestamp: Timestamp?,
    val targetOfferId: String? = null
)
