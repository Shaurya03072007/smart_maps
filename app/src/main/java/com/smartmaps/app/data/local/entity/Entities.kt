package com.smartmaps.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_categories")
data class ServiceCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,          // emoji icon
    val color: Long,           // ARGB color for UI
    val sortOrder: Int = 0
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val icon: String,
    val keywords: String,       // comma-separated keywords for search
    val synonyms: String,       // comma-separated synonyms
    val possibleWorkers: Int,
    val estimatedDurationMinMin: Int,
    val estimatedDurationMinMax: Int,
    val estimatedLabourMin: Int,  // INR
    val estimatedLabourMax: Int,  // INR
    val requiredSkills: String    // comma-separated
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessName: String,
    val phone: String,
    val email: String,
    val description: String,
    val profileImageUrl: String = "",
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val serviceRadius: Double,  // km
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val completedJobs: Int = 0,
    val isAvailable: Boolean = true,
    val isVerified: Boolean = false,
    val workingHoursStart: String = "08:00",
    val workingHoursEnd: String = "20:00",
    val workingDays: String = "Mon,Tue,Wed,Thu,Fri,Sat",
    val priceRangeMin: Int = 0,
    val priceRangeMax: Int = 0,
    val experienceYears: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "provider_services")
data class ProviderServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: String,
    val serviceId: String
)

@Entity(tableName = "places_cache")
data class PlaceCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryKey: String,
    val categoryLabel: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val openingHours: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val priceLevel: Int = 0,
    val source: String = "osm",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val providerId: String,
    val serviceId: String,
    val status: String,        // BookingStatus enum name
    val scheduledAt: Long,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val estimatedDuration: Int = 0,   // minutes
    val estimatedCost: Int = 0,       // INR
    val actualCost: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val userId: String,
    val bookingId: String,
    val rating: Float,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val senderId: String,
    val senderType: String,    // "user" | "provider"
    val content: String,
    val messageType: String = "text", // "text" | "image"
    val imageUrl: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val placeId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_providers")
data class SavedProviderEntity(
    @PrimaryKey val providerId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "local_user",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val defaultAddress: String = "",
    val defaultLatitude: Double = 0.0,
    val defaultLongitude: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val text: String,
    val intentJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
