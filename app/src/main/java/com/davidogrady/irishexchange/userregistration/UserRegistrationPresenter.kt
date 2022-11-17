package com.davidogrady.irishexchange.userregistration

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.managers.StorageManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.util.EncryptionHelper
import com.davidogrady.irishexchange.util.FirebaseDeviceRegistration
import com.google.gson.Gson

class UserRegistrationPresenter(
    private val view: UserRegistrationContract.View,
    private val authManager: AuthManager,
    private val storageManager: StorageManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
) : UserRegistrationContract.Presenter {

    init {
        view.presenter = this
    }

    override fun registerUser(user: User, password: String, compressedImageByteArray: ByteArray) {
        view.showLoadingIndicator()
        // Firebase Authentication to create user with email and password
        authManager.registerUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                storeRegisteredUserUid()
                getDeviceRegistrationToken()
                uploadImageToFirebaseStorage(user, compressedImageByteArray)
            }
            .addOnFailureListener {
                view.showRegistrationFailureMessage(it.message.toString())
                view.hideLoadingIndicator()
            }
    }

    private fun uploadImageToFirebaseStorage(user: User, compressedImageByteArray: ByteArray) {
        storageManager.uploadImageToFirebaseStorage(
            compressedImageByteArray,
            uploadImageSuccessHandler = {
                saveUserToDatabase(user, it)
            }, uploadImageErrorHandler = {
                view.showImageUploadFailureMessage(it)
            })
    }

    private fun saveUserToDatabase(user: User, profileImageUrl: String) {
        val uid = authManager.getUidForCurrentUser()
        // generate AES unique public token
        user.uid = uid!!
        user.publicKey = generatePublicKeyAES()
        storeUserPublicKey(user.publicKey)
        databaseManager.saveUserToFirebaseDatabase(user, profileImageUrl)
            .addOnSuccessListener {
                view.showRegistrationSuccessMessage()
                view.hideLoadingIndicator()
                view.navigateToCreateProfileScreen()
            }.addOnFailureListener {
                view.showRegistrationFailureMessage(it.message!!)
            }
    }

    private fun generatePublicKeyAES(): ArrayList<Int> {

        // realtime database does not allow us to store array of bytes
        val publicKeyInts : ArrayList<Int> = arrayListOf()

        val publicKeyBytes = EncryptionHelper().generateRandomPublicKey()

        publicKeyBytes!!.encoded.forEach {
            publicKeyInts.add(it.toInt())
        }

        return publicKeyInts
    }

    private fun storeRegisteredUserUid() {
        val currentUserUid = authManager.getUidForCurrentUser()
        preferencesManager.storePreferenceKey("uid", currentUserUid!!)
    }

    private fun storeUserPublicKey(publicKey: ArrayList<Int>) {
        val publicKeyJson = Gson().toJson(publicKey)
        preferencesManager.storePreferenceKey("publicKey", publicKeyJson)
    }

    private fun getDeviceRegistrationToken() {
        FirebaseDeviceRegistration().getDeviceRegistrationToken(
            deviceRegTokenCompleteListener = { firebaseToken ->
                // if Firebase token is different from stored token then update this value in shared preferences
                // and the database
                val storedToken = preferencesManager.getStoredPreferenceString("deviceToken")
                if (firebaseToken != storedToken) {
                    preferencesManager.storePreferenceKey("deviceToken", firebaseToken)
                    updateDeviceRegistrationTokenInDatabase(firebaseToken)
                }
            })
    }

    private fun updateDeviceRegistrationTokenInDatabase(token: String) {
        val currentUserUid = authManager.getUidForCurrentUser()
        if (!currentUserUid.isNullOrEmpty())
            databaseManager.updateUserDeviceRegToken(currentUserUid, token)
    }
}


