package com.smartmaps.app.data.repository

import com.smartmaps.app.data.local.dao.BookingDao
import com.smartmaps.app.data.local.dao.MessageDao
import com.smartmaps.app.data.local.dao.ProviderDao
import com.smartmaps.app.data.local.dao.ReviewDao
import com.smartmaps.app.data.local.entity.BookingEntity
import com.smartmaps.app.data.local.entity.MessageEntity
import com.smartmaps.app.data.local.entity.ReviewEntity
import com.smartmaps.app.data.remote.api.BackendApi
import com.smartmaps.app.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val bookingDao: BookingDao,
    private val messageDao: MessageDao,
    private val reviewDao: ReviewDao,
    private val providerDao: ProviderDao,
    private val backendApi: BackendApi
) {

    // ─────────────────────────────────────────────────────────────
    // Bookings (Synced with Backend + Local Room DB)
    // ─────────────────────────────────────────────────────────────
    fun getAllBookings(): Flow<List<BookingEntity>> = bookingDao.getAllBookings()

    suspend fun syncBookingsFromBackend() = withContext(Dispatchers.IO) {
        try {
            val remote = backendApi.getBookings()
            for (b in remote.bookings) {
                bookingDao.insert(
                    BookingEntity(
                        id = b.id,
                        userId = b.userId,
                        providerId = b.providerId,
                        serviceId = b.serviceId,
                        status = b.status,
                        scheduledAt = b.scheduledAt,
                        address = b.address,
                        latitude = b.latitude,
                        longitude = b.longitude,
                        notes = b.notes,
                        estimatedCost = b.estimatedCost,
                        actualCost = b.actualCost,
                        createdAt = b.createdAt,
                        updatedAt = b.updatedAt
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun createBooking(
        providerId: String,
        serviceId: String,
        address: String,
        latitude: Double,
        longitude: Double,
        scheduledAt: Long,
        notes: String,
        estimatedCost: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val id = "bk_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val provider = providerDao.getProviderById(providerId)
            val finalCost = if (estimatedCost > 0) estimatedCost else (provider?.priceRangeMin?.takeIf { it > 0 } ?: 350)

            val booking = BookingEntity(
                id = id,
                userId = "local_user",
                providerId = providerId,
                serviceId = serviceId,
                status = BookingStatus.PENDING.name,
                scheduledAt = scheduledAt,
                address = address,
                latitude = latitude,
                longitude = longitude,
                notes = notes,
                estimatedCost = finalCost
            )
            bookingDao.insert(booking)

            // Sync with Backend
            var syncedMessagesFromBackend = false
            try {
                backendApi.createBooking(
                    BackendBookingCreateRequest(
                        id = id,
                        userId = "local_user",
                        providerId = providerId,
                        serviceId = serviceId,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        scheduledAt = scheduledAt,
                        notes = notes,
                        estimatedCost = finalCost
                    )
                )
                syncMessagesForBooking(id)
                syncedMessagesFromBackend = true
            } catch (_: Exception) {}

            // Ensure an immediate welcome message from the vendor is present in local Room DB
            if (!syncedMessagesFromBackend) {
                val vendorName = provider?.businessName?.ifBlank { provider.name } ?: "Verified Vendor"
                val welcomeMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    bookingId = id,
                    senderId = providerId,
                    senderType = "provider",
                    content = "👋 Namaste! Thank you for reaching out to $vendorName. " +
                        "Standard inspection & labour charge starts around ₹$finalCost. " +
                        "Feel free to share details, photos, or ask for an exact quote right here!"
                )
                messageDao.insert(welcomeMsg)
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateDirectInquiryBooking(providerId: String): String = withContext(Dispatchers.IO) {
        val existing = bookingDao.getAllBookings().first().firstOrNull { it.providerId == providerId }
        if (existing != null) {
            syncMessagesForBooking(existing.id)
            return@withContext existing.id
        }
        val provider = providerDao.getProviderById(providerId)
        val created = createBooking(
            providerId = providerId,
            serviceId = "direct_inquiry",
            address = provider?.address ?: "Customer Location",
            latitude = provider?.latitude ?: 17.3850,
            longitude = provider?.longitude ?: 78.4867,
            scheduledAt = System.currentTimeMillis() + 3600_000,
            notes = "Direct Inquiry via SmartMaps Vendor Connect",
            estimatedCost = provider?.priceRangeMin ?: 300
        )
        created.getOrDefault("bk_direct_$providerId")
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus, quotedCost: Int? = null) = withContext(Dispatchers.IO) {
        bookingDao.updateStatus(bookingId, status.name)
        if (quotedCost != null && quotedCost > 0) {
            bookingDao.getBookingById(bookingId)?.let { b ->
                bookingDao.update(b.copy(status = status.name, estimatedCost = quotedCost, updatedAt = System.currentTimeMillis()))
            }
        }

        try {
            backendApi.updateBookingStatus(
                bookingId = bookingId,
                request = BackendBookingStatusRequest(status = status.name, quotedCost = quotedCost)
            )
            syncMessagesForBooking(bookingId)
            return@withContext
        } catch (_: Exception) {}

        // Offline status update message in chat
        val b = bookingDao.getBookingById(bookingId)
        val provider = b?.let { providerDao.getProviderById(it.providerId) }
        val vName = provider?.businessName ?: "Service Provider"
        val statusText = when (status) {
            BookingStatus.ACCEPTED -> "✅ Booking Accepted by $vName! I will arrive at ${b?.address ?: "your address"} as scheduled."
            BookingStatus.ON_THE_WAY -> "🛵 $vName is on the way to your location! Live tracking is active."
            BookingStatus.ARRIVED -> "📍 I have arrived at your location. Please share floor/door details if needed."
            BookingStatus.IN_PROGRESS -> "🔧 Service work has started."
            BookingStatus.COMPLETED -> "🎉 Job completed! Total charge: ₹${quotedCost ?: b?.estimatedCost ?: 350}. Thank you for choosing $vName!"
            BookingStatus.REJECTED -> "⚠️ Sorry, I am tied up with an emergency job and cannot take this slot."
            BookingStatus.CANCELLED -> "❌ Booking was cancelled."
            else -> null
        }
        if (statusText != null) {
            messageDao.insert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    bookingId = bookingId,
                    senderId = b?.providerId ?: "provider",
                    senderType = "provider",
                    content = statusText
                )
            )
        }
    }

    suspend fun getBookingById(id: String) = withContext(Dispatchers.IO) {
        bookingDao.getBookingById(id)
    }

    // ─────────────────────────────────────────────────────────────
    // Real-Time Vendor-User Messages (Backend Synced + Smart Reply)
    // ─────────────────────────────────────────────────────────────
    fun getMessages(bookingId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForBooking(bookingId)

    suspend fun syncMessagesForBooking(bookingId: String) = withContext(Dispatchers.IO) {
        try {
            val remote = backendApi.getBookingMessages(bookingId)
            for (m in remote.messages) {
                messageDao.insert(
                    MessageEntity(
                        id = m.id,
                        bookingId = m.bookingId,
                        senderId = m.senderId,
                        senderType = m.senderType,
                        content = m.content,
                        messageType = m.messageType,
                        isRead = m.isRead,
                        createdAt = m.createdAt
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun sendMessage(
        bookingId: String,
        text: String,
        senderType: String = "user",
        imageUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        if (text.isBlank() && imageUrl.isBlank()) return@withContext

        // 1. Try syncing with Backend first (which also triggers smart vendor reply if senderType == "user")
        try {
            val resp = backendApi.sendBookingMessage(
                bookingId = bookingId,
                request = BackendSendMessageRequest(
                    senderId = if (senderType == "user") "local_user" else "provider",
                    senderType = senderType,
                    senderName = if (senderType == "user") "Customer" else "Vendor",
                    content = text,
                    messageType = if (imageUrl.isNotBlank()) "image" else "text"
                )
            )
            for (m in resp.messages) {
                messageDao.insert(
                    MessageEntity(
                        id = m.id,
                        bookingId = m.bookingId,
                        senderId = m.senderId,
                        senderType = m.senderType,
                        content = m.content,
                        messageType = m.messageType,
                        isRead = m.isRead,
                        createdAt = m.createdAt
                    )
                )
            }
            return@withContext
        } catch (_: Exception) {
            // Fall back to local Room DB + smart vendor response
        }

        val now = System.currentTimeMillis()
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            bookingId = bookingId,
            senderId = if (senderType == "user") "local_user" else "provider",
            senderType = senderType,
            content = text,
            messageType = if (imageUrl.isNotBlank()) "image" else "text",
            imageUrl = imageUrl,
            createdAt = now
        )
        messageDao.insert(message)

        // If sent by consumer, generate a realistic context-aware vendor reply locally
        if (senderType == "user") {
            val booking = bookingDao.getBookingById(bookingId)
            val provider = booking?.let { providerDao.getProviderById(it.providerId) }
            val vendorName = provider?.businessName ?: "Service Provider"
            val estCost = booking?.estimatedCost?.takeIf { it > 0 } ?: (provider?.priceRangeMin ?: 350)
            val lower = text.lowercase()

            val vendorReply = when {
                listOf("price", "cost", "charge", "quote", "how much", "rate", "discount").any { lower.contains(it) } ->
                    "💰 My standard service & labour charge is ₹$estCost. If any spare parts are needed, I will show you the transparent rate card before starting. Shall I confirm ₹$estCost?"
                listOf("when", "time", "arrive", "reach", "eta", "urgent", "fast", "where").any { lower.contains(it) } ->
                    "🛵 I am ~1.4 km away and can reach your location in 15–20 minutes. You can view my pin on SmartMaps!"
                listOf("location", "address", "pin", "map", "landmark").any { lower.contains(it) } ->
                    "📍 Got your location! I have opened the route on SmartMaps and will call you if I need the gate/flat number."
                listOf("ok", "confirm", "yes", "sure", "book", "come").any { lower.contains(it) } -> {
                    if (booking != null && booking.status == BookingStatus.PENDING.name) {
                        bookingDao.updateStatus(bookingId, BookingStatus.ACCEPTED.name)
                    }
                    "👍 Confirmed! I've accepted your request and packed the required tools. See you shortly!"
                }
                else ->
                    "Got it! Thanks for sharing: \"${text.take(45)}\". I have all the tools ready and will take care of this for you."
            }

            messageDao.insert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    bookingId = bookingId,
                    senderId = booking?.providerId ?: "provider",
                    senderType = "provider",
                    content = vendorReply,
                    createdAt = now + 400
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Reviews
    // ─────────────────────────────────────────────────────────────
    fun getProviderReviews(providerId: String) = reviewDao.getReviewsForProvider(providerId)

    suspend fun submitReview(
        providerId: String,
        bookingId: String,
        rating: Float,
        comment: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = reviewDao.getReviewByBooking(bookingId)
            if (existing != null) {
                return@withContext Result.failure(Exception("You have already reviewed this booking"))
            }

            val review = ReviewEntity(
                id = UUID.randomUUID().toString(),
                providerId = providerId,
                userId = "local_user",
                bookingId = bookingId,
                rating = rating,
                comment = comment
            )
            reviewDao.insert(review)

            val avgRating = reviewDao.getAverageRating(providerId) ?: rating
            val reviewCount = reviewDao.getReviewCount(providerId)
            val provider = providerDao.getProviderById(providerId)
            provider?.let {
                providerDao.update(it.copy(rating = avgRating, reviewCount = reviewCount))
            }

            try {
                backendApi.submitReview(
                    BackendReviewCreateRequest(
                        providerId = providerId,
                        userId = "local_user",
                        bookingId = bookingId,
                        rating = rating,
                        comment = comment
                    )
                )
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
