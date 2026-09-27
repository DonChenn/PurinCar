// Keeps the Smartcar access and refresh tokens on the device, encrypted with a key only this app can use.
package com.example.purincar.data.smartcar

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.smartcarTokens: DataStore<Preferences> by preferencesDataStore(name = "smartcar_tokens")

class SmartcarTokenStore(context: Context) {
    private val store = context.smartcarTokens

    // Reads the saved access token, if Smartcar is connected.
    suspend fun accessToken(): String? = read(ACCESS_TOKEN)

    // Reads the saved refresh token, if Smartcar is connected.
    suspend fun refreshToken(): String? = read(REFRESH_TOKEN)

    // Saves a fresh pair of tokens, keeping the old refresh token if Smartcar didn't send a new one.
    suspend fun save(accessToken: String, refreshToken: String?) {
        store.edit { prefs ->
            prefs[ACCESS_TOKEN] = encrypt(accessToken)
            refreshToken?.let { prefs[REFRESH_TOKEN] = encrypt(it) }
        }
    }

    // Forgets both tokens.
    suspend fun clear() {
        store.edit { it.clear() }
    }

    // Reads and decrypts one token, returning null if it's missing or can't be decrypted.
    private suspend fun read(key: Preferences.Key<String>): String? {
        val stored = store.data.first()[key] ?: return null
        return try {
            decrypt(stored)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't decrypt a saved Smartcar token", e)
            null
        }
    }

    // Encrypts text with the app's key, storing the random IV in front of the ciphertext.
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // Decrypts text written by encrypt.
    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    // Loads the app's AES key from the Android Keystore, creating it the first time.
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val TAG = "SmartcarTokenStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "purincar_smartcar_tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
