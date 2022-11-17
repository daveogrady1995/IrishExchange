package com.davidogrady.irishexchange.resetpassword

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager

class ResetPasswordPresenter(
    private val view: ResetPasswordContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager
) : ResetPasswordContract.Presenter {

    init {
        view.presenter = this
    }

    override fun sendPasswordRequestToEmail(email: String) {
        authManager.sendPasswordResetToEmail(email, emailSentSuccessHandler = {
            view.showPasswordRequestEmailSentSuccessToast()
        }, emailSentFailureHandler = {
            view.showPasswordRequestEmailFailureToast(it)
        })
    }
}