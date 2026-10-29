package se.duokraft.admin.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Stores the WordPress *application password* (never the user's real password),
 * encrypted with a key held in the Android Keystore.
 */
class SessionStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "dk_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    data class Credentials(val userLogin: String, val appPassword: String)

    val credentials: Credentials?
        get() {
            val user = prefs.getString(KEY_USER, null) ?: return null
            val pass = prefs.getString(KEY_PASS, null) ?: return null
            return Credentials(user, pass)
        }

    val isLoggedIn: Boolean get() = credentials != null

    fun save(userLogin: String, appPassword: String) {
        prefs.edit().putString(KEY_USER, userLogin).putString(KEY_PASS, appPassword).apply()
    }

    /** One-time value that ties the browser login to this app (prevents forged callbacks). */
    fun newLoginState(): String {
        val state = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_STATE, state).apply()
        return state
    }

    fun consumeLoginState(): String? {
        val state = prefs.getString(KEY_STATE, null)
        prefs.edit().remove(KEY_STATE).apply()
        return state
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // Emitted when the website rejects the stored login (revoked or expired).
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired

    fun markExpired() {
        if (isLoggedIn) {
            clear()
            _expired.tryEmit(Unit)
        }
    }

    private companion object {
        const val KEY_USER = "user_login"
        const val KEY_PASS = "app_password"
        const val KEY_STATE = "login_state"
    }
}
