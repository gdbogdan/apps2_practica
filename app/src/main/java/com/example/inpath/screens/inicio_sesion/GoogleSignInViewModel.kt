package com.example.inpath.screens.inicio_sesion

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.AndroidViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class GoogleSignInViewModel(
    private val auth: FirebaseAuth,
    application: Application
) : AndroidViewModel(application) {
    private val credentialManager = CredentialManager.Companion.create(getApplication())

    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun createNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val byte = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(byte)

        return digest.fold("") { str, it ->
            str + "%02x".format(it)
        }
    }


    private suspend fun buildCredentialsRequest(context: Context): GetCredentialResponse { // Añade context como parámetro
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("499532920493-qkpaav46490drk29v4i39utoushujd5d.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .setNonce(createNonce())
                    .build()
            )
            .build()
        return credentialManager.getCredential(request = request, context = context)
    }

    suspend fun handleSignIn(result: GetCredentialResponse, context: Context): Boolean {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val tokenCredential = GoogleIdTokenCredential.Companion.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                return authResult.user != null
            } catch (e: GoogleIdTokenParsingException) {
                e.printStackTrace()
                return false
            }
        } else {
            println("Credential is not GoogleIdTokenCredential")
            return false
        }
    }



    suspend fun signIn(context: Context): Boolean {
        if (isSignedIn()) return true
        return try {
            Log.d("SignIn", "Entrando al bloque SignIn()")
            val result = buildCredentialsRequest(context)
            Log.d("SignIn", "Antes de llamar a HandleSignIn")
            handleSignIn(result, context)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            false
        }

    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        auth.signOut()
    }
}