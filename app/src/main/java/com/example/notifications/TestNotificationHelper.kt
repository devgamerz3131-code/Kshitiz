package com.example.notifications

import android.content.Context
import com.example.notifications.model.NotificationCategory
import com.example.notifications.model.NotificationDeepLinks
import com.example.notifications.model.NotificationPersonality
import com.example.notifications.model.SmartNotificationPayload

/**
 * Dedicated helper to instantly trigger production-grade Android system notifications
 * for all 17 developer testing categories.
 * Each method builds a real SmartNotificationPayload and dispatches it via NotificationDispatcher.
 */
object TestNotificationHelper {

    fun sendTestNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_general_${System.currentTimeMillis()}",
            title = "🤖 Ranki: System Test Active!",
            body = "Notification pipeline is 100% operational. Ready to help you conquer your syllabus!",
            category = NotificationCategory.AI_TIPS,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.NOTIFICATION_SETTINGS,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_general_ping"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendFunnyNotification(context: Context) {
        val jokes = listOf(
            Pair("🤖 Ranki: Kitab kholein?", "Phone ki battery 95% hai aur physics ka syllabus 5%. Kuch toh balance karo dost! 😂"),
            Pair("🤖 Ranki: Serious Sawal!", "Instagram reels se JEE clear hota toh sab IITian hote. Chalo notes kholein! 💀"),
            Pair("🤖 Ranki: Chemistry Calling!", "Organic chemistry aur crush mein ek similarity hai: samajh dono nahi aate jab tak dhyan na do! 🤣")
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "test_funny_${System.currentTimeMillis()}",
            title = jokes.first,
            body = jokes.second,
            category = NotificationCategory.FUNNY_ROAST,
            personality = NotificationPersonality.FUNNY,
            deepLink = NotificationDeepLinks.PRACTICE,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_funny_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendSavageNotification(context: Context) {
        val savages = listOf(
            Pair("🤖 Ranki: Reality Check 😈", "Exam calendar badal nahi sakta, par tumhara rank zaroor drop ho sakta hai agar abhi nahi padha."),
            Pair("🤖 Ranki: Ouch! ⚠️", "Competition so nahi raha hai. Woh 12 ghante padh raha hai aur tum bas notification dekh rahe ho."),
            Pair("🤖 Ranki: Excuse Expired 🛑", "'Kal se padhunga' wala kal aaj hi hai. Desk par aao abhi.")
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "test_savage_${System.currentTimeMillis()}",
            title = savages.first,
            body = savages.second,
            category = NotificationCategory.FUNNY_ROAST,
            personality = NotificationPersonality.SAVAGE,
            deepLink = NotificationDeepLinks.TODAY_TARGET,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_savage_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendMotivationNotification(context: Context) {
        val motivations = listOf(
            Pair("🤖 Ranki: Future Topper 🚀", "Small disciplines repeated with consistency every day lead to great achievements. Start your 25-minute sprint!"),
            Pair("🤖 Ranki: Every Page Counts 🌟", "One chapter today is infinitely greater than zero chapters tomorrow. Let's make it count!"),
            Pair("🤖 Ranki: Peak Performance 🔥", "Your dream university is built on the questions you solve today. Open Rankify and let's conquer!")
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "test_motivation_${System.currentTimeMillis()}",
            title = motivations.first,
            body = motivations.second,
            category = NotificationCategory.MOTIVATION,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.STUDY_SESSION,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_motivation_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendAiMentorNotification(context: Context) {
        val mentorTips = listOf(
            Pair("🤖 Ranki AI Mentor: Spaced Retrieval", "Active recall strengthens neural pathways 300% better than re-reading. Try 5 quick flashcards now!"),
            Pair("🤖 Ranki AI Mentor: The Feynman Technique", "If you can't explain a formula simply, you don't understand it yet. Teach it to Ranki in Doubt Mode!"),
            Pair("🤖 Ranki AI Mentor: Cognitive Rest Cycle", "After 50 minutes of deep problem-solving, take a 10-minute non-screen break for memory consolidation.")
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "test_ai_mentor_${System.currentTimeMillis()}",
            title = mentorTips.first,
            body = mentorTips.second,
            category = NotificationCategory.AI_MENTOR,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.AI_TUTOR,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_mentor_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendWeakChapterNotification(context: Context, chapter: String = "Wave Optics", subject: String = "Physics") {
        val payload = SmartNotificationPayload(
            notificationId = "test_weak_${System.currentTimeMillis()}",
            title = "🤖 Ranki: Weak Spot Detected 🎯",
            body = "$chapter ($subject) accuracy is currently low. Let's solve 5 targeted PYQs together to boost your confidence!",
            category = NotificationCategory.WEAK_CHAPTER,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.WEAK_CHAPTER,
            channelId = NotificationChannelsManager.CHANNEL_URGENT,
            templateKey = "test_weak_chapter"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendRevisionReminder(context: Context, chapter: String = "Electrochemistry", subject: String = "Chemistry") {
        val payload = SmartNotificationPayload(
            notificationId = "test_revision_${System.currentTimeMillis()}",
            title = "🤖 Ranki: Spaced Repetition Due 🧠",
            body = "It has been 6 days since you studied $chapter ($subject). A quick 15-minute formula scan will prevent forgetting!",
            category = NotificationCategory.REVISION,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.REVISION,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_revision"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendFormulaOfDayNotification(context: Context) {
        val formulas = listOf(
            Pair("⚡ Formula of the Day: Biot-Savart Law", "dB = (μ₀/4π) · (I dl sinθ) / r² | Crucial for Magnetic Effects of Current PYQs!"),
            Pair("🔬 Formula of the Day: Nernst Equation", "E = E° - (0.0591/n) · log(Q) at 298 K | Direct 3-mark question guaranteed!"),
            Pair("📐 Shortcut of the Day: Integration By Parts", "ILATE rule: Inverse, Logarithmic, Algebraic, Trigonometric, Exponential!")
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "test_formula_${System.currentTimeMillis()}",
            title = formulas.first,
            body = formulas.second,
            category = NotificationCategory.FORMULA_OF_DAY,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.FORMULAS,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_formula_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendQuestionOfDayNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_qotd_${System.currentTimeMillis()}",
            title = "🤖 Ranki: Question of the Day 🎯",
            body = "Q: What is the drift velocity of free electrons in a copper wire carrying 1.5 A current? Tap to solve & earn 15 SP!",
            category = NotificationCategory.QUESTION_OF_DAY,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.PRACTICE,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_qotd_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendStreakReminder(context: Context, streak: Int = 12) {
        val payload = SmartNotificationPayload(
            notificationId = "test_streak_${System.currentTimeMillis()}",
            title = "🤖 Ranki: $streak-Day Streak on the Line! 🔥",
            body = "Don't let your streak break today! Complete just 20 minutes of study or log 1 target to keep your flame burning.",
            category = NotificationCategory.STREAK,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.STREAK,
            channelId = NotificationChannelsManager.CHANNEL_ACHIEVEMENTS,
            templateKey = "test_streak"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendTargetReminder(context: Context, pending: Int = 3) {
        val payload = SmartNotificationPayload(
            notificationId = "test_target_${System.currentTimeMillis()}",
            title = "🤖 Ranki: $pending Daily Targets Pending ⏳",
            body = "Evening study window is open! You have $pending tasks left to hit 100% daily completion. Let's finish strong.",
            category = NotificationCategory.TARGET,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.TODAY_TARGET,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_target"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendAchievementNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_achievement_${System.currentTimeMillis()}",
            title = "🤖 Ranki: Milestone Unlocked! 🏆",
            body = "Boom! 100 Questions Solved with >80% accuracy! Ranki is proud of your relentless dedication. +50 SP awarded!",
            category = NotificationCategory.ACHIEVEMENT,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.LEADERBOARD,
            channelId = NotificationChannelsManager.CHANNEL_ACHIEVEMENTS,
            templateKey = "test_achievement"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendExamCountdownNotification(context: Context, examName: String = "CBSE Board Exams", days: Long = 18) {
        val payload = SmartNotificationPayload(
            notificationId = "test_exam_${System.currentTimeMillis()}",
            title = "🤖 Ranki: $days Days Until $examName ⏰",
            body = "Every single hour invested now translates directly into rank points. Review your syllabus checklist today!",
            category = NotificationCategory.EXAM_COUNTDOWN,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.EXAM_PLANNER,
            channelId = NotificationChannelsManager.CHANNEL_EXAMS,
            templateKey = "test_exam_countdown"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendComebackReminder(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_comeback_${System.currentTimeMillis()}",
            title = "🤖 Ranki: We Miss You! Welcome Back 🤝",
            body = "Taking a break is part of the journey, but restarting is where champions are made. Let's do a 10-minute warm-up!",
            category = NotificationCategory.COMEBACK,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.STUDY_SESSION,
            channelId = NotificationChannelsManager.CHANNEL_URGENT,
            templateKey = "test_comeback"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendRankifyExclusiveNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_exclusive_${System.currentTimeMillis()}",
            title = "🤖 Ranki Exclusive: Supercharged Lecture Drop 💎",
            body = "Exclusive Class 12 Masterclass & handwritten formula cheat-sheet are now unlocked in your vault!",
            category = NotificationCategory.RANKIFY_EXCLUSIVE,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.EXCLUSIVE,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_exclusive"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendSongReleasedNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_song_${System.currentTimeMillis()}",
            title = "🤖 Ranki Music Vault: 432Hz Alpha Focus Beats 🎵",
            body = "New binaural study track unlocked! Put on your headphones and enter deep flow state without mental fatigue.",
            category = NotificationCategory.NEW_SONGS,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.MUSIC_VAULT,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_song"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    fun sendCommunityUpdateNotification(context: Context) {
        val payload = SmartNotificationPayload(
            notificationId = "test_community_${System.currentTimeMillis()}",
            title = "🤖 Ranki Leaderboard: Peer Sprint Active! 👥",
            body = "Toppers in your batch have completed 3 hours today. Check the live leaderboard and climb up the ranks!",
            category = NotificationCategory.COMMUNITY_ACTIVITY,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.COMMUNITY,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "test_community"
        )
        NotificationDispatcher.showNotification(context, payload)
    }
}
