package com.example.data.remote

import android.content.Context
import com.example.R
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
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VendorProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocietyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.withFirestoreLogging(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- REALTIME STREAMS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreLogging(OperationType.GET, path, uid)
        )
    }

    fun observeDiscoverableSocieties(): Flow<List<Society>> = flow {
        val uid = requireUserId()
        val path = "societies"
        emitAll(
            db.collection("societies")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Society::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeSociety(societyId: String): Flow<Society?> = flow {
        val uid = requireUserId()
        val path = "societies/$societyId"
        emitAll(
            db.collection("societies").document(societyId)
                .snapshots()
                .map { it.toObject(Society::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreLogging(OperationType.GET, path, uid)
        )
    }

    fun observeMyMembership(societyId: String, userId: String): Flow<SocietyMember?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "societies/$societyId/members/$userId"
        emitAll(
            db.collection("societies").document(societyId)
                .collection("members").document(userId)
                .snapshots()
                .map { it.toObject(SocietyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreLogging(OperationType.GET, path, uid)
        )
    }

    fun observeSocietyMembers(societyId: String): Flow<List<SocietyMember>> = flow {
        val uid = requireUserId()
        val path = "societies/$societyId/members"
        emitAll(
            db.collection("societies").document(societyId)
                .collection("members")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocietyMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.flatNumber }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeSocietyOffers(societyId: String): Flow<List<Offer>> = flow {
        val uid = requireUserId()
        val path = "societies/$societyId/offers"
        emitAll(
            db.collection("societies").document(societyId)
                .collection("offers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Offer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeUserParticipations(userId: String): Flow<List<Participation>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "participations"
        emitAll(
            db.collection("participations")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Participation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeSocietyParticipations(societyId: String, adminUid: String): Flow<List<Participation>> = flow {
        val uid = requireUserId()
        val path = "participations"
        emitAll(
            db.collection("participations")
                .whereEqualTo("societyId", societyId)
                .whereEqualTo("societyAdminUid", adminUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Participation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeActiveVendors(): Flow<List<VendorProfile>> = flow {
        val uid = requireUserId()
        val path = "vendors"
        emitAll(
            db.collection("vendors")
                .whereEqualTo("activeCatalog", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VendorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.businessName.lowercase() }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeMyVendorProfile(userId: String): Flow<VendorProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "vendors/$userId"
        emitAll(
            db.collection("vendors").document(userId)
                .snapshots()
                .map { it.toObject(VendorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreLogging(OperationType.GET, path, uid)
        )
    }

    fun observeVendorQuotations(vendorUid: String): Flow<List<Quotation>> = flow {
        val uid = requireUserId()
        check(uid == vendorUid) { "User ID mismatch" }
        val path = "quotations"
        emitAll(
            db.collection("quotations")
                .whereEqualTo("vendorUid", vendorUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Quotation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    fun observeSocietyQuotations(societyId: String, adminUid: String): Flow<List<Quotation>> = flow {
        val uid = requireUserId()
        val path = "quotations"
        emitAll(
            db.collection("quotations")
                .whereEqualTo("societyId", societyId)
                .whereEqualTo("societyAdminUid", adminUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Quotation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .withFirestoreLogging(OperationType.LIST, path, uid)
        )
    }

    // --- MUTATIONS ---

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Community Member").take(80)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "activeRole" to UserRole.MEMBER.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                user.email?.takeIf { it.isNotBlank() }?.let { payload["email"] = it.take(120) }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun switchActiveRole(role: UserRole): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "activeRole" to role.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSociety(
        name: String,
        city: String,
        address: String,
        totalUnits: Long,
        flatNumber: String,
        adminDisplayName: String
    ): Result<Society> {
        var path = "societies"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanCity = city.trim()
            val cleanAddress = address.trim()
            val cleanFlat = flatNumber.trim().ifBlank { "Admin Office" }
            val cleanAdminName = adminDisplayName.trim().ifBlank { "Society Admin" }

            require(cleanName.length in 2..100) { "Society name must be 2–100 characters." }
            require(cleanCity.length in 2..60) { "City must be 2–60 characters." }
            require(cleanAddress.length in 2..200) { "Address must be 2–200 characters." }
            require(totalUnits in 1L..10000L) { "Total units must be between 1 and 10,000." }

            val societyId = "soc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val inviteCode = cleanName.filter { it.isLetterOrDigit() }.uppercase().take(3).padEnd(3, 'S') +
                (100..999).random().toString()

            val socRef = db.collection("societies").document(societyId)
            path = socRef.path
            val socPayload = mapOf(
                "adminUid" to uid,
                "adminName" to cleanAdminName.take(80),
                "name" to cleanName,
                "city" to cleanCity,
                "address" to cleanAddress,
                "totalUnits" to totalUnits,
                "inviteCode" to inviteCode,
                "memberCount" to 1L,
                "discoverable" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            socRef.set(socPayload).await()

            val memberRef = socRef.collection("members").document(uid)
            path = memberRef.path
            memberRef.set(
                mapOf(
                    "userId" to uid,
                    "societyId" to societyId,
                    "displayName" to cleanAdminName.take(80),
                    "flatNumber" to cleanFlat.take(40),
                    "role" to "ADMIN",
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val userRef = db.collection("users").document(uid)
            path = userRef.path
            userRef.update(
                mapOf(
                    "activeSocietyId" to societyId,
                    "activeSocietyName" to cleanName,
                    "flatNumber" to cleanFlat.take(40),
                    "activeRole" to UserRole.ADMIN.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val created = socRef.get().await()
                .toObject(Society::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: Society(id = societyId, adminUid = uid, name = cleanName, inviteCode = inviteCode)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinSociety(
        society: Society,
        flatNumber: String,
        displayName: String
    ): Result<Unit> {
        var path = "societies/${society.id}/members"
        return try {
            val uid = requireUserId()
            val cleanFlat = flatNumber.trim()
            val cleanName = displayName.trim().ifBlank { "Resident" }
            require(society.id.isNotBlank()) { "Society ID cannot be blank." }
            require(cleanFlat.length in 1..40) { "Please enter your flat/unit number." }

            val socRef = db.collection("societies").document(society.id)
            val memberRef = socRef.collection("members").document(uid)
            path = memberRef.path
            val existingMember = memberRef.get().await()
            if (!existingMember.exists()) {
                val roleForMember = if (society.adminUid == uid) "ADMIN" else "MEMBER"
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "societyId" to society.id,
                        "displayName" to cleanName.take(80),
                        "flatNumber" to cleanFlat,
                        "role" to roleForMember,
                        "status" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                path = socRef.path
                socRef.update(
                    mapOf(
                        "memberCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val userRef = db.collection("users").document(uid)
            path = userRef.path
            userRef.update(
                mapOf(
                    "activeSocietyId" to society.id,
                    "activeSocietyName" to society.name.take(120),
                    "flatNumber" to cleanFlat,
                    "activeRole" to if (society.adminUid == uid) UserRole.ADMIN.name else UserRole.MEMBER.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createOffer(
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
        serviceDetails: String
    ): Result<String> {
        var path = "societies/${society.id}/offers"
        return try {
            val uid = requireUserId()
            require(society.adminUid == uid) { "Only the Society Admin can publish offers." }
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanUnit = unitLabel.trim()
            val cleanDeadline = deadlineLabel.trim()
            val cleanDetails = serviceDetails.trim()

            require(cleanTitle.length in 2..120) { "Title must be 2–120 characters." }
            require(cleanDesc.length in 2..600) { "Description must be 2–600 characters." }
            require(estimatedPrice > 0.0) { "Estimated price must be greater than 0." }
            require(cleanUnit.length in 1..50) { "Unit label is required." }
            require(minThreshold in 1L..10000L) { "Minimum threshold must be between 1 and 10,000." }
            require(maxLimit in 0L..100000L) { "Max limit must be 0 or greater." }
            require(cleanDeadline.length in 1..60) { "Deadline is required." }

            val offerId = "off_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val offerRef = db.collection("societies").document(society.id)
                .collection("offers").document(offerId)
            path = offerRef.path

            val payload = mapOf(
                "societyId" to society.id,
                "societyName" to society.name.take(100),
                "societyAdminUid" to uid,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.name,
                "estimatedPrice" to estimatedPrice,
                "unitLabel" to cleanUnit,
                "negotiatedPrice" to 0.0,
                "minThreshold" to minThreshold,
                "thresholdType" to thresholdType.name,
                "maxLimit" to maxLimit,
                "interestedCount" to 0L,
                "totalRequestedQty" to 0L,
                "confirmedCount" to 0L,
                "confirmedQty" to 0L,
                "status" to OfferStatus.COLLECTING_INTEREST.name,
                "deadlineLabel" to cleanDeadline,
                "serviceDetails" to cleanDetails.take(400),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            offerRef.set(payload).await()
            Result.success(offerId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun expressOrUpdateInterest(
        offer: Offer,
        memberName: String,
        flatNumber: String,
        requestedQty: Long,
        preferredOption: String,
        existingParticipation: Participation?
    ): Result<Unit> {
        var path = "participations"
        return try {
            val uid = requireUserId()
            require(requestedQty in 1L..1000L) { "Quantity must be between 1 and 1,000." }
            val cleanOption = preferredOption.trim().take(120)
            val cleanName = memberName.trim().ifBlank { "Member" }.take(80)
            val cleanFlat = flatNumber.trim().ifBlank { "Resident" }.take(40)

            val partId = "${offer.societyId}_${offer.id}_$uid"
            val partRef = db.collection("participations").document(partId)
            path = partRef.path

            val wasInterested = existingParticipation?.stageEnum() == ParticipationStage.INTERESTED
            val oldQty = if (wasInterested) existingParticipation.requestedQty else 0L
            val memberDelta = if (wasInterested) 0L else 1L
            val qtyDelta = requestedQty - oldQty

            if (existingParticipation == null) {
                partRef.set(
                    mapOf(
                        "userId" to uid,
                        "memberName" to cleanName,
                        "flatNumber" to cleanFlat,
                        "societyId" to offer.societyId,
                        "societyAdminUid" to offer.societyAdminUid,
                        "offerId" to offer.id,
                        "offerTitle" to offer.title.take(120),
                        "stage" to ParticipationStage.INTERESTED.name,
                        "requestedQty" to requestedQty,
                        "confirmedQty" to 0L,
                        "preferredOption" to cleanOption,
                        "unitPrice" to offer.effectivePrice(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                partRef.update(
                    mapOf(
                        "stage" to ParticipationStage.INTERESTED.name,
                        "requestedQty" to requestedQty,
                        "preferredOption" to cleanOption,
                        "unitPrice" to offer.effectivePrice(),
                        "memberName" to cleanName,
                        "flatNumber" to cleanFlat,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val offerRef = db.collection("societies").document(offer.societyId)
                .collection("offers").document(offer.id)
            path = offerRef.path

            val projectedCount = (offer.interestedCount + memberDelta).coerceAtLeast(0L)
            val projectedQty = (offer.totalRequestedQty + qtyDelta).coerceAtLeast(0L)
            val projectedProgress = if (offer.thresholdTypeEnum() == ThresholdType.QUANTITY) projectedQty else projectedCount
            val nextStatus = if (offer.statusEnum() == OfferStatus.COLLECTING_INTEREST && projectedProgress >= offer.minThreshold) {
                OfferStatus.THRESHOLD_REACHED.name
            } else {
                offer.status
            }

            offerRef.update(
                mapOf(
                    "interestedCount" to FieldValue.increment(memberDelta),
                    "totalRequestedQty" to FieldValue.increment(qtyDelta),
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun withdrawInterest(
        offer: Offer,
        participation: Participation
    ): Result<Unit> {
        var path = "participations/${participation.id}"
        return try {
            val uid = requireUserId()
            check(participation.userId == uid)
            val partRef = db.collection("participations").document(participation.id)
            partRef.update(
                mapOf(
                    "stage" to ParticipationStage.WITHDRAWN.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (participation.stageEnum() == ParticipationStage.INTERESTED) {
                val offerRef = db.collection("societies").document(offer.societyId)
                    .collection("offers").document(offer.id)
                path = offerRef.path
                val decCount = if (offer.interestedCount > 0L) -1L else 0L
                val decQty = -participation.requestedQty.coerceAtMost(offer.totalRequestedQty)
                offerRef.update(
                    mapOf(
                        "interestedCount" to FieldValue.increment(decCount),
                        "totalRequestedQty" to FieldValue.increment(decQty),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun confirmFinalOrder(
        offer: Offer,
        memberName: String,
        flatNumber: String,
        confirmedQty: Long,
        preferredOption: String,
        existingParticipation: Participation?
    ): Result<Unit> {
        var path = "participations"
        return try {
            val uid = requireUserId()
            require(offer.statusEnum() == OfferStatus.FINAL_OFFER_PUBLISHED) {
                "Final negotiated offer has not been published yet."
            }
            require(confirmedQty in 1L..1000L) { "Confirmed quantity must be between 1 and 1,000." }

            val partId = "${offer.societyId}_${offer.id}_$uid"
            val partRef = db.collection("participations").document(partId)
            path = partRef.path

            val wasConfirmed = existingParticipation?.stageEnum() == ParticipationStage.CONFIRMED
            val oldConfirmedQty = if (wasConfirmed) existingParticipation.confirmedQty else 0L
            val countDelta = if (wasConfirmed) 0L else 1L
            val qtyDelta = confirmedQty - oldConfirmedQty

            if (existingParticipation == null) {
                partRef.set(
                    mapOf(
                        "userId" to uid,
                        "memberName" to memberName.trim().ifBlank { "Member" }.take(80),
                        "flatNumber" to flatNumber.trim().ifBlank { "Resident" }.take(40),
                        "societyId" to offer.societyId,
                        "societyAdminUid" to offer.societyAdminUid,
                        "offerId" to offer.id,
                        "offerTitle" to offer.title.take(120),
                        "stage" to ParticipationStage.CONFIRMED.name,
                        "requestedQty" to confirmedQty,
                        "confirmedQty" to confirmedQty,
                        "preferredOption" to preferredOption.trim().take(120),
                        "unitPrice" to offer.effectivePrice(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                partRef.update(
                    mapOf(
                        "stage" to ParticipationStage.CONFIRMED.name,
                        "confirmedQty" to confirmedQty,
                        "preferredOption" to preferredOption.trim().take(120),
                        "unitPrice" to offer.effectivePrice(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val offerRef = db.collection("societies").document(offer.societyId)
                .collection("offers").document(offer.id)
            path = offerRef.path
            offerRef.update(
                mapOf(
                    "confirmedCount" to FieldValue.increment(countDelta),
                    "confirmedQty" to FieldValue.increment(qtyDelta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelFinalOrder(
        offer: Offer,
        participation: Participation
    ): Result<Unit> {
        var path = "participations/${participation.id}"
        return try {
            val uid = requireUserId()
            check(participation.userId == uid)
            val partRef = db.collection("participations").document(participation.id)
            partRef.update(
                mapOf(
                    "stage" to ParticipationStage.CANCELLED.name,
                    "confirmedQty" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (participation.stageEnum() == ParticipationStage.CONFIRMED) {
                val offerRef = db.collection("societies").document(offer.societyId)
                    .collection("offers").document(offer.id)
                path = offerRef.path
                val decCount = if (offer.confirmedCount > 0L) -1L else 0L
                val decQty = -participation.confirmedQty.coerceAtMost(offer.confirmedQty)
                offerRef.update(
                    mapOf(
                        "confirmedCount" to FieldValue.increment(decCount),
                        "confirmedQty" to FieldValue.increment(decQty),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN VENDOR ENGAGEMENT & NEGOTIATION ---

    suspend fun moveOfferToNegotiation(offer: Offer): Result<Unit> {
        val path = "societies/${offer.societyId}/offers/${offer.id}"
        return try {
            val uid = requireUserId()
            require(offer.societyAdminUid == uid) { "Only the Society Admin can start vendor negotiation." }
            db.collection("societies").document(offer.societyId)
                .collection("offers").document(offer.id)
                .update(
                    mapOf(
                        "status" to OfferStatus.VENDOR_NEGOTIATION.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun registerOrUpdateVendorProfile(
        businessName: String,
        category: OfferCategory,
        city: String,
        description: String,
        existingVendor: VendorProfile?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        var path = "vendors/$uid"
        return try {
            val cleanName = businessName.trim()
            val cleanCity = city.trim()
            val cleanDesc = description.trim()
            require(cleanName.length in 2..100) { "Business name must be 2–100 characters." }
            require(cleanCity.length in 2..60) { "City must be 2–60 characters." }
            require(cleanDesc.length in 2..300) { "Description must be 2–300 characters." }

            val vendorRef = db.collection("vendors").document(uid)
            if (existingVendor == null) {
                vendorRef.set(
                    mapOf(
                        "vendorUid" to uid,
                        "businessName" to cleanName,
                        "category" to category.name,
                        "city" to cleanCity,
                        "description" to cleanDesc,
                        "activeCatalog" to true,
                        "completedDeals" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                vendorRef.update(
                    mapOf(
                        "businessName" to cleanName,
                        "category" to category.name,
                        "city" to cleanCity,
                        "description" to cleanDesc,
                        "activeCatalog" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            path = "users/$uid"
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "businessName" to cleanName,
                        "activeRole" to UserRole.VENDOR.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun inviteVendorForQuotation(
        offer: Offer,
        vendor: VendorProfile
    ): Result<Unit> {
        val quoteId = "qt_${offer.id}_${vendor.vendorUid.take(12)}"
        val path = "quotations/$quoteId"
        return try {
            val uid = requireUserId()
            require(offer.societyAdminUid == uid) { "Only the Society Admin can invite vendors." }
            val quoteRef = db.collection("quotations").document(quoteId)
            val existing = quoteRef.get().await()
            if (existing.exists()) {
                quoteRef.update(
                    mapOf(
                        "aggregatedMemberCount" to offer.interestedCount,
                        "aggregatedQty" to offer.totalRequestedQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                quoteRef.set(
                    mapOf(
                        "societyId" to offer.societyId,
                        "societyName" to offer.societyName.take(100),
                        "societyAdminUid" to uid,
                        "offerId" to offer.id,
                        "offerTitle" to offer.title.take(120),
                        "offerCategory" to offer.category,
                        "aggregatedMemberCount" to offer.interestedCount,
                        "aggregatedQty" to offer.totalRequestedQty,
                        "estimatedPrice" to offer.estimatedPrice,
                        "unitLabel" to offer.unitLabel.take(50),
                        "vendorUid" to vendor.vendorUid,
                        "vendorName" to vendor.businessName.take(100),
                        "status" to QuotationStatus.INVITED.name,
                        "quotedPrice" to 0.0,
                        "availability" to "",
                        "estimatedDelivery" to "",
                        "proposalNotes" to "",
                        "adminQuery" to "",
                        "vendorReply" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (offer.statusEnum() == OfferStatus.COLLECTING_INTEREST ||
                offer.statusEnum() == OfferStatus.THRESHOLD_REACHED
            ) {
                db.collection("societies").document(offer.societyId)
                    .collection("offers").document(offer.id)
                    .update(
                        mapOf(
                            "status" to OfferStatus.VENDOR_NEGOTIATION.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitVendorQuotation(
        quotation: Quotation,
        quotedPrice: Double,
        availability: String,
        estimatedDelivery: String,
        proposalNotes: String,
        vendorReply: String
    ): Result<Unit> {
        val path = "quotations/${quotation.id}"
        return try {
            val uid = requireUserId()
            require(quotation.vendorUid == uid) { "Only the invited vendor can submit this quotation." }
            require(quotedPrice > 0.0) { "Quoted price must be greater than 0." }
            require(availability.trim().isNotEmpty()) { "Availability window is required." }
            require(estimatedDelivery.trim().isNotEmpty()) { "Estimated delivery timeline is required." }

            db.collection("quotations").document(quotation.id)
                .update(
                    mapOf(
                        "status" to QuotationStatus.SUBMITTED.name,
                        "quotedPrice" to quotedPrice,
                        "availability" to availability.trim().take(120),
                        "estimatedDelivery" to estimatedDelivery.trim().take(120),
                        "proposalNotes" to proposalNotes.trim().take(500),
                        "vendorReply" to vendorReply.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun askVendorQuery(quotation: Quotation, query: String): Result<Unit> {
        val path = "quotations/${quotation.id}"
        return try {
            val uid = requireUserId()
            require(quotation.societyAdminUid == uid) { "Only the Society Admin can send queries." }
            require(query.trim().isNotEmpty()) { "Query cannot be empty." }
            db.collection("quotations").document(quotation.id)
                .update(
                    mapOf(
                        "adminQuery" to query.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptQuotationAndPublishFinalOffer(
        offer: Offer,
        selectedQuotation: Quotation,
        otherQuotations: List<Quotation>,
        finalNegotiatedPrice: Double,
        finalTerms: String
    ): Result<Unit> {
        var path = "quotations/${selectedQuotation.id}"
        return try {
            val uid = requireUserId()
            require(offer.societyAdminUid == uid) { "Only the Society Admin can publish the final deal." }
            require(finalNegotiatedPrice > 0.0) { "Negotiated price must be greater than 0." }

            db.collection("quotations").document(selectedQuotation.id)
                .update(
                    mapOf(
                        "status" to QuotationStatus.ACCEPTED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            otherQuotations.filter { it.id != selectedQuotation.id && it.statusEnum() != QuotationStatus.REJECTED }
                .forEach { other ->
                    db.collection("quotations").document(other.id)
                        .update(
                            mapOf(
                                "status" to QuotationStatus.REJECTED.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }

            val offerRef = db.collection("societies").document(offer.societyId)
                .collection("offers").document(offer.id)
            path = offerRef.path
            offerRef.update(
                mapOf(
                    "status" to OfferStatus.FINAL_OFFER_PUBLISHED.name,
                    "negotiatedPrice" to finalNegotiatedPrice,
                    "selectedVendorUid" to selectedQuotation.vendorUid,
                    "selectedVendorName" to selectedQuotation.vendorName.take(120),
                    "selectedQuotationId" to selectedQuotation.id,
                    "finalTerms" to finalTerms.trim().ifBlank {
                        "Final negotiated society rate. Confirm your units within 48 hours."
                    }.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun closeOffer(offer: Offer): Result<Unit> {
        val path = "societies/${offer.societyId}/offers/${offer.id}"
        return try {
            val uid = requireUserId()
            require(offer.societyAdminUid == uid) { "Only the Society Admin can close an offer." }
            db.collection("societies").document(offer.societyId)
                .collection("offers").document(offer.id)
                .update(
                    mapOf(
                        "status" to OfferStatus.CLOSED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
