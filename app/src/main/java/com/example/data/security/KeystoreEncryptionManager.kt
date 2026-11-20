package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreEncryptionManager {

    companion object {
        private const val KEY_ALIAS = "nexus_webrtc_signaling_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
    }

    init {
        generateSecretKeyIfNeeded()
    }

    private fun generateSecretKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )

                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
                Log.d("KeystoreEncryptionManager", "Generated AES 256-bit GCM Key in Android Keystore")
            }
        } catch (e: Exception) {
            Log.e("KeystoreEncryptionManager", "Error initializing Keystore key: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plain WebRTC SDP string or command text using AES/GCM with IV prepended
     */
    fun encryptSignalingPayload(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("KeystoreEncryptionManager", "Encryption failed: ${e.message}")
            plainText // Fallback to plain text if hardware keystore fails
        }
    }

    /**
     * Decrypts AES/GCM encrypted Base64 string payload retrieved from Firestore
     */
    fun decryptSignalingPayload(encryptedBase64: String): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return encryptedBase64

            val iv = ByteArray(IV_LENGTH)
            val cipherText = ByteArray(combined.size - IV_LENGTH)

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.d("KeystoreEncryptionManager", "Decryption fallback (plain payload): ${e.message}")
            encryptedBase64
        }
    }
}
