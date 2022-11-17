package com.davidogrady.irishexchange.createprofile

import com.davidogrady.irishexchange.models.UserProfile

interface CreateProfileContract {
    interface View {
        var presenter: Presenter
        fun navigateToLoginScreen()
        fun showMissingFieldsMessage()
        fun showCreateProfileFailureMessage()
        fun showCreateProfileSuccessMessage()
        fun showNetworkUnavailableMessage()
        fun navigateToMainActivity()
        fun navigateToRegistrationScreen()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun isUserLoggedIn() : Boolean
        fun createUserProfile(userProfile: UserProfile)
    }
}