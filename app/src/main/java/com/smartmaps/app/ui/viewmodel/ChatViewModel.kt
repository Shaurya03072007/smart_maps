package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartmaps.app.data.local.entity.ChatSessionEntity
import com.smartmaps.app.data.location.LocationRepository
import com.smartmaps.app.data.remote.model.*
import com.smartmaps.app.data.repository.BookingRepository
import com.smartmaps.app.data.repository.ChatRepository
import com.smartmaps.app.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSessionEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val suggestedPlaces: List<Place> = emptyList(),
    val suggestedProviders: List<ProviderInfo> = emptyList(),
    val currentIntentResult: IntentResult? = null,
    val mapContext: MapContextInfo? = null,
    val quickReplies: List<String> = listOf(
        "My AC isn't cooling ❄️",
        "Bathroom tap is leaking 🚿",
        "Need an Electrician ⚡",
        "Car breakdown / Puncture 🚗",
        "Home deep cleaning 🧽"
    ),
    val error: String? = null,
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "Active Session",
    val agentEngine: String = "Google ADK"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val locationRepository: LocationRepository,
    private val chatRepository: ChatRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private val gson = Gson()
    private var historyObserverJob: Job? = null

    init {
        observeAllSessions()
        loadInitialSession()
    }

    private fun observeAllSessions() {
        viewModelScope.launch {
            chatRepository.getAllSessions().collect { sessions ->
                val curId = _uiState.value.currentSessionId
                val curTitle = sessions.find { it.id == curId }?.title ?: "SmartMaps ADK Session"
                _uiState.update { it.copy(sessions = sessions, currentSessionTitle = curTitle) }
            }
        }
    }

    private fun loadInitialSession() {
        viewModelScope.launch {
            val session = chatRepository.getOrCreateTodaySession()
            observeSessionHistory(session.id, session.title)
        }
    }

    fun selectSession(sessionId: String) {
        val sess = _uiState.value.sessions.find { it.id == sessionId }
        observeSessionHistory(sessionId, sess?.title ?: "Conversation")
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                startNewSession()
            }
        }
    }

    private fun observeSessionHistory(sessionId: String, title: String = "New Conversation") {
        historyObserverJob?.cancel()
        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                currentSessionTitle = title,
                suggestedProviders = emptyList(),
                suggestedPlaces = emptyList(),
                mapContext = null
            )
        }
        historyObserverJob = viewModelScope.launch {
            chatRepository.getHistoryForSession(sessionId).collect { historyList ->
                if (historyList.isEmpty()) {
                    val welcomeMsg = ChatMessage(
                        id = "welcome_$sessionId",
                        role = "model",
                        text = "Hi! I'm your **SmartMaps AI Vendor Concierge** (powered by **Google ADK**). 🛠️🗺️\n\n" +
                            "Describe your problem or service need and I will:\n" +
                            "• **Smart-match verified local vendors** with AI Match %\n" +
                            "• **Estimate fair labour costs** in ₹ (INR)\n" +
                            "• **Pin vendors live on the map** & connect you via chat or booking!\n\n" +
                            "Try asking: *\"My AC isn't cooling\"* or *\"Need a plumber urgently\"*."
                    )
                    _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
                } else {
                    val messages = historyList.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            role = entity.role,
                            text = entity.text,
                            timestamp = entity.timestamp,
                            intentResult = entity.intentJson?.let {
                                runCatching { gson.fromJson(it, IntentResult::class.java) }.getOrNull()
                            }
                        )
                    }
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendQuickReply(prompt: String) {
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val sessionId = _uiState.value.currentSessionId
        addUserMessage(text)
        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            val userLatLng = locationRepository.currentLocation.value ?: LatLng(17.3850, 78.4867)

            geminiRepository.chatWithAdk(
                sessionId = sessionId,
                userMessage = text,
                latLng = userLatLng
            ).onSuccess { adkResp ->
                addModelMessage(adkResp.reply, adkResp.intentResult)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestedProviders = adkResp.suggestedProviders,
                        suggestedPlaces = adkResp.suggestedPlaces,
                        currentIntentResult = adkResp.intentResult,
                        mapContext = adkResp.mapContext,
                        quickReplies = adkResp.quickReplies.ifEmpty { it.quickReplies },
                        agentEngine = adkResp.agentEngine
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "AI Concierge error: ${err.message}"
                    )
                }
            }
        }
    }

    fun openVendorDirectChat(providerId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bookingId = bookingRepository.getOrCreateDirectInquiryBooking(providerId)
            onReady(bookingId)
        }
    }

    private fun addUserMessage(text: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            chatRepository.addMessage(sessionId, "user", text)
        }
    }

    private fun addModelMessage(text: String, intentResult: IntentResult? = null) {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            chatRepository.addMessage(sessionId, "model", text, intentResult)
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val session = chatRepository.createNewSession()
            observeSessionHistory(session.id, session.title)
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(suggestedPlaces = emptyList(), suggestedProviders = emptyList(), mapContext = null) }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
