package com.smartmaps.app.data.local.dao

import androidx.room.*
import com.smartmaps.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────
// ServiceCategoryDao
// ─────────────────────────────────────────────────────────────
@Dao
interface ServiceCategoryDao {
    @Query("SELECT * FROM service_categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<ServiceCategoryEntity>>

    @Query("SELECT * FROM service_categories ORDER BY sortOrder ASC")
    suspend fun getAllCategoriesOnce(): List<ServiceCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<ServiceCategoryEntity>)

    @Query("SELECT COUNT(*) FROM service_categories")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────────────
// ServiceDao
// ─────────────────────────────────────────────────────────────
@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY name ASC")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getServicesByCategory(categoryId: String): Flow<List<ServiceEntity>>

    @Query("""
        SELECT * FROM services 
        WHERE name LIKE '%' || :query || '%' 
           OR keywords LIKE '%' || :query || '%'
           OR synonyms LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT 30
    """)
    suspend fun searchServices(query: String): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getServiceById(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Query("SELECT COUNT(*) FROM services")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────────────
// ProviderDao
// ─────────────────────────────────────────────────────────────
@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY rating DESC, completedJobs DESC")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Query("""
        SELECT p.* FROM providers p
        INNER JOIN provider_services ps ON p.id = ps.providerId
        WHERE ps.serviceId = :serviceId AND p.isAvailable = 1
        ORDER BY p.rating DESC
    """)
    suspend fun getProvidersByService(serviceId: String): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE isAvailable = 1 ORDER BY rating DESC")
    suspend fun getAvailableProviders(): List<ProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<ProviderEntity>)

    @Update
    suspend fun update(provider: ProviderEntity)

    @Delete
    suspend fun delete(provider: ProviderEntity)

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────────────
// ProviderServiceDao
// ─────────────────────────────────────────────────────────────
@Dao
interface ProviderServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(providerService: ProviderServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providerServices: List<ProviderServiceEntity>)

    @Query("SELECT * FROM provider_services WHERE providerId = :providerId")
    suspend fun getServicesForProvider(providerId: String): List<ProviderServiceEntity>

    @Query("DELETE FROM provider_services WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: String)
}

// ─────────────────────────────────────────────────────────────
// PlaceCacheDao
// ─────────────────────────────────────────────────────────────
@Dao
interface PlaceCacheDao {
    @Query("""
        SELECT * FROM places_cache 
        WHERE categoryKey LIKE '%' || :categoryQuery || '%'
           OR categoryLabel LIKE '%' || :categoryQuery || '%'
           OR name LIKE '%' || :categoryQuery || '%'
        ORDER BY rating DESC
        LIMIT :limit
    """)
    suspend fun searchPlaces(categoryQuery: String, limit: Int = 100): List<PlaceCacheEntity>

    @Query("SELECT * FROM places_cache WHERE id = :id")
    suspend fun getPlaceById(id: String): PlaceCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<PlaceCacheEntity>)

    @Query("DELETE FROM places_cache WHERE cachedAt < :olderThan")
    suspend fun evictOldCache(olderThan: Long)

    @Query("SELECT COUNT(*) FROM places_cache")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────────────
// BookingDao
// ─────────────────────────────────────────────────────────────
@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookingsByUser(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getBookingsByProvider(providerId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(booking: BookingEntity)

    @Update
    suspend fun update(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status, updatedAt = :updatedAt WHERE id = :bookingId")
    suspend fun updateStatus(bookingId: String, status: String, updatedAt: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────
// ReviewDao
// ─────────────────────────────────────────────────────────────
@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getReviewsForProvider(providerId: String): Flow<List<ReviewEntity>>

    @Query("SELECT AVG(rating) FROM reviews WHERE providerId = :providerId")
    suspend fun getAverageRating(providerId: String): Float?

    @Query("SELECT COUNT(*) FROM reviews WHERE providerId = :providerId")
    suspend fun getReviewCount(providerId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getReviewByBooking(bookingId: String): ReviewEntity?
}

// ─────────────────────────────────────────────────────────────
// MessageDao
// ─────────────────────────────────────────────────────────────
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE bookingId = :bookingId ORDER BY createdAt ASC")
    fun getMessagesForBooking(bookingId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE bookingId = :bookingId AND senderType != :currentUserType")
    suspend fun markAsRead(bookingId: String, currentUserType: String)

    @Query("SELECT COUNT(*) FROM messages WHERE bookingId = :bookingId AND isRead = 0 AND senderType != :currentUserType")
    suspend fun getUnreadCount(bookingId: String, currentUserType: String): Int
}

// ─────────────────────────────────────────────────────────────
// SavedDao
// ─────────────────────────────────────────────────────────────
@Dao
interface SavedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlace(savedPlace: SavedPlaceEntity)

    @Delete
    suspend fun unsavePlace(savedPlace: SavedPlaceEntity)

    @Query("SELECT * FROM saved_places ORDER BY savedAt DESC")
    fun getSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT COUNT(*) FROM saved_places WHERE placeId = :placeId")
    suspend fun isPlaceSaved(placeId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProvider(savedProvider: SavedProviderEntity)

    @Delete
    suspend fun unsaveProvider(savedProvider: SavedProviderEntity)

    @Query("SELECT * FROM saved_providers ORDER BY savedAt DESC")
    fun getSavedProviders(): Flow<List<SavedProviderEntity>>
}

// ─────────────────────────────────────────────────────────────
// UserProfileDao
// ─────────────────────────────────────────────────────────────
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'local_user' LIMIT 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'local_user' LIMIT 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)
}

// ─────────────────────────────────────────────────────────────
// Chat DAOs
// ─────────────────────────────────────────────────────────────
@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE createdAt >= :startOfDayMillis ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionSince(startOfDayMillis: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSessionTime(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getHistoryForSession(sessionId: String): Flow<List<ChatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatHistoryEntity)

    @Query("DELETE FROM chat_history WHERE sessionId = :sessionId")
    suspend fun deleteHistoryForSession(sessionId: String)
}
