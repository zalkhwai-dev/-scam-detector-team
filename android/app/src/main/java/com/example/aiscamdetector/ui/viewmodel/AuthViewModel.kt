package com.example.aiscamdetector.ui.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.aiscamdetector.integration.ScanHistoryDbHelper
import java.security.MessageDigest

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ScanHistoryDbHelper(application)

    var fullName by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    var fullNameError by mutableStateOf<String?>(null)
        private set
    var emailError by mutableStateOf<String?>(null)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set

    fun onFullNameChange(value: String) { fullName = value; fullNameError = null }
    fun onEmailChange(value: String) { email = value; emailError = null }
    fun onPasswordChange(value: String) { password = value; passwordError = null }

    fun validateLogin(): Boolean {
        emailError = if (isEmailValid(email)) null else "Enter a valid email."
        passwordError = if (password.length >= 6) null else "Password must be at least 6 characters."
        if (emailError != null || passwordError != null) return false

        val storedHash = dbHelper.findPasswordHash(email)
        if (storedHash == null || storedHash != hash(password)) {
            passwordError = "Invalid email or password."
            return false
        }
        return true
    }

    fun validateSignup(): Boolean {
        fullNameError = if (fullName.isNotBlank()) null else "Enter your full name."
        emailError = if (isEmailValid(email)) null else "Enter a valid email."
        passwordError = if (password.length >= 6) null else "Password must be at least 6 characters."
        if (fullNameError != null || emailError != null || passwordError != null) return false

        if (dbHelper.emailExists(email)) {
            emailError = "This email is already registered."
            return false
        }

        dbHelper.createUser(fullName, email, hash(password))
        return true
    }

    private fun isEmailValid(value: String) =
        value.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(value).matches()

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}