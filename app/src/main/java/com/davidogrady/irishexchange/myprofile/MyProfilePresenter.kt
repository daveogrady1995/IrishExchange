package com.davidogrady.irishexchange.myprofile

import com.davidogrady.irishexchange.constants.CurrentUser
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.managers.StorageManager
import com.davidogrady.irishexchange.models.User

class MyProfilePresenter(
    private val view: MyProfileContract.View,
    private val authManager: AuthManager,
    private val storageManager: StorageManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
) : MyProfileContract.Presenter {

    private var latestFetchAndListenForUserRefInitialized: Boolean = false

    init {
        view.presenter = this
    }

    private  var currentUser: User? = null

    override fun isUserLoggedIn() = authManager.isUserLoggedIn()

    override fun logOutUser() {
        databaseManager.updateUserInactiveStatus(authManager.getUidForCurrentUser()!!)
        authManager.logOutCurrentUser()
        preferencesManager.removeStoredPreference("uid")
        preferencesManager.removeStoredPreference("deviceToken")
        view.navigateToRegistrationScreen()
    }

    override fun fetchUserAndDisplayProfileDetails(uid: String) {

        if (!latestFetchAndListenForUserRefInitialized) {
            fetchAndListenForUserRefInitialize()
            // if user has created their profile go to latest messages
            databaseManager.fetchUserFromDatabaseAndListenForChanges(uid,
                fetchUserSuccessHandler = { user ->
                    currentUser = user
                    // check if user has created their profile
                    if (user.userProfile != null) {
                        view.populateViewWithUserProfileDetails(user)
                        view.hideLoadingIndicatorFragment()
                        view.enableSelectPhotoButton()
                    }
                    else {
                        view.displayUnableToRetrieveProfileErrorMessage()
                    }
                }, fetchUserFailureHandler = {
                    view.displayUnableToRetrieveProfileErrorMessage()
                })
        } else {
            currentUser = CurrentUser.currentUser

            if (currentUser != null) {
                // check if user has created their profile
                if (currentUser!!.userProfile != null) {
                    view.populateViewWithUserProfileDetails(currentUser!!)
                    view.hideLoadingIndicatorFragment()
                    view.enableSelectPhotoButton()
                }
                else {
                    view.displayUnableToRetrieveProfileErrorMessage()
                }
            }
        }
    }

    override fun uploadImageToFirebaseStorage(compressedImageByteArray: ByteArray) {

        if (currentUser != null) {
            val previousPhotoUrl = currentUser!!.profileImageUrl
            storageManager.uploadImageToFirebaseStorage(
                compressedImageByteArray,
                uploadImageSuccessHandler = {
                    deletePreviousImageInFirebaseStorage(previousPhotoUrl, it)
                }, uploadImageErrorHandler = {
                    view.showImageUploadFailureMessage()

                })
        }
    }

    override fun deletePreviousImageInFirebaseStorage(previousPhotoUrl: String, newPhotoUrl: String) {
        storageManager.deleteImageInFirebaseStorage(previousPhotoUrl, deleteImageCompleteHandler = {
            updateUserProfileImageUrlInDatabase(newPhotoUrl)
        })
    }

    override fun updateUserProfileImageUrlInDatabase(profileImageUrl: String) {
        val uid = authManager.getUidForCurrentUser()
        databaseManager.updateProfileImageUrlForUser(uid!!, profileImageUrl,
            updateProfileImageUrlForUserSuccessHandler = {
                view.showImageUploadSuccessMessage()
                view.enableUserInteractionOnView()
                view.renderLocalImageIntoView()
                view.hideLoadingIndicatorUploadPhoto()
            }, updateProfileImageUrlForUserFailureHandler = {
                view.showImageUploadFailureMessage()
            })
    }

    override fun getLoggedInUserUid(): String? {
        return preferencesManager.getStoredPreferenceString("uid")
    }

    override fun updateUserInactiveStatus(currentUserUid: String) {
        databaseManager.updateUserInactiveStatus(currentUserUid)
    }

    override fun updateUserActiveStatus(currentUserUid: String) {
        databaseManager.updateUserActiveStatus(currentUserUid)
    }

    // ensure we only have 1 firebase database connection at a time
    private fun fetchAndListenForUserRefInitialize() {
        latestFetchAndListenForUserRefInitialized = true
    }

}