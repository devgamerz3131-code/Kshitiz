package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Result of attempting to launch an external AI application.
 */
data class ExternalAiLaunchResult(
    val assistantName: String,
    val directSharingAttempted: Boolean = false,
    val appOpenedDirectly: Boolean,
    val promptPrefilledInApp: Boolean,
    val promptCopiedToClipboard: Boolean,
    val messageToStudent: String
)

/**
 * Helper for generating standardized Class 12 CBSE lecture study prompts
 * and launching external AI assistants (ChatGPT or Gemini) with zero cost
 * and maximum privacy protection.
 */
object ExternalAiPromptHelper {

    const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val CHATGPT_WEB_URL = "https://chatgpt.com/"
    const val GEMINI_WEB_URL = "https://gemini.google.com/app"

    /**
     * Builds the personalized Class 12 CBSE lecture study prompt.
     *
     * Adheres strictly to the required template:
     * - Includes validated YouTube lecture URL.
     * - Includes Subject and Chapter only when selected (omits rather than inserting null/fake values).
     * - Never includes student personal data (marks, email, name, private notes).
     * - Explicitly instructs the AI to report if actual video analysis is unavailable.
     */
    fun buildLecturePrompt(
        youtubeUrl: String,
        subject: String?,
        chapter: String?
    ): String {
        val sb = StringBuilder()
        sb.append("I am a Class 12 CBSE student.\n\n")
        sb.append("Please analyze this YouTube lecture:\n")
        sb.append(youtubeUrl.trim()).append("\n\n")

        val hasSubject = !subject.isNullOrBlank()
        val hasChapter = !chapter.isNullOrBlank()

        if (hasSubject) {
            sb.append("Subject: ").append(subject.trim()).append("\n")
        }
        if (hasChapter) {
            sb.append("Chapter: ").append(chapter.trim()).append("\n")
        }
        if (hasSubject || hasChapter) {
            sb.append("\n")
        }

        sb.append("If you can access and understand the actual video, please provide:\n")
        sb.append("1. A detailed Hinglish summary of the lecture.\n")
        sb.append("2. Important concepts explained in simple language.\n")
        sb.append("3. Important formulas and derivations covered in the lecture.\n")
        sb.append("4. Topic-wise timestamps, only if they can be verified.\n")
        sb.append("5. Important revision points.\n")
        sb.append("6. Five practice questions with answers and step-by-step solutions.\n\n")
        sb.append("Explain everything at Class 12 CBSE level.\n")
        sb.append("Keep scientific terminology and formulas in English.\n\n")
        sb.append("If you cannot access or analyze the actual video, tell me clearly. Do not invent the lecture's content, formulas, explanations or timestamps.")

        return sb.toString()
    }

    /**
     * Copies the prompt text securely to the Android clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "StudyDock Lecture Prompt"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText(label, text)
                clipboard.setPrimaryClip(clip)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inspects whether the target installed package has an exported Activity
     * capable of receiving Android ACTION_SEND intents with "text/plain".
     */
    fun findSendTextActivity(context: Context, targetPackage: String, text: String): android.content.ComponentName? {
        val pm = context.packageManager
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(targetPackage)
        }

        return try {
            val defaultList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    sendIntent,
                    android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(sendIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            }

            val list = if (defaultList.isNotEmpty()) {
                defaultList
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(
                        sendIntent,
                        android.content.pm.PackageManager.ResolveInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(sendIntent, 0)
                }
            }

            // Must belong to targetPackage and preferably be exported to prevent SecurityException
            val matched = list.firstOrNull {
                it.activityInfo != null &&
                it.activityInfo.packageName == targetPackage &&
                it.activityInfo.exported
            } ?: list.firstOrNull {
                it.activityInfo != null &&
                it.activityInfo.packageName == targetPackage
            }

            matched?.let { android.content.ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Launches the official external AI assistant using proper Android text sharing (ACTION_SEND),
     * or gracefully falls back to copying the prompt to clipboard and opening the app or website.
     */
    fun launchAssistant(
        context: Context,
        targetPackage: String,
        assistantName: String,
        webFallbackUrl: String,
        prompt: String
    ): ExternalAiLaunchResult {
        // Step 1: Always copy the complete lecture prompt to clipboard as a reliable guarantee
        val copied = copyToClipboard(context, prompt, "$assistantName Lecture Prompt")

        val pm = context.packageManager

        // Step 2: Query whether the installed app supports Android ACTION_SEND for text/plain
        val sendComponent = findSendTextActivity(context, targetPackage, prompt)
        if (sendComponent != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, prompt)
                component = sendComponent
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(shareIntent)
                return ExternalAiLaunchResult(
                    assistantName = assistantName,
                    directSharingAttempted = true,
                    appOpenedDirectly = true,
                    promptPrefilledInApp = true,
                    promptCopiedToClipboard = copied,
                    messageToStudent = "Sharing lecture prompt with $assistantName..."
                )
            } catch (e: Exception) {
                // If direct sharing failed at runtime (e.g. security violation), fall back
            }
        }

        // Step 3: Check if the app is installed, open via launcher intent with clipboard fallback
        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            return try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ExternalAiLaunchResult(
                    assistantName = assistantName,
                    directSharingAttempted = false,
                    appOpenedDirectly = true,
                    promptPrefilledInApp = false,
                    promptCopiedToClipboard = copied,
                    messageToStudent = "Prompt copied! Long-press the chat box and tap Paste."
                )
            } catch (e: Exception) {
                fallbackOpenWeb(context, webFallbackUrl, assistantName, copied)
            }
        }

        // Step 4: Web fallback
        return fallbackOpenWeb(context, webFallbackUrl, assistantName, copied)
    }

    /**
     * Launches the official ChatGPT application with prompt text sharing via ACTION_SEND
     * if supported; otherwise copies to clipboard and opens the app or web fallback.
     */
    fun launchChatGPT(
        context: Context,
        prompt: String
    ): ExternalAiLaunchResult {
        return launchAssistant(
            context = context,
            targetPackage = CHATGPT_PACKAGE,
            assistantName = "ChatGPT",
            webFallbackUrl = CHATGPT_WEB_URL,
            prompt = prompt
        )
    }

    /**
     * Launches the official Gemini application with prompt text sharing via ACTION_SEND
     * if supported; otherwise copies to clipboard and opens the app or web fallback.
     */
    fun launchGemini(
        context: Context,
        prompt: String
    ): ExternalAiLaunchResult {
        return launchAssistant(
            context = context,
            targetPackage = GEMINI_PACKAGE,
            assistantName = "Gemini",
            webFallbackUrl = GEMINI_WEB_URL,
            prompt = prompt
        )
    }

    /**
     * Safely opens an external URL using ACTION_VIEW.
     */
    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens original YouTube video in the official YouTube app or browser.
     */
    fun openYouTubeLecture(context: Context, youtubeUrl: String, videoId: String): Boolean {
        // Try native YouTube intent first: vnd.youtube:VIDEO_ID
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                return true
            }
        } catch (ignored: Exception) {}

        // Fallback to web URL intent
        return openExternalUrl(context, youtubeUrl)
    }

    private fun fallbackOpenWeb(
        context: Context,
        webUrl: String,
        assistantName: String,
        copied: Boolean
    ): ExternalAiLaunchResult {
        openExternalUrl(context, webUrl)
        val message = if (copied) {
            "Prompt copied! Long-press the chat box and tap Paste."
        } else {
            "Opening $assistantName website..."
        }
        return ExternalAiLaunchResult(
            assistantName = assistantName,
            directSharingAttempted = false,
            appOpenedDirectly = false,
            promptPrefilledInApp = false,
            promptCopiedToClipboard = copied,
            messageToStudent = message
        )
    }
}
