package com.davidogrady.irishexchange.userregistration

import com.davidogrady.irishexchange.models.User

interface UserRegistrationContract {
    interface View {
        var presenter: Presenter
        fun showLoadingIndicator()
        fun hideLoadingIndicator()
        fun showRegistrationFailureMessage(errorMessage: String)
        fun showRegistrationSuccessMessage()
        fun showImageUploadFailureMessage(errorMessage: String)
        fun navigateToCreateProfileScreen()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun registerUser(user: User, password: String, compressedImageByteArray: ByteArray) {}
        fun userLogOut() { }
    }
}