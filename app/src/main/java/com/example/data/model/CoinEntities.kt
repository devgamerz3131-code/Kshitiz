package com.example.data.model

import com.google.firebase.Timestamp

data class RankifyCoinWallet(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    val lastRewardDate: String = "", // yyyy-MM-dd
    val dailyCategoryTotals: Map<String, Int> = emptyMap(),
    val updatedAt: Timestamp? = null
)

data class StudyPointWallet(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    val lastRewardDate: String = "", // yyyy-MM-dd
    val dailyCategoryTotals: Map<String, Int> = emptyMap(),
    val updatedAt: Timestamp? = null,
    // Security hint fields for Firestore rules validation
    val lastTransactionId: String = "",
    val lastSourceType: String = "",
    val lastUnlockVideoId: String = ""
)

data class CoinTransaction(
    val transactionId: String = "",
    val type: String = "EARN", // "EARN", "SPEND"
    val amount: Int = 0,
    val sourceType: String = "", // "DAILY_TARGET", "STUDY_SESSION", "PRACTICE", "CHAPTER_REVISION", "STREAK_BONUS"
    val sourceId: String = "", // ID of the target, session, etc.
    val createdAt: Timestamp? = null,
    val description: String = ""
)

data class StudyPointTransaction(
    val transactionId: String = "",
    val type: String = "EARN", // "EARN", "SPEND"
    val amount: Int = 0,
    val sourceType: String = "", 
    val sourceId: String = "", 
    val createdAt: Timestamp? = null,
    val description: String = ""
)

data class SongUnlock(
    val videoId: String = "",
    val unlockedAt: Timestamp? = null,
    val pointsSpent: Int = 0,
    val transactionId: String = ""
)
