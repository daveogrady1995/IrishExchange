package com.davidogrady.irishexchange.editprofile

import com.davidogrady.irishexchange.models.UserProfile

interface EditProfileContract {
    interface View {
        var presenter: Presenter
        fun populateEditProfileFields(userProfile: UserProfile)
        fun showEditProfileSuccessMessage()
        fun showEditProfileFailureMessage()
        fun showMissingFieldsMessage()
        fun disableUserInteractionOnView()
        fun enableUserInteractionOnView()
        fun showLoadingIndicatorEditProfile()
        fun hideLoadingIndicatorEditProfile()
        fun navigateToMainActivity()
        fun navigateBackToCreateProfileFragment()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}

        fun getLoggedInUserUid(): String?
        fun fetchCurrentUserAndPopulateFields(uid: String)
        fun createUserProfile(userProfile: UserProfile)
    }
}