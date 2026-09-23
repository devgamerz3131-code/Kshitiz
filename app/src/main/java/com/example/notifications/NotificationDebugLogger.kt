package com.example.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app debug log repository for Rankify Smart Notifications.
 * Records all evaluation decisions, dispatches, and exact skip reasons:
 * - Skipped because target completed
 * - Skipped because user studying
 * - Skipped because quiet hours
 * - Skipped because duplicate
 * - Skipped because minimum spacing active (< 30 mins)
 * - Skipped because daily adaptive limit reached
 */
data class DebugNotificationLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val title: String,
    val details: String
) {
    enum class LogType {
        DISPATCHED,
        SKIPPED,
        SCHEDULED,
        INFO,
        ERROR
    }

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

object NotificationDebugLogger {

    private val _logs = MutableStateFlow<List<DebugNotificationLog>>(emptyList())
    val logs: StateFlow<List<DebugNotificationLog>> = _logs.asStateFlow()

    private const val MAX_LOGS = 120

    fun logSkip(reason: String, category: String = "Engine") {
        addLog(
            DebugNotificationLog(
                type = DebugNotificationLog.LogType.SKIPPED,
                title = "Skipped ($category)",
                details = reason
            )
        )
    }

    fun logDispatch(title: String, category: String, personality: String) {
        addLog(
            DebugNotificationLog(
                type = DebugNotificationLog.LogType.DISPATCHED,
                title = "Dispatched: $title",
                details = "Category: $category • Vibe: $personality"
            )
        )
    }

    fun logScheduled(info: String) {
        addLog(
            DebugNotificationLog(
                type = DebugNotificationLog.LogType.SCHEDULED,
                title = "Scheduler Event",
                details = info
            )
        )
    }

    fun logInfo(title: String, details: String) {
        addLog(
            DebugNotificationLog(
                type = DebugNotificationLog.LogType.INFO,
                title = title,
                details = details
            )
        )
    }

    fun logError(title: String, error: String) {
        addLog(
            DebugNotificationLog(
                type = DebugNotificationLog.LogType.ERROR,
                title = title,
                details = error
            )
        )
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(entry: DebugNotificationLog) {
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > MAX_LOGS) {
            _logs.value = current.take(MAX_LOGS)
        } else {
            _logs.value = current
        }
    }
}
