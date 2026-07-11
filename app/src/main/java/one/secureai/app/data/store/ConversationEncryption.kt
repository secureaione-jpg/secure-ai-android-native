package one.secureai.app.data.store

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ConversationEncryption {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128
    private val SALT = "secure-ai-e2e".toByteArray(Charsets.UTF_8)

    private val keyCache = mutableMapOf<String, SecretKey>()
    private val lock = Any()

    private fun deriveKey(uid: String): SecretKey = synchronized(lock) {
        keyCache.getOrPut(uid) {
            val spec = PBEKeySpec(uid.toCharArray(), SALT, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val raw = factory.generateSecret(spec).encoded
            SecretKeySpec(raw, "AES")
        }
    }

    fun encrypt(plaintext: String, uid: String): String {
        val key = deriveKey(uid)
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String, uid: String): String {
        val key = deriveKey(uid)
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
