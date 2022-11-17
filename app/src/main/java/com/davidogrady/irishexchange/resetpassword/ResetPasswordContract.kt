package com.davidogrady.irishexchange.resetpassword

interface ResetPasswordContract {
    interface View {
        var presenter: Presenter
        fun showPasswordRequestEmailFailureToast(errorMessage: String)
        fun showPasswordRequestEmailSentSuccessToast()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}

        fun sendPasswordRequestToEmail(email: String)
    }
}