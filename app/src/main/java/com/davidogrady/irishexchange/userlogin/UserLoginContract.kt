package com.davidogrady.irishexchange.userlogin

interface UserLoginContract {
    interface View {
        var presenter: Presenter
        fun showLoadingIndicator()
        fun hideLoadingIndicator()
        fun showConnectivityErrorMessage()
        fun showLoginSuccessMessage()
        fun navigateToLatestMessagesScreen()
        fun showLoginFailureMessage(message: String)
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun loginUserWithEmailAndPassword(email: String, password: String)
    }
}