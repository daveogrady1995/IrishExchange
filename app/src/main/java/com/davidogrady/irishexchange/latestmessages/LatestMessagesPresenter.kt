package com.davidogrady.irishexchange.latestmessages

import android.content.Context
import android.graphics.Color
import android.os.Handler
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.holders.LatestMessageRow
import com.davidogrady.irishexchange.models.ChatMessage
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.Item

class LatestMessagesPresenter(
    private val view: LatestMessagesContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
) : LatestMessagesContract.Presenter {

    private var latestMessagesAdapter = GroupAdapter<ViewHolder>()
    // show a a loading indicator until the initial list is loaded because there will be overhead
    private var loadedInitialData = false
    // every time monitor for a new message in Firebase add it into the map
    private val latestMessagesMap = HashMap<String, ChatMessage>()

    private lateinit var currentUser: User

    // prevent multi clicking of recycler view item
    private var lastClickTime = System.currentTimeMillis()
    private val clickInterval = 300

    init {
        view.presenter = this
        // set item click listener on the adapter
        latestMessagesAdapter.setOnItemClickListener { adapterItem, adapterView ->

            val now = System.currentTimeMillis()
            if (now - lastClickTime < clickInterval)
                return@setOnItemClickListener

            lastClickTime = System.currentTimeMillis()

            val row = adapterItem as LatestMessageRow? ?: return@setOnItemClickListener

            view.navigateToChatLogActivityWithLatestMessagesRowBundle(row.chatPartnerUser!!)
        }

        latestMessagesAdapter.setOnItemLongClickListener { adapterItem, adapterView ->
            adapterView.setBackgroundColor(Color.parseColor("#e6e6e6"))
            view.deleteMessageAlertDialog(adapterView, adapterItem)
            return@setOnItemLongClickListener(true)
        }
    }

    override fun deleteLatestMessageFromDatabase(adapterItem: Item<ViewHolder>) {
        latestMessagesAdapter.remove(adapterItem)
        val row = adapterItem as LatestMessageRow
        databaseManager.removeLatestMessageForUser(getLoggedInUserUid()!!, row.userPartnerId)
        view.showMessageDeletedToast()
    }

    override fun logOutUser() {
        authManager.logOutCurrentUser()
        preferencesManager.removeStoredPreference("uid")
        preferencesManager.removeStoredPreference("deviceToken")
        view.navigateToLoginScreen()
    }

    override fun fetchCurrentUserAndListenForLatestMessages(uid: String) {

        databaseManager.fetchUserFromDatabase(uid,
            fetchUserSuccessHandler = { user ->
                currentUser = user
                // check if user has created their profile
                //CurrentUser.CURRENT_USER = user
                if (user.userProfile != null) {

                    // if there is no messages to load then do not show loading indicator
                    databaseManager.checkIfUserHasLatestMessagesInDatabase(uid,
                        userHasMessagesHandler = {
                            view.showLoadingIndicator()
                            view.hideNoNewMessagesText()
                        }, userHasNoMessagesHandler = {
                            view.hideLoadingIndicator()
                            view.showNoNewMessagesText()
                        })

                    if (!latestMessagesRefInitialized) {
                        Handler().postDelayed({listenForLatestMessages(uid)}, 1500)
                    }
                    else
                        listenForLatestMessages(uid)
                }
                else
                    view.navigateToCreateProfileActivity()

            }, fetchUserFailureHandler = {
            })

    }

    override fun listenForLatestMessages(currentUserId: String) {
        view.updateLatestMessagesRecyclerView(latestMessagesAdapter)

        val ref = databaseManager.listenForLatestMessages(currentUserId)
        latestMessagesRefInitialized()
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                initialDataLoaded()
                view.hideLoadingIndicator()
                view.hideNoNewMessagesText()

                val latestMessage = p0.getValue(ChatMessage::class.java) ?: return

                // hide messages sent to themselves
                if(latestMessage.fromId == currentUserId && latestMessage.toId == currentUserId)
                    return

                latestMessagesMap[p0.key!!] = latestMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val latestMessage = p0.getValue(ChatMessage::class.java) ?: return

                // hide messages sent to themselves
                if(latestMessage.fromId == currentUserId && latestMessage.toId == currentUserId)
                    return

                latestMessagesMap[p0.key!!] = latestMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }
            override fun onChildRemoved(p0: DataSnapshot) {
            }
            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    override fun isNetworkAvailable(context: Context) = NetworkAvailability().isInternetAvailable(context)

    override fun getLoggedInUserUid(): String? {
        return authManager.getUidForCurrentUser()
    }

    override fun initialDataLoaded() {
        if (!loadedInitialData) {
            // finished loading initial data
            loadedInitialData = true
        }
    }

    override fun updateUserActiveStatus(currentUserUid: String) {
        databaseManager.updateUserActiveStatus(currentUserUid)
    }

    private fun refreshRecyclerViewMessages() {
        // clear the existing latest messages after retrieving a new one to ensure we don't get
        // a latest message for the same user twice
        latestMessagesAdapter.clear()
        // grab all of the chat messages that we've monitored for so far and sort by date
        val sortedLatestMessages = sortLatestMessagesMapByCurrentTimeMillis()
        sortedLatestMessages.values.forEach { latestMessage ->
            val userPartnerId = determineChatPartner(latestMessage)
            // doing the database async call inside the LatestMessagesRow
            if (latestMessagesShouldBeBlocked(currentUser.blockedUsers, userPartnerId)) return
            latestMessagesAdapter.add(LatestMessageRow(latestMessage, userPartnerId, currentUser, databaseManager))
        }
    }

    private fun latestMessagesShouldBeBlocked(blockedUsers: ArrayList<String>, userPartnerId: String) : Boolean {
        return blockedUsers.contains(userPartnerId)
    }

    private fun determineChatPartner(latestMessage: ChatMessage) : String {
        val chatPartnerId: String
        if (latestMessage.fromId == authManager.getUidForCurrentUser())
            chatPartnerId = latestMessage.toId
        else
            chatPartnerId = latestMessage.fromId

        return chatPartnerId
    }

    private fun sortLatestMessagesMapByCurrentTimeMillis() : Map<String, ChatMessage> {
        return latestMessagesMap.toList()
            .sortedByDescending { (_, value) -> value.currentTimeMillis }.toMap()
    }

    // ensure we only have 1 firebase database connection at a time
    private fun latestMessagesRefInitialized() {
        latestMessagesRefInitialized = true
    }

    companion object {
        var latestMessagesRefInitialized: Boolean = false
    }

}