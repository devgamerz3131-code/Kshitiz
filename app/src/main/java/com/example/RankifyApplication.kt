package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class RankifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ensureFirebase(this)
        com.example.notifications.NotificationChannelsManager.setupNotificationChannels(this)
        val prefs = com.example.notifications.NotificationPreferencesManager.getInstance(this).getPreferences()
        if (prefs.masterEnabled) {
            com.example.notifications.NotificationScheduler.schedulePeriodicWork(this)
        }
    }

    companion object {
        private const val TAG = "RankifyApplication"

        var appContext: Context? = null
            private set

        var initializationError: String? = null
            private set

        /**
         * Ensures Firebase default app is initialized safely.
         * First checks if FirebaseInitProvider already initialized the default app.
         * If not, calls FirebaseApp.initializeApp with automatic resource detection,
         * followed by manual resource resolution fallback if needed.
         */
        fun ensureFirebase(context: Context): Boolean {
            val appCtx = context.applicationContext ?: context
            appContext = appCtx

            return try {
                if (FirebaseApp.getApps(appCtx).isEmpty()) {
                    Log.d(TAG, "No default FirebaseApp found. Attempting explicit initialization...")
                    var app = FirebaseApp.initializeApp(appCtx)
                    if (app == null) {
                        Log.w(TAG, "FirebaseApp.initializeApp(context) returned null. Attempting explicit FirebaseOptions from resources...")
                        val res = appCtx.resources
                        val pkg = appCtx.packageName

                        val appIdId = res.getIdentifier("google_app_id", "string", pkg)
                        val apiKeyId = res.getIdentifier("google_api_key", "string", pkg)
                        val projectIdId = res.getIdentifier("project_id", "string", pkg)
                        val storageBucketId = res.getIdentifier("google_storage_bucket", "string", pkg)
                        val senderIdId = res.getIdentifier("gcm_defaultSenderId", "string", pkg)

                        val appId = if (appIdId != 0) res.getString(appIdId) else null
                        val apiKey = if (apiKeyId != 0) res.getString(apiKeyId) else null
                        val projectId = if (projectIdId != 0) res.getString(projectIdId) else null
                        val storageBucket = if (storageBucketId != 0) res.getString(storageBucketId) else null
                        val senderId = if (senderIdId != 0) res.getString(senderIdId) else null

                        if (!appId.isNullOrEmpty() && !apiKey.isNullOrEmpty()) {
                            val optionsBuilder = FirebaseOptions.Builder()
                                .setApplicationId(appId)
                                .setApiKey(apiKey)
                            if (!projectId.isNullOrEmpty()) optionsBuilder.setProjectId(projectId)
                            if (!storageBucket.isNullOrEmpty()) optionsBuilder.setStorageBucket(storageBucket)
                            if (!senderId.isNullOrEmpty()) optionsBuilder.setGcmSenderId(senderId)

                            app = FirebaseApp.initializeApp(appCtx, optionsBuilder.build())
                        }
                    }

                    if (app == null) {
                        initializationError = "Firebase initialization failed: required configuration values (google_app_id/google_api_key) missing."
                        Log.e(TAG, initializationError!!)
                        false
                    } else {
                        initializationError = null
                        Log.i(TAG, "FirebaseApp initialized successfully: ${app.name}")
                        true
                    }
                } else {
                    initializationError = null
                    Log.d(TAG, "Default FirebaseApp already initialized.")
                    true
                }
            } catch (e: Exception) {
                initializationError = "Firebase initialization failed: ${e.localizedMessage ?: e.message}"
                Log.e(TAG, "Failed to initialize FirebaseApp in process ${appCtx.packageName}", e)
                false
            }
        }
    }
}
