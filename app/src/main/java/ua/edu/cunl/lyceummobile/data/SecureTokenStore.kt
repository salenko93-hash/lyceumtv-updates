package ua.edu.cunl.lyceummobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("lyceummobile_secure", Context.MODE_PRIVATE)
    private val alias = "lyceummobile_alerts_token_v1"

    fun save(token: String): Boolean {
        val clean = token.trim()
        if (clean.isEmpty()) return false
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ciphertext = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString("token", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .apply()
            true
        }.getOrDefault(false)
    }

    fun load(): String? = runCatching {
        val iv = prefs.getString("iv", null) ?: return null
        val encrypted = prefs.getString("token", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        String(
            cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)),
            Charsets.UTF_8
        )
    }.getOrNull()

    fun clear(): Boolean {
        prefs.edit().remove("iv").remove("token").apply()
        return true
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
