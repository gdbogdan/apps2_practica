package com.example.inpath.screens.inicio_sesion

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

class GoogleSignInViewModelFactory(
    private val auth: FirebaseAuth,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoogleSignInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoogleSignInViewModel(auth, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}