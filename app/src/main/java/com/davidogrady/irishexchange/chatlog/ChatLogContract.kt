package com.davidogrady.irishexchange.chatlog

import com.davidogrady.irishexchange.models.User
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

interface ChatLogContract {
    interface View {
        var presenter: Presenter
        fun updateChatLogRecyclerView(adapter: GroupAdapter<ViewHolder>)
        fun displayUnableToFetchMessagesError()
        fun displayUnableToSendMessageError()
        fun clearChatLogInput()
        fun updateChatLogRecyclerViewPosition(adapter: GroupAdapter<ViewHolder>)
        fun isNetworkAvailable(): Boolean
        fun showLoadingIndicator()
        fun hideLoadingIndicator()
        fun checkIfUserHasPassedNotificationBundle(): Boolean?
        fun displayNotificationNetworkUnavailableMessage()
        fun displayReportUserSuccessMessage()
        fun displayAlreadyReportedUserMessage()
        fun displayReportUserFailureMessage()
        fun displayAlreadyBlockedUserMessage()
        fun displayBlockedUserSuccessMessage()
        fun displayBlockedUserFailureMessage()
        fun navigateToMainActivity()
        fun updateIrishHelperGridView(adapter: GroupAdapter<ViewHolder>)
        fun getUserPartner(): User?
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun performSendMessage(user: User, message: String)
        fun fetchUserPartnersAndListenForMessages(user: User, loggedInUserId: String)
        fun logOutUser()
        fun removeChatLogMessagesListener()
        fun getLoggedInUserUid(): String?
        fun fetchUserFromDatabase(fromId: String)
        fun getChatPartner(): User?
        fun reportUser()
        fun blockUser()
        fun updateFlashcardsInIrishHelperTab(tabPositon: Int)
    }
}