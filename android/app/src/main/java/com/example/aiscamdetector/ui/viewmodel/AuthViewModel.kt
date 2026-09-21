package com.example.aiscamdetector.ui.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

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
        return emailError == null && passwordError == null
    }

    fun validateSignup(): Boolean {
        fullNameError = if (fullName.isNotBlank()) null else "Enter your full name."
        emailError = if (isEmailValid(email)) null else "Enter a valid email."
        passwordError = if (password.length >= 6) null else "Password must be at least 6 characters."
        return fullNameError == null && emailError == null && passwordError == null
    }

    private fun isEmailValid(value: String) =
        value.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(value).matches()
}
