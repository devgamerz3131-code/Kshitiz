package com.example.data.model

/**
 * Dedicated administrator access states for Rankify.
 * - Checking: Verification against Firestore system_admins/{uid} is in progress.
 * - Admin: Verified administrator (system_admins/{uid} exists and role == "admin").
 * - NotAdmin: Verified non-admin or unauthenticated/guest session.
 * - Error: Network or verification error occurred (access denied by default).
 */
sealed interface AdminAccessState {
    data object Checking : AdminAccessState
    data object Admin : AdminAccessState
    data object NotAdmin : AdminAccessState
    data class Error(val message: String) : AdminAccessState
}
