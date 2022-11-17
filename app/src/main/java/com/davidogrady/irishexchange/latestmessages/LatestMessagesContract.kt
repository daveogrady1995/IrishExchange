package com.davidogrady.irishexchange.latestmessages

import android.content.Context
import com.davidogrady.irishexchange.models.User
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder

interface LatestMessagesContract {
    interface View {
        var presenter: Presenter
        fun navigateToLoginScreen()
        fun updateLatestMessagesRecyclerView(adapter: GroupAdapter<ViewHolder>)
        fun showLoadingIndicator()
        fun hideLoadingIndicator()
        fun navigateToCreateProfileActivity()
        fun showNetworkUnavailableMessage()
        fun isNetworkAvailable(): Boolean
        fun navigateToChatLogActivityWithLatestMessagesRowBundle(chatPartnerUser: User)
        fun hideNoNewMessagesText()
        fun showNoNewMessagesText()
        fun deleteMessageAlertDialog(adapterView: android.view.View, adapterItem: Item<ViewHolder>)
        fun showMessageDeletedToast()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun logOutUser()
        fun fetchCurrentUserAndListenForLatestMessages(uid: String)
        fun listenForLatestMessages(currentUserId: String)
        fun isNetworkAvailable(context: Context) : Boolean
        fun initialDataLoaded()
        fun getLoggedInUserUid(): String?
        fun updateUserActiveStatus(currentUserUid: String)
        fun deleteLatestMessageFromDatabase(adapterItem: Item<ViewHolder>)
    }
}