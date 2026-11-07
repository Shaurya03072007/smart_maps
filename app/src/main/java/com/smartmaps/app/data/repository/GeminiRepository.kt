package com.smartmaps.app.data.repository

import com.smartmaps.app.data.local.dao.ServiceDao
import com.smartmaps.app.data.remote.api.BackendApi
import com.smartmaps.app.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agentic AI & Beast Search Repository.
 * Routes all AI chat (Google ADK) and Beast Search requests to the SmartMaps Backend Platform.
 * Zero API keys are stored on the Android client.
 * Includes a deterministic local ADK fallback so the app works even if offline.
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val providerRepository: ProviderRepository,
    private val placeRepository: PlaceRepository,
    private val serviceDao: ServiceDao
) {

    // Session memory cache for offline fallback continuity
    private val localSessionVendors = mutableMapOf<String, List<ProviderInfo>>()
    private val localSessionIntent = mutableMapOf<String, IntentResult>()

    // ─────────────────────────────────────────────────────────────
    // Google ADK Stateful Agentic Chat (Backend Primary + Local Fallback)
    // ─────────────────────────────────────────────────────────────
    suspend fun chatWithAdk(
        sessionId: String?,
        userMessage: String,
        latLng: LatLng
    ): Result<BackendChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.chat(
                BackendChatRequest(
                    sessionId = sessionId,
                    userId = "local_user",
                    message = userMessage,
                    latitude = latLng.lat,
                    longitude = latLng.lng
                )
            )
            localSessionVendors[response.sessionId] = response.suggestedProviders
            response.intentResult?.let { localSessionIntent[response.sessionId] = it }
            Result.success(response)
        } catch (e: Exception) {
            // Instant local ADK fallback if backend server is unreachable
            val sid = sessionId ?: UUID.randomUUID().toString()
            val fallback = runLocalAgentFallback(sid, userMessage, latLng)
            Result.success(fallback)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Beast-Level Unified Hybrid Search (Backend Primary + Local Instant Engine)
    // ─────────────────────────────────────────────────────────────
    suspend fun beastSearch(
        query: String,
        category: String? = null,
        tab: String = "ALL",
        latLng: LatLng,
        radiusKm: Double = 10.0,
        sortByDistance: Boolean = false
    ): Result<BackendSearchResponse> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        try {
            val response = backendApi.beastSearch(
                BackendSearchRequest(
                    query = query,
                    category = category,
                    tab = tab,
                    latitude = latLng.lat,
                    longitude = latLng.lng,
                    radiusKm = radiusKm,
                    sortByDistance = sortByDistance,
                    limit = 30
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            // Instant local hybrid search (<15ms)
            val intent = analyzeIntentLocally(query.ifBlank { category ?: "" })
            val vendors = if (tab == "ALL" || tab == "SERVICES") {
                providerRepository.searchNearbyProviders(
                    userLatLng = latLng,
                    serviceId = intent.service,
                    query = query,
                    radiusKm = radiusKm,
                    sortByDistance = sortByDistance
                ).getOrDefault(emptyList())
            } else emptyList()

            val places = if (tab == "ALL" || tab == "PLACES") {
                val cats = if (!category.isNullOrBlank()) listOf(category)
                else intent.placeCategories?.takeIf { it.isNotEmpty() } ?: listOf(query.ifBlank { "restaurant" })
                placeRepository.searchNearbyPlaces(
                    latLng = latLng,
                    categoryQueries = cats,
                    radiusMeters = (radiusKm * 1000).toInt()
                ).getOrDefault(emptyList())
            } else emptyList()

            val elapsed = (System.currentTimeMillis() - t0).toDouble()
            val topPick = vendors.firstOrNull()
            val summary = buildString {
                append("AI matched ${vendors.size} verified vendors & ${places.size} places in ${elapsed.toInt()} ms")
                if (topPick != null) {
                    append(" · Top Pick: ${topPick.businessName} (${topPick.matchScore}% Match)")
                }
            }
            Result.success(
                BackendSearchResponse(
                    query = query,
                    latencyMs = elapsed,
                    analysisText = summary,
                    intent = intent,
                    vendors = vendors,
                    places = places
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Fast Intent Classification
    // ─────────────────────────────────────────────────────────────
    suspend fun classifyIntent(userInput: String): Result<IntentResult> = withContext(Dispatchers.IO) {
        Result.success(analyzeIntentLocally(userInput))
    }

    private suspend fun runLocalAgentFallback(
        sessionId: String,
        message: String,
        latLng: LatLng
    ): BackendChatResponse {
        val msgLower = message.trim().lowercase()
        val prevVendors = localSessionVendors[sessionId].orEmpty()
        val prevIntent = localSessionIntent[sessionId]

        val isFollowup = prevVendors.isNotEmpty() && listOf(
            "cheaper", "cheapest", "price", "cost", "compare",
            "fastest", "closest", "nearest", "eta", "urgent",
            "why", "best", "top", "first", "experience", "book", "map"
        ).any { msgLower.contains(it) }

        val intent = if (isFollowup && prevIntent != null) prevIntent else analyzeIntentLocally(message)
        val vendors = if (intent.intent == "service_request" || isFollowup) {
            providerRepository.searchNearbyProviders(
                userLatLng = latLng,
                serviceId = intent.service,
                query = if (isFollowup) (intent.query ?: "") else message,
                radiusKm = intent.radiusKm ?: 15.0,
                sortByDistance = msgLower.contains("closest") || msgLower.contains("fastest")
            ).getOrDefault(emptyList())
        } else emptyList()

        val places = if (intent.intent == "place_search" || (vendors.isEmpty() && !intent.placeCategories.isNullOrEmpty())) {
            placeRepository.searchNearbyPlaces(
                latLng = latLng,
                categoryQueries = intent.placeCategories ?: listOf("restaurant"),
                radiusMeters = ((intent.radiusKm ?: 5.0) * 1000).toInt()
            ).getOrDefault(emptyList())
        } else emptyList()

        localSessionVendors[sessionId] = vendors
        localSessionIntent[sessionId] = intent

        val reply = when {
            isFollowup && vendors.isNotEmpty() && (msgLower.contains("cheap") || msgLower.contains("price") || msgLower.contains("cost") || msgLower.contains("compare")) -> {
                val cheapest = vendors.minByOrNull { it.priceRangeMin } ?: vendors.first()
                "💰 **Price Comparison (${intent.serviceName ?: "Service"}):**\n" +
                    "• **Most Affordable:** **${cheapest.businessName}** (${cheapest.name}) starts at **₹${cheapest.priceRangeMin}** (${cheapest.rating}★, ~${cheapest.estimatedArrivalMinutes} min ETA).\n" +
                    vendors.filter { it.id != cheapest.id }.take(2).joinToString("\n") {
                        "• **${it.businessName}**: ₹${it.priceRangeMin}–₹${it.priceRangeMax} (${it.matchScore}% AI Match · ${it.completedJobs} jobs)"
                    } + "\n\nTap **Book** or **Chat** on any vendor card below to lock in your quote!"
            }
            isFollowup && vendors.isNotEmpty() && (msgLower.contains("fast") || msgLower.contains("close") || msgLower.contains("near") || msgLower.contains("eta")) -> {
                val fastest = vendors.minByOrNull { it.distanceMeters } ?: vendors.first()
                "🛵 **Fastest Available Vendor Near You:**\n" +
                    "• **${fastest.businessName}** (${fastest.name}) is just **${"%.1f".format(fastest.distanceMeters / 1000.0)} km away** (~${fastest.estimatedArrivalMinutes} min ETA)!\n" +
                    "• **AI Match:** ${fastest.matchScore}% · **Rating:** ${fastest.rating}★ (${fastest.completedJobs} jobs)\n" +
                    "• **Estimated Rate:** ₹${fastest.priceRangeMin}–₹${fastest.priceRangeMax}\n\n" +
                    "Check the live map preview below or tap **Book** for priority dispatch!"
            }
            vendors.isNotEmpty() -> {
                val top = vendors.first()
                "🤖 **AI Smart Vendor Match for ${intent.serviceName ?: "Your Request"}:**\n" +
                    "Estimated Labour: **₹${intent.estimatedLabourMin ?: 250} – ₹${intent.estimatedLabourMax ?: 900}** (${intent.estimatedDurationMin ?: 30}–${intent.estimatedDurationMax ?: 90} mins)\n\n" +
                    "⭐ **Top Pick: ${top.businessName} (${top.matchScore}% AI Match)**\n" +
                    "• **Why chosen:** ${top.matchReason.ifBlank { "Verified specialist · ${"%.1f".format(top.distanceMeters / 1000.0)} km away" }}\n" +
                    "• **Pricing & ETA:** Starts at **₹${top.priceRangeMin}** · Arrives in **~${top.estimatedArrivalMinutes} mins**\n\n" +
                    "🗺️ Vendors are pinned on the **Smart Map** below. Tap **Book**, **Chat**, or **Call** to connect!"
            }
            places.isNotEmpty() -> {
                val topP = places.first()
                "📍 **Found ${places.size} Nearby Places on SmartMaps:**\n" +
                    "• **Closest:** **${topP.name}** (${topP.categoryLabel}) — **${"%.1f".format(topP.distanceMeters / 1000.0)} km away**\n" +
                    places.drop(1).take(2).joinToString("\n") { "• **${it.name}** — ${"%.1f".format(it.distanceMeters / 1000.0)} km · ${it.address}" }
            }
            else -> {
                "Hi! I'm your **SmartMaps AI Vendor Concierge** (powered by Google ADK). 🛠️🗺️\n\n" +
                    "Describe your problem (e.g., *\"My AC isn't cooling\"*, *\"Bathroom tap leaking\"*, *\"Need an electrician\"*, or *\"Car battery dead\"*) " +
                    "and I'll smart-match verified local vendors, estimate fair ₹ costs, and pin them on the map!"
            }
        }

        val markers = buildList {
            vendors.take(5).forEach { v ->
                add(
                    MapMarkerInfo(
                        id = v.id,
                        type = "vendor",
                        title = v.businessName,
                        subtitle = "${v.matchScore}% Match · ₹${v.priceRangeMin}+",
                        lat = v.latLng.lat,
                        lng = v.latLng.lng,
                        rating = v.rating
                    )
                )
            }
            places.take(5).forEach { p ->
                add(
                    MapMarkerInfo(
                        id = p.id,
                        type = "place",
                        title = p.name,
                        subtitle = p.categoryLabel,
                        lat = p.latLng.lat,
                        lng = p.latLng.lng,
                        rating = p.rating
                    )
                )
            }
        }

        val quickReplies = if (vendors.isNotEmpty()) {
            listOf(
                "Which vendor is cheapest?",
                "Who can arrive fastest?",
                "Why is ${vendors.first().businessName.split(" ").first()} #1?",
                "Show on Map"
            )
        } else {
            listOf(
                "My AC isn't cooling ❄️",
                "Tap is leaking 🚿",
                "Need an Electrician ⚡",
                "Car puncture / mechanic 🚗"
            )
        }

        return BackendChatResponse(
            sessionId = sessionId,
            reply = reply,
            intentResult = intent,
            suggestedProviders = vendors,
            suggestedPlaces = places,
            mapContext = MapContextInfo(
                centerLat = latLng.lat,
                centerLng = latLng.lng,
                zoom = 14.0,
                markers = markers,
                hasVendorPins = vendors.isNotEmpty(),
                hasPlacePins = places.isNotEmpty()
            ),
            quickReplies = quickReplies,
            agentEngine = "Google ADK"
        )
    }

    private fun analyzeIntentLocally(input: String): IntentResult {
        val q = input.trim().lowercase()
        val urgency = when {
            listOf("emergency", "urgent", "spark", "burst", "asap", "immediately", "now", "breakdown").any { q.contains(it) } -> "emergency"
            listOf("fast", "quick", "today", "leaking", "not cooling", "not working").any { q.contains(it) } -> "high"
            else -> "normal"
        }

        data class Rule(
            val keywords: List<String>,
            val serviceId: String,
            val serviceName: String,
            val category: String,
            val minCost: Int,
            val maxCost: Int
        )

        val rules = listOf(
            Rule(listOf("ac", "air conditioner", "cooling", "gas refill", "hvac", "split ac"), "ac_repair", "AC Repair & Gas Refill", "home_services", 350, 1200),
            Rule(listOf("tap", "plumb", "pipe", "leak", "drain", "sink", "toilet", "flush", "geyser", "water"), "plumbing_tap_repair", "Plumbing & Leak Repair", "home_services", 200, 750),
            Rule(listOf("electric", "wire", "wiring", "switch", "socket", "mcb", "spark", "fan", "inverter", "power", "light"), "electrical_wiring", "Electrician & Fault Repair", "home_services", 150, 800),
            Rule(listOf("washing machine", "washer", "fridge", "refrigerator", "microwave", "ro", "purifier", "tv", "appliance"), "washing_machine_repair", "Appliance & Washing Machine Repair", "appliance_repair", 300, 1100),
            Rule(listOf("car", "bike", "mechanic", "puncture", "tyre", "battery", "towing", "roadside", "breakdown"), "car_repair_general", "Mechanic & Roadside Assistance", "vehicle", 200, 1500),
            Rule(listOf("carpenter", "wood", "furniture", "door", "lock", "hinge", "bed", "cupboard", "wardrobe"), "carpentry_furniture", "Carpentry & Lock Repair", "home_services", 250, 1000),
            Rule(listOf("clean", "pest", "cockroach", "termite", "sofa", "sanitiz", "maid"), "cleaning_home", "Deep Cleaning & Pest Control", "home_services", 499, 2499),
            Rule(listOf("laptop", "computer", "pc", "mobile", "phone", "screen", "wifi", "cctv", "camera"), "laptop_repair", "Laptop, Mobile & CCTV Service", "technology", 300, 1500),
            Rule(listOf("salon", "haircut", "facial", "beauty", "makeup", "bridal", "spa", "massage", "wax"), "salon_haircut", "Home Salon & Wellness", "beauty_wellness", 299, 2000),
            Rule(listOf("paint", "waterproof", "wall", "tile", "ceiling"), "painting_interior", "Painting & Waterproofing", "construction", 1000, 12000)
        )

        val matchedRule = rules.firstOrNull { rule -> rule.keywords.any { q.contains(it) } }
        if (matchedRule != null) {
            return IntentResult(
                intent = "service_request",
                category = matchedRule.category,
                categoryName = matchedRule.serviceName,
                service = matchedRule.serviceId,
                serviceName = matchedRule.serviceName,
                query = input,
                problem = input,
                urgency = urgency,
                estimatedLabourMin = matchedRule.minCost,
                estimatedLabourMax = matchedRule.maxCost,
                confidence = 0.95f
            )
        }

        val placeMap = mapOf(
            "hospital" to listOf("hospital", "emergency", "medical", "clinic", "doctor"),
            "pharmacy" to listOf("pharmacy", "chemist", "medicine"),
            "restaurant" to listOf("restaurant", "food", "eat", "dinner", "lunch", "biryani", "breakfast"),
            "cafe" to listOf("cafe", "coffee", "tea", "bakery", "wifi"),
            "atm" to listOf("atm", "cash"),
            "bank" to listOf("bank"),
            "supermarket" to listOf("supermarket", "grocery", "mart", "mall"),
            "fuel" to listOf("petrol", "fuel", "gas", "ev"),
            "gym" to listOf("gym", "fitness"),
            "park" to listOf("park", "garden"),
            "cinema" to listOf("cinema", "movie", "theatre")
        )

        val matchedPlaceCats = placeMap.entries
            .filter { (k, kws) -> q.contains(k) || kws.any { q.contains(it) } }
            .map { it.key }

        if (matchedPlaceCats.isNotEmpty()) {
            return IntentResult(
                intent = "place_search",
                category = matchedPlaceCats.first(),
                placeCategories = matchedPlaceCats,
                query = input,
                confidence = 0.9f
            )
        }

        // Default to service_request so vendor discovery stays primary
        return IntentResult(
            intent = "service_request",
            category = "home_services",
            service = null,
            serviceName = "Verified Local Service Vendor",
            placeCategories = listOf("restaurant", "cafe", "hospital"),
            query = input,
            problem = input,
            urgency = urgency,
            estimatedLabourMin = 250,
            estimatedLabourMax = 900,
            confidence = 0.85f
        )
    }
}
