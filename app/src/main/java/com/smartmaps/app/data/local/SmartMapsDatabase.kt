package com.smartmaps.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartmaps.app.data.local.dao.*
import com.smartmaps.app.data.local.entity.*

@Database(
    entities = [
        ServiceCategoryEntity::class,
        ServiceEntity::class,
        ProviderEntity::class,
        ProviderServiceEntity::class,
        PlaceCacheEntity::class,
        BookingEntity::class,
        ReviewEntity::class,
        MessageEntity::class,
        SavedPlaceEntity::class,
        SavedProviderEntity::class,
        UserProfileEntity::class,
        ChatSessionEntity::class,
        ChatHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SmartMapsDatabase : RoomDatabase() {
    abstract fun serviceCategoryDao(): ServiceCategoryDao
    abstract fun serviceDao(): ServiceDao
    abstract fun providerDao(): ProviderDao
    abstract fun providerServiceDao(): ProviderServiceDao
    abstract fun placeCacheDao(): PlaceCacheDao
    abstract fun bookingDao(): BookingDao
    abstract fun reviewDao(): ReviewDao
    abstract fun messageDao(): MessageDao
    abstract fun savedDao(): SavedDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatHistoryDao(): ChatHistoryDao
}
