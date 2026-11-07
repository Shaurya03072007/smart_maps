package com.smartmaps.app.di

import android.content.Context
import androidx.room.Room
import com.smartmaps.app.BuildConfig
import com.smartmaps.app.R
import com.smartmaps.app.data.local.SmartMapsDatabase
import com.smartmaps.app.data.local.dao.*
import com.smartmaps.app.data.remote.api.BackendApi
import com.smartmaps.app.data.remote.api.NominatimApi
import com.smartmaps.app.data.remote.api.OverpassApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ──────────────────────────
    // Room Database
    // ──────────────────────────
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartMapsDatabase =
        Room.databaseBuilder(context, SmartMapsDatabase::class.java, "smartmaps.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideServiceCategoryDao(db: SmartMapsDatabase) = db.serviceCategoryDao()
    @Provides fun provideServiceDao(db: SmartMapsDatabase) = db.serviceDao()
    @Provides fun provideProviderDao(db: SmartMapsDatabase) = db.providerDao()
    @Provides fun provideProviderServiceDao(db: SmartMapsDatabase) = db.providerServiceDao()
    @Provides fun providePlaceCacheDao(db: SmartMapsDatabase) = db.placeCacheDao()
    @Provides fun provideBookingDao(db: SmartMapsDatabase) = db.bookingDao()
    @Provides fun provideReviewDao(db: SmartMapsDatabase) = db.reviewDao()
    @Provides fun provideMessageDao(db: SmartMapsDatabase) = db.messageDao()
    @Provides fun provideSavedDao(db: SmartMapsDatabase) = db.savedDao()
    @Provides fun provideUserProfileDao(db: SmartMapsDatabase) = db.userProfileDao()
    @Provides fun provideChatSessionDao(db: SmartMapsDatabase) = db.chatSessionDao()
    @Provides fun provideChatHistoryDao(db: SmartMapsDatabase) = db.chatHistoryDao()

    // ──────────────────────────
    // OkHttp (No Client API Keys)
    // ──────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val reqWithUa = original.newBuilder()
                    .header("User-Agent", "SmartMaps-Android/2.0 (contact@smartmaps.app)")
                    .build()

                // Smart Backend Host Resolver: works on Physical USB (127.0.0.1), Wi-Fi LAN (192.168.29.248), and Emulator (10.0.2.2)
                if (reqWithUa.url.port == 8000) {
                    val candidateHosts = listOf(reqWithUa.url.host, "127.0.0.1", "192.168.29.248", "10.0.2.2").distinct()
                    var lastException: java.io.IOException? = null
                    for (host in candidateHosts) {
                        try {
                            val candidateUrl = reqWithUa.url.newBuilder().host(host).build()
                            val candidateReq = reqWithUa.newBuilder().url(candidateUrl).build()
                            return@addInterceptor chain.proceed(candidateReq)
                        } catch (e: java.io.IOException) {
                            lastException = e
                        }
                    }
                    throw (lastException ?: java.io.IOException("Backend unreachable on any local host"))
                }

                chain.proceed(reqWithUa)
            }
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // ──────────────────────────
    // Retrofit instances
    // ──────────────────────────
    @Provides
    @Singleton
    @Named("backend")
    fun provideBackendRetrofit(okHttpClient: OkHttpClient, @ApplicationContext context: Context): Retrofit {
        val baseUrl = context.getString(R.string.backend_base_url)
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassRetrofit(okHttpClient: OkHttpClient, @ApplicationContext context: Context): Retrofit =
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.overpass_base_url))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(okHttpClient: OkHttpClient, @ApplicationContext context: Context): Retrofit =
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.nominatim_base_url))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ──────────────────────────
    // API Services
    // ──────────────────────────
    @Provides
    @Singleton
    fun provideBackendApi(@Named("backend") retrofit: Retrofit): BackendApi =
        retrofit.create(BackendApi::class.java)

    @Provides
    @Singleton
    fun provideOverpassApi(@Named("overpass") retrofit: Retrofit): OverpassApi =
        retrofit.create(OverpassApi::class.java)

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApi =
        retrofit.create(NominatimApi::class.java)
}
