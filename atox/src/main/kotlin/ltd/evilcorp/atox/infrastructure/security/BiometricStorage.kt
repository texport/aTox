package ltd.evilcorp.atox.infrastructure.security

import android.content.Context
import android.util.Base64
import ltd.evilcorp.core.profile.ProfileManager

object BiometricStorage {
    private const val PREFS_NAME_BASE = "atox_biometric_prefs"
    private const val KEY_ENCRYPTED_PASSWORD = "encrypted_password"
    private const val KEY_IV = "iv"

    private fun getPrefsName(context: Context): String {
        val profileId = ProfileManager.getActiveProfileId(context)
        return "${PREFS_NAME_BASE}_$profileId"
    }

    fun saveEncryptedPassword(context: Context, encryptedPassword: ByteArray, iv: ByteArray) {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        val encryptedBase64 = Base64.encodeToString(encryptedPassword, Base64.DEFAULT)
        val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)
        prefs.edit()
            .putString(KEY_ENCRYPTED_PASSWORD, encryptedBase64)
            .putString(KEY_IV, ivBase64)
            .apply()
    }

    fun getEncryptedPassword(context: Context): ByteArray? {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        val base64 = prefs.getString(KEY_ENCRYPTED_PASSWORD, null) ?: return null
        return Base64.decode(base64, Base64.DEFAULT)
    }

    fun getIv(context: Context): ByteArray? {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        val base64 = prefs.getString(KEY_IV, null) ?: return null
        return Base64.decode(base64, Base64.DEFAULT)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getEncryptedPassword(context) != null && getIv(context) != null
    }

    @Suppress("unused")
    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
