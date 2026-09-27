// Signs the user in and out with their Google account and tracks who is signed in.
package com.example.purincar.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.purincar.R
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.network.toUserMessage
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val appContext: Context,
    private val auth: FirebaseAuth,
    scope: CoroutineScope
) {
    private val credentials = CredentialManager.create(appContext)

    val userId: StateFlow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser?.uid)

    // Returns the signed-in user's id right now.
    fun currentUserId(): String? = auth.currentUser?.uid

    // Shows the Google account picker and signs into Firebase with the chosen account; null means the user backed out.
    suspend fun signIn(activityContext: Context): PurinCarResult<Unit>? =
        try {
            val option = GetSignInWithGoogleOption.Builder(appContext.getString(R.string.default_web_client_id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentials.getCredential(activityContext, request).credential
            check(
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "Unexpected credential type" }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            PurinCarResult.Success(Unit)
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: NoCredentialException) {
            PurinCarResult.Error(appContext.getString(R.string.sign_in_no_account))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            PurinCarResult.Error(e.toUserMessage())
        }

    // Signs out of Firebase and forgets the chosen Google account.
    suspend fun signOut() {
        auth.signOut()
        try {
            credentials.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't clear saved Google credential", e)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
