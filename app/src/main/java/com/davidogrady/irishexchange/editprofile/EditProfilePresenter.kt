package com.davidogrady.irishexchange.editprofile

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.UserProfile

class EditProfilePresenter(
    private val view: EditProfileContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
) : EditProfileContract.Presenter {

    init {
        view.presenter = this
    }

    override fun getLoggedInUserUid(): String? {
        return preferencesManager.getStoredPreferenceString("uid")
    }

    override fun fetchCurrentUserAndPopulateFields(uid: String) {
        databaseManager.fetchUserFromDatabase(uid,
            fetchUserSuccessHandler = { user ->
                view.populateEditProfileFields(user.userProfile!!)
            }, fetchUserFailureHandler = {
            })

    }

    override fun createUserProfile(userProfile: UserProfile) {
        val uid = authManager.getUidForCurrentUser()
        databaseManager.saveProfileDetailsForUser(userProfile, uid!!,
            saveProfileDetailsForUserSuccessHandler = {
                view.showEditProfileSuccessMessage()
                view.enableUserInteractionOnView()
                view.hideLoadingIndicatorEditProfile()
                view.navigateBackToCreateProfileFragment()
            }, saveProfileDetailsForUserFailureHandler = {
                view.showEditProfileFailureMessage()
            })
    }

}