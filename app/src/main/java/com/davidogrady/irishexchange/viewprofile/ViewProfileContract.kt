package com.davidogrady.irishexchange.viewprofile

import com.davidogrady.irishexchange.models.User

interface ViewProfileContract {
    interface View {
        var presenter: Presenter
        fun showNetworkUnavailableMessage()
        fun navigateToRegistrationScreen()
        fun populateViewWithUserProfileDetails(user: User)
        fun displayUnableToRetrieveProfileErrorMessage()
        fun renderFirebaseImageIntoView(profileImageUrl: String)
        fun showLoadingIndicatorFragment()
        fun hideLoadingIndicatorFragment()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun isUserLoggedIn(): Boolean
        fun logOutUser()
        fun getLoggedInUserUid(): String?
        fun displayProfileDetails(user: User)
    }
}