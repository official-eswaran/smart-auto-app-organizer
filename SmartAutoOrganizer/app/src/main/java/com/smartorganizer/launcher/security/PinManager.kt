package com.smartorganizer.launcher.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages per-folder PIN storage using EncryptedSharedPreferences.
 * PINs are stored as SHA-256 hashes — never in plain text.
 */
@Singleton
class PinManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "smart_organizer_pins",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun key(folderId: Long) = "pin_$folderId"
    private fun lockedKey(folderId: Long) = "locked_$folderId"

    /** Returns true if this folder has a PIN set. */
    fun isLocked(folderId: Long): Boolean =
        prefs.getBoolean(lockedKey(folderId), false)

    /** Set or update the PIN for a folder. */
    fun setPin(folderId: Long, pin: String) {
        prefs.edit()
            .putString(key(folderId), hash(pin))
            .putBoolean(lockedKey(folderId), true)
            .apply()
    }

    /** Remove the PIN lock from a folder. */
    fun clearPin(folderId: Long) {
        prefs.edit()
            .remove(key(folderId))
            .putBoolean(lockedKey(folderId), false)
            .apply()
    }

    /** Returns true if the provided PIN matches the stored hash. */
    fun verifyPin(folderId: Long, pin: String): Boolean {
        val stored = prefs.getString(key(folderId), null) ?: return false
        return stored == hash(pin)
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
