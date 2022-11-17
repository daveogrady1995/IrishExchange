package com.davidogrady.irishexchange.myprofile

import com.davidogrady.irishexchange.models.User

interface MyProfileContract {
    interface View {
        var presenter: Presenter
        fun showNetworkUnavailableMessage()
        fun navigateToRegistrationScreen()
        fun populateViewWithUserProfileDetails(user: User)
        fun displayUnableToRetrieveProfileErrorMessage()
        fun showImageUploadFailureMessage()
        fun showImageUploadSuccessMessage()
        fun renderFirebaseImageIntoView(profileImageUrl: String)
        fun disableUserInteractionOnView()
        fun enableUserInteractionOnView()
        fun showLoadingIndicatorFragment()
        fun hideLoadingIndicatorFragment()
        fun showLoadingIndicatorUploadPhoto()
        fun hideLoadingIndicatorUploadPhoto()
        fun renderLocalImageIntoView()
        fun enableSelectPhotoButton()
        fun disableSelectPhotoButton()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun isUserLoggedIn(): Boolean
        fun logOutUser()
        fun fetchUserAndDisplayProfileDetails(uid: String)
        fun uploadImageToFirebaseStorage(compressedImageByteArray: ByteArray)
        fun deletePreviousImageInFirebaseStorage(previousPhotoUrl: String, newPhotoUrl: String)
        fun updateUserProfileImageUrlInDatabase(profileImageUrl: String)
        fun getLoggedInUserUid(): String?
        fun updateUserInactiveStatus(currentUserUid: String)
        fun updateUserActiveStatus(currentUserUid: String)
    }
}