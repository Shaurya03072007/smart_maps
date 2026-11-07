package com.smartmaps.app.data.repository

import com.google.gson.Gson
import com.smartmaps.app.data.local.dao.ChatHistoryDao
import com.smartmaps.app.data.local.dao.ChatSessionDao
import com.smartmaps.app.data.local.entity.ChatHistoryEntity
import com.smartmaps.app.data.local.entity.ChatSessionEntity
import com.smartmaps.app.data.remote.model.ChatMessage
import com.smartmaps.app.data.remote.model.IntentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val sessionDao: ChatSessionDao,
    private val historyDao: ChatHistoryDao
) {
    private val gson = Gson()

    fun getAllSessions(): Flow<List<ChatSessionEntity>> = sessionDao.getAllSessions()

    fun getHistoryForSession(sessionId: String): Flow<List<ChatHistoryEntity>> =
        historyDao.getHistoryForSession(sessionId)

    suspend fun getOrCreateTodaySession(): ChatSessionEntity = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        val latest = sessionDao.getLatestSessionSince(startOfDay)
        if (latest != null) {
            latest
        } else {
            val newSession = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                title = "New Conversation"
            )
            sessionDao.insertSession(newSession)
            newSession
        }
    }

    suspend fun createNewSession(): ChatSessionEntity = withContext(Dispatchers.IO) {
        val newSession = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = "New Conversation"
        )
        sessionDao.insertSession(newSession)
        newSession
    }

    suspend fun addMessage(
        sessionId: String,
        role: String,
        text: String,
        intentResult: IntentResult? = null,
        messageId: String = UUID.randomUUID().toString()
    ) = withContext(Dispatchers.IO) {
        // If this is the very first user message, rename the session based on it
        if (role == "user") {
            val session = sessionDao.getSessionById(sessionId)
            if (session != null && session.title == "New Conversation") {
                val shortTitle = if (text.length > 25) "${text.take(25)}..." else text
                sessionDao.insertSession(session.copy(title = shortTitle))
            }
        }

        val entity = ChatHistoryEntity(
            id = messageId,
            sessionId = sessionId,
            role = role,
            text = text,
            intentJson = intentResult?.let { gson.toJson(it) }
        )
        historyDao.insertMessage(entity)
        sessionDao.updateSessionTime(sessionId)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        historyDao.deleteHistoryForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }
}
