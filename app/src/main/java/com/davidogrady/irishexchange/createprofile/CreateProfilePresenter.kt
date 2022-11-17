package com.davidogrady.irishexchange.createprofile

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.models.UserProfile

class CreateProfilePresenter(
    private val view: CreateProfileContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager
) : CreateProfileContract.Presenter {

    init {
        view.presenter = this
    }

    override fun isUserLoggedIn() = authManager.isUserLoggedIn()

    override fun createUserProfile(userProfile: UserProfile) {
        val uid = authManager.getUidForCurrentUser()
        databaseManager.saveProfileDetailsForUser(userProfile, uid!!,
            saveProfileDetailsForUserSuccessHandler = {
                view.showCreateProfileSuccessMessage()
                view.navigateToMainActivity()
        }, saveProfileDetailsForUserFailureHandler = {
                view.showCreateProfileFailureMessage()
        })
    }
}