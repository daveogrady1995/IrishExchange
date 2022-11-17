package com.davidogrady.irishexchange.util

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptionHelper {

    fun encrypt(
        plaintext: ByteArray?,
        key: ByteArray
    ): ByteArray? {
        try {
            val cipher = Cipher.getInstance("AES")
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            return cipher.doFinal(plaintext)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun decrypt(
        cipherText: ByteArray?,
        key: ByteArray
    ): String? {
        try {
            val cipher = Cipher.getInstance("AES")
            val keySpec =
                SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decryptedText = cipher.doFinal(cipherText)
            return String(decryptedText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun generateRandomPublicKey() : SecretKey? {
        val keyGenerator: KeyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

}