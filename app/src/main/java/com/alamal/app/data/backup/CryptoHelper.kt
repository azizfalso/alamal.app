package com.alamal.app.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption with PBKDF2-HMAC-SHA256 key derivation.
 *
 * File format (all big-endian):
 *   [0..3]   magic "ALAM" (4 bytes)
 *   [4]      version (1 byte, = 1)
 *   [5..20]  salt (16 bytes)
 *   [21..32] IV (12 bytes)
 *   [33..]   ciphertext + GCM tag
 */
object CryptoHelper {

    private val MAGIC = byteArrayOf(0x41, 0x4C, 0x41, 0x4D) // "ALAM"
    private const val VERSION: Byte = 1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_BITS = 256

    private val secureRandom = SecureRandom()

    /** True if the bytes look like our encrypted container. */
    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        for (i in 0 until 4) if (bytes[i] != MAGIC[i]) return false
        return true
    }

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LEN).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { secureRandom.nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plain)

        val out = ByteArray(4 + 1 + SALT_LEN + IV_LEN + cipherBytes.size)
        var pos = 0
        System.arraycopy(MAGIC, 0, out, pos, 4); pos += 4
        out[pos] = VERSION; pos += 1
        System.arraycopy(salt, 0, out, pos, SALT_LEN); pos += SALT_LEN
        System.arraycopy(iv, 0, out, pos, IV_LEN); pos += IV_LEN
        System.arraycopy(cipherBytes, 0, out, pos, cipherBytes.size)
        return out
    }

    /**
     * Decrypts the container. Returns plaintext bytes.
     * Throws on wrong password (AEADBadTagException) or malformed input.
     */
    fun decrypt(encrypted: ByteArray, password: String): ByteArray {
        if (encrypted.size < 4 + 1 + SALT_LEN + IV_LEN) {
            throw IllegalArgumentException("الملف المشفَّر تالف أو قصير جدًا")
        }
        for (i in 0 until 4) {
            if (encrypted[i] != MAGIC[i]) {
                throw IllegalArgumentException("الملف ليس بصيغة Alamal المشفَّرة")
            }
        }
        if (encrypted[4] != VERSION) {
            throw IllegalArgumentException("إصدار الملف غير مدعوم (${encrypted[4]})")
        }
        var pos = 5
        val salt = encrypted.copyOfRange(pos, pos + SALT_LEN); pos += SALT_LEN
        val iv = encrypted.copyOfRange(pos, pos + IV_LEN); pos += IV_LEN
        val cipherBytes = encrypted.copyOfRange(pos, encrypted.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
