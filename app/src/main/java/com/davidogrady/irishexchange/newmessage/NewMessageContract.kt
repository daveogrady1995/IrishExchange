package com.davidogrady.irishexchange.newmessage

import com.davidogrady.irishexchange.holders.UserItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

interface NewMessageContract {
    interface View {
        var presenter: Presenter
        fun updateNewMessageRecyclerView(adapter: GroupAdapter<ViewHolder>)
        fun navigateToChatLogActivityWithUserItemBundle(userItem: UserItem)
        fun showLoadingIndicator()
        fun hideLoadingIndicator()
        fun navigateToRegistrationScreen()
        fun showNetworkUnavailableMessage()
    }
    interface Presenter {
        fun start() {}
        fun stop() {}
        fun getLoggedInUserUid(): String?
        fun fetchUsers(loggedInUserUid: String)
        fun logOutUser()
    }
}