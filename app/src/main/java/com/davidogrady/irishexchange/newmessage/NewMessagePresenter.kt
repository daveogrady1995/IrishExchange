package com.davidogrady.irishexchange.newmessage

import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.holders.UserItem
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

class NewMessagePresenter(
    private val view: NewMessageContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
) : NewMessageContract.Presenter {

    // prevent multi clicking of recycler view item
    private var lastClickTime = System.currentTimeMillis()
    private val clickInterval = 300

    init {
        view.presenter = this
    }

    override fun fetchUsers(loggedInUserUid: String) {

        databaseManager.fetchUserFromDatabase(loggedInUserUid, fetchUserSuccessHandler = { currentUser ->

            val unsortedUserItems = arrayListOf<UserItem>()

            val ref = databaseManager.fetchUsersFromDatabase()
            ref.keepSynced(true)
            ref.addListenerForSingleValueEvent(object: ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {
                    val adapter = GroupAdapter<ViewHolder>()

                    p0.children.forEach {
                        val user = it.getValue(User::class.java) ?: return
                        if (user.uid != loggedInUserUid) {
                            if (!usersHaveBlockedEachOther(currentUser, user))
                                unsortedUserItems.add(UserItem(user))
                        }
                    }

                    adapter.setOnItemClickListener { adapterItem, _ ->
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > clickInterval) {
                            lastClickTime = System.currentTimeMillis()
                            val userItem = adapterItem as UserItem
                            view.navigateToChatLogActivityWithUserItemBundle(userItem)
                        }
                    }

                    sortUserItemsByOnlineStatus(unsortedUserItems).forEach {
                        adapter.add(it)
                    }

                    view.updateNewMessageRecyclerView(adapter)
                    view.hideLoadingIndicator()
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })

        }, fetchUserFailureHandler = {
            return@fetchUserFromDatabase
        })


    }

    private fun sortUserItemsByOnlineStatus(userItems: ArrayList<UserItem>)
            = userItems.sortedWith(compareByDescending {it.user.userOnline})

    private fun usersHaveBlockedEachOther(currentUser: User, chatPartner: User) : Boolean {
        return (currentUser.blockedUsers.contains(chatPartner.uid) ||
                chatPartner.blockedUsers.contains(currentUser.uid))
    }

    override fun getLoggedInUserUid(): String? {
        return preferencesManager.getStoredPreferenceString("uid")
    }

    override fun logOutUser() {
        databaseManager.updateUserInactiveStatus(authManager.getUidForCurrentUser()!!)
        authManager.logOutCurrentUser()
        preferencesManager.removeStoredPreference("uid")
        preferencesManager.removeStoredPreference("deviceToken")

        view.navigateToRegistrationScreen()
    }
}