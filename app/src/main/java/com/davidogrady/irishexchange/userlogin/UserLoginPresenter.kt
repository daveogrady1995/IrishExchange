package com.davidogrady.irishexchange.userlogin
import com.davidogrady.irishexchange.constants.CurrentUser
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.util.FirebaseDeviceRegistration
import com.google.gson.Gson

class UserLoginPresenter(
    private val view: UserLoginContract.View,
    private val authManager: AuthManager,
    private val preferencesManager: SharedPreferencesManager,
    private val databaseManager: DatabaseManager
    ) : UserLoginContract.Presenter {

    init {
        view.presenter = this
    }

    override fun loginUserWithEmailAndPassword(email: String, password: String) {
        view.showLoadingIndicator()
        authManager.loginUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                storeLoggedInUserUid()
                storeDeviceRegistrationToken()
                fetchUserAndStorePublicKey()
                view.showLoginSuccessMessage()
                view.hideLoadingIndicator()
                view.navigateToLatestMessagesScreen()
            }
            .addOnFailureListener {
                view.showLoginFailureMessage(it.message.toString())
                view.hideLoadingIndicator()
            }
    }

    private fun fetchUserAndStorePublicKey() {
        val user = CurrentUser.currentUser
        if (user != null) {
            storeUserPublicKey(user.publicKey)
        }
    }

    private fun storeUserPublicKey(publicKey: ArrayList<Int>) {
        val publicKeyJson = Gson().toJson(publicKey)
        preferencesManager.storePreferenceKey("publicKey", publicKeyJson)
    }

    private fun storeLoggedInUserUid() {
        val currentUserUid = authManager.getUidForCurrentUser()
        preferencesManager.storePreferenceKey("uid", currentUserUid!!)
    }

    private fun storeDeviceRegistrationToken() {
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