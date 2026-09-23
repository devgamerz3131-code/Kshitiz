package com.example

import com.example.data.model.AdminAccessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminVerificationTest {

    @Test
    fun defaultAdminState_isNotAdmin() {
        val defaultState: AdminAccessState = AdminAccessState.NotAdmin
        assertFalse("Default state should not be Admin", defaultState is AdminAccessState.Admin)
    }

    @Test
    fun checkingState_doesNotGrantAdminAccess() {
        val checkingState: AdminAccessState = AdminAccessState.Checking
        assertFalse("Checking state must not grant administrator access", checkingState is AdminAccessState.Admin)
    }

    @Test
    fun errorState_doesNotGrantAdminAccess() {
        val errorState: AdminAccessState = AdminAccessState.Error("Network error")
        assertFalse("Error state must not grant administrator access", errorState is AdminAccessState.Admin)
        assertEquals("Network error", (errorState as AdminAccessState.Error).message)
    }

    @Test
    fun verifiedAdminState_grantsAdminAccess() {
        val adminState: AdminAccessState = AdminAccessState.Admin
        assertTrue("Admin state must be recognized as Admin", adminState is AdminAccessState.Admin)
    }

    @Test
    fun roleCheck_requiresStrictAdminValue() {
        fun isValidAdmin(role: String?): Boolean = (role == "admin")

        assertTrue("role == 'admin' should pass", isValidAdmin("admin"))
        assertFalse("role == 'user' should fail", isValidAdmin("user"))
        assertFalse("role == 'student' should fail", isValidAdmin("student"))
        assertFalse("role == null should fail", isValidAdmin(null))
        assertFalse("role == '' should fail", isValidAdmin(""))
        assertFalse("role == 'ADMIN' with wrong case should fail", isValidAdmin("ADMIN"))
    }
}
