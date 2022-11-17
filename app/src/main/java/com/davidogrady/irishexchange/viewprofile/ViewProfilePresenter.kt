package com.davidogrady.irishexchange.viewprofile

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User

class ViewProfilePresenter(
    private val view: ViewProfileContract.View,
    private val authManager: AuthManager,
    private val preferencesManager: SharedPreferencesManager
) : ViewProfileContract.Presenter {

    init {
        view.presenter = this
    }

    override fun isUserLoggedIn() = authManager.isUserLoggedIn()

    override fun logOutUser() {
        authManager.logOutCurrentUser()
        preferencesManager.removeStoredPreference("uid")
        preferencesManager.removeStoredPreference("deviceToken")
        view.navigateToRegistrationScreen()
    }

    override fun displayProfileDetails(user: User) {
        // check if user has created their profile
        if (user.userProfile != null) {
            view.populateViewWithUserProfileDetails(user)
            view.hideLoadingIndicatorFragment()
        }
        else {
            view.displayUnableToRetrieveProfileErrorMessage()
        }
    }

    override fun getLoggedInUserUid(): String? {
        return authManager.getUidForCurrentUser()
    }


}