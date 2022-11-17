package com.davidogrady.irishexchange.chatlog

import com.davidogrady.irishexchange.holders.ChatFromItem
import com.davidogrady.irishexchange.holders.ChatToItem
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.ChatMessage
import com.davidogrady.irishexchange.models.NotificationRequest
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.util.FcmNotificationApi
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import android.os.Handler
import com.davidogrady.irishexchange.constants.IrishHelper
import com.davidogrady.irishexchange.holders.ChatMessagesDateGrouper
import com.davidogrady.irishexchange.holders.IrishHelperGridItem
import com.davidogrady.irishexchange.util.EncryptionHelper
import com.davidogrady.irishexchange.util.MessageDateFormatter
import kotlin.collections.ArrayList

class ChatLogPresenter(
    private val view: ChatLogContract.View,
    private val authManager: AuthManager,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: SharedPreferencesManager
    ) : ChatLogContract.Presenter {

    private var chatLogAdapter = GroupAdapter<ViewHolder>()
    private var irishHelperGridAdapter = GroupAdapter<ViewHolder>()

    private lateinit var chatLogMessagesReference: DatabaseReference
    private lateinit var chatLogMessagesChildEventListener: ChildEventListener
    private var chatLogMessagesRefInitialized = false

    private var currentUser: User? = null
    private var chatPartner: User? = null
    private var loadingData: Boolean = false

    // grouping of messages by date
    private var startedMessageDateGrouping = false
    private lateinit var lastMessageDateText: String

    init {
        view.presenter = this

        view.updateChatLogRecyclerView(chatLogAdapter)

        IrishHelper.greetings.forEach {
            irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
        }

        view.updateIrishHelperGridView(irishHelperGridAdapter)

        chatLogAdapter.setOnItemClickListener { adapterItem, _ ->

            if (adapterItem is ChatToItem) {
                adapterItem.toggleTimeTextView()
            }

            if (adapterItem is ChatFromItem) {
                adapterItem.toggleTimeTextView()
            }
        }

        irishHelperGridAdapter.setOnItemClickListener { adapterItem, _ ->

            val helperItem = adapterItem as IrishHelperGridItem
            val userPartner = view.getUserPartner()
            if (userPartner != null)
                performSendMessage(userPartner, helperItem.irishText)
        }

    }

    override fun performSendMessage(user: User, message: String) {
        val fromId = getLoggedInUserUid()
        val toId = user.uid
        view.clearChatLogInput()

        if (currentUser != null) {

            val encryptedMessage = encryptUserMessage(message, currentUser!!.publicKey)

            saveEncryptedMessageToDatabase(encryptedMessage, fromId, toId)

            listenForChatPartnerOnlineStatusAndSendFcmNotification(message, toId, user.deviceRegToken)
        }

        else {
            databaseManager.fetchUserFromDatabase(fromId!!, fetchUserSuccessHandler = {
                currentUser = it

                val encryptedMessage = encryptUserMessage(message, currentUser!!.publicKey)

                saveEncryptedMessageToDatabase(encryptedMessage, fromId, toId)

                listenForChatPartnerOnlineStatusAndSendFcmNotification(message, toId, user.deviceRegToken)

            }, fetchUserFailureHandler = {})
        }
    }

    override fun fetchUserPartnersAndListenForMessages(user: User, loggedInUserId: String) {

        // if we are not connected to the internet use the passed in bundle
        // otherwise retrieve user in database with their latest information
        if (view.isNetworkAvailable()) {
            databaseManager.fetchUserFromDatabase(user.uid, fetchUserSuccessHandler = {
                this.chatPartner = it

                // if messages are still being retrieved from db start the spinner
                // but only while they are online
                view.showLoadingIndicator()
                loadingData = true

                // add a little delay so spinner does not flash the screen
                Handler().postDelayed({listenForChatLogMessages(loggedInUserId)}, 1000)
            }, fetchUserFailureHandler = {})
        } else {
            chatPartner = user
            listenForChatLogMessages(loggedInUserId)
        }
    }

    // finish listening for messages when we leave the activity
    override fun removeChatLogMessagesListener() {
        // only remove if we are already listening
        if (chatLogMessagesRefInitialized) {
            databaseManager.removeChatLogMessagesEventListener(chatLogMessagesReference,
                chatLogMessagesChildEventListener)
        }
    }

    override fun logOutUser() {
        authManager.logOutCurrentUser()
        preferencesManager.removeStoredPreference("uid")
        preferencesManager.removeStoredPreference("deviceToken")
    }

    override fun getLoggedInUserUid(): String? {
        return preferencesManager.getStoredPreferenceString("uid")
    }

    override fun fetchUserFromDatabase(fromId: String) {
        databaseManager.fetchUserFromDatabase(fromId, fetchUserSuccessHandler = {
            currentUser = it
        }, fetchUserFailureHandler = {
        })
    }

    override fun getChatPartner() : User? {
        return chatPartner
    }

    override fun reportUser() {
        val currentUser = this.currentUser ?: return
        val chatPartner = this.chatPartner ?: return

        // make sure they haven't already reported same person
        if (currentUser.reportedUsers.contains(chatPartner.uid))
            view.displayAlreadyReportedUserMessage()
        else {
            databaseManager.reportUser(chatPartner,
                reportUserSuccessHandler = {
                    // update reportedUsers field for user and tell the DB
                    updateReportedUsersArrayForUser(currentUser, chatPartner.uid)

                }, reportUserFailureHandler ={})
        }
    }

    override fun blockUser() {
        view.showLoadingIndicator()
        val currentUser = this.currentUser ?: return
        val chatPartner = this.chatPartner ?: return

        // make sure they haven't already blocked same person
        if (currentUser.blockedUsers.contains(chatPartner.uid))
            view.displayAlreadyBlockedUserMessage()
        else {
            Handler().postDelayed({updateBlockedUsersArrayForUser(currentUser, chatPartner.uid)}, 3000)
        }

    }

    override fun updateFlashcardsInIrishHelperTab(tabPositon: Int) {

        when (tabPositon) {
            IrishHelper.GREETINGS_TAB -> {
                irishHelperGridAdapter.clear()
                IrishHelper.greetings.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }

            }
            IrishHelper.ABOUT_ME -> {
                irishHelperGridAdapter.clear()
                IrishHelper.aboutMe.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }

            }
            IrishHelper.SCHOOL_TAB -> {
                irishHelperGridAdapter.clear()
                IrishHelper.school.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }
            }
            IrishHelper.JOBS_TAB -> {
                irishHelperGridAdapter.clear()
                IrishHelper.jobs.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }
            }
            IrishHelper.DAYS_TAB -> {
                irishHelperGridAdapter.clear()
                IrishHelper.days.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }
            }
            IrishHelper.MONTHS_TAB -> {
                irishHelperGridAdapter.clear()
                IrishHelper.months.forEach {
                    irishHelperGridAdapter.add(IrishHelperGridItem(it.irishPhrase, it.englishPhrase))
                }
            }
        }

    }

    private fun updateReportedUsersArrayForUser(currentUser: User, chatPartnerUid: String) {
        val reportedUsers = currentUser.reportedUsers
        reportedUsers.add(chatPartnerUid)
        databaseManager.updateReportedUsersArrayForUser(currentUser.uid, reportedUsers,
            updateReportedUsersArraySuccessHandler = {
                view.displayReportUserSuccessMessage()
            }, updateReportedUsersArrayFailureHandler = {
            })
    }

    private fun updateBlockedUsersArrayForUser(currentUser: User, chatPartnerUid: String) {
        val loggedInUserBlockedUsers = currentUser.blockedUsers
        loggedInUserBlockedUsers.add(chatPartnerUid)

        databaseManager.updateBlockedUsersArrayForUsers(currentUser.uid,
            loggedInUserBlockedUsers,
            updateBlockedUsersArraySuccessHandler = {
                deleteLatestMessagesForUsers(currentUser.uid, chatPartnerUid)
            }, updateBlockedUsersArrayFailureHandler = {
                view.displayBlockedUserFailureMessage()
                view.hideLoadingIndicator()
            })
    }

    private fun deleteLatestMessagesForUsers(currentUserUid: String, chatPartnerUid: String) {
        databaseManager.removeLatestMessageForBothUsers(currentUserUid, chatPartnerUid,
            removeLatestMessageForBothUsersSuccessHandler = {
                view.displayBlockedUserSuccessMessage()
                view.hideLoadingIndicator()
                removeChatLogMessagesListener()
                view.navigateToMainActivity()
            }, removeLatestMessageForBothUsersFailureHandler = {
                view.hideLoadingIndicator()
                view.displayBlockedUserFailureMessage()
            })

    }

    private fun listenForChatLogMessages(loggedInUserId: String) {

        databaseManager.fetchUserFromDatabase(loggedInUserId,
            fetchUserSuccessHandler = { currentUser ->
                val toId = this.chatPartner!!.uid

                if (!chatLogMessagesRefInitialized) {
                    view.showLoadingIndicator()

                    // if there is no messages to load then stop the loading process
                    databaseManager.checkIfUserHasNoChatLogMessagesInDatabase(loggedInUserId, toId,
                        userHasMessagesHandler = {
                            view.hideLoadingIndicator()
                        }, userHasNoMessagesHandler = {
                            view.hideLoadingIndicator()
                        })
                }


                // storing the reference and listeners so we can stop listening after leaving activity
                // we stop listening so the user can't read the incoming messages
                if (!chatLogMessagesRefInitialized) {
                    chatLogMessagesReferenceInit(loggedInUserId, toId)

                    chatLogMessagesChildEventListener = chatLogMessagesReference.addChildEventListener(object: ChildEventListener {

                        override fun onChildAdded(p0: DataSnapshot, p1: String?) {

                            if (loadingData)
                                view.hideLoadingIndicator()

                            val chatMessage = p0.getValue(ChatMessage::class.java) ?: return

                            // start grouping of messages by date but ignore the time
                            if (!startedMessageDateGrouping) {
                                lastMessageDateText = MessageDateFormatter().getMessageDateText(chatMessage.currentDateTimeText)
                                chatLogAdapter.add(ChatMessagesDateGrouper(lastMessageDateText))
                                startedMessageDateGrouping = true
                            } else {
                                val currentMessageDate = MessageDateFormatter().getMessageDateText(chatMessage.currentDateTimeText)
                                if (!checkIfCurrentMessageIsOnTheSameDayAsTheLastMessage(currentMessageDate))
                                    chatLogAdapter.add(ChatMessagesDateGrouper(currentMessageDate))

                                lastMessageDateText = currentMessageDate
                            }



                            // determine which chat bubble is displayed (left or right)
                            if(chatMessage.fromId == loggedInUserId) {
                                val decryptedMessage = decryptUserMessage(chatMessage.encryptedMessage, currentUser) ?: return
                                val messageTimeText = MessageDateFormatter().getMessageTimeText(chatMessage.currentDateTimeText)
                                chatLogAdapter.add(ChatFromItem(decryptedMessage, messageTimeText, currentUser))

                            } else if (chatMessage.fromId == toId) {
                                val decryptedMessage = decryptUserMessage(chatMessage.encryptedMessage, chatPartner!!) ?: return
                                 val messageTimeText = MessageDateFormatter().getMessageTimeText(chatMessage.currentDateTimeText)
                                chatLogAdapter.add(ChatToItem(decryptedMessage, messageTimeText, chatPartner!!))
                                val messageRead = chatMessage.messageRead
                                if (messageRead != null) {
                                    // update read status from this chat message
                                    if (messageRead == false) {
                                        chatMessage.messageRead = true
                                        // wait a few seconds for the latest message node to be added from the chat partner
                                        // or message updated will not be registered
                                        Handler().postDelayed({
                                            databaseManager.updateMessageReadStatusToUserMessage(chatMessage.referenceId,
                                                chatMessage.fromId, chatMessage.toId, true)
                                        }, 3000)
                                    }
                                }
                            }

                            view.updateChatLogRecyclerViewPosition(chatLogAdapter)

                        }
                        override fun onCancelled(p0: DatabaseError) {
                        }
                        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                        }
                        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                        }
                        override fun onChildRemoved(p0: DataSnapshot) {
                        }
                    })

                }

            }, fetchUserFailureHandler = {
            })

    }

    // ensure we only have 1 firebase database connection at a time
    private fun chatLogMessagesReferenceInit(loggedInUserId: String, toId: String) {
        chatLogMessagesReference = databaseManager.listenForChatLogMessages(loggedInUserId, toId)
        chatLogMessagesRefInitialized = true
    }

    private fun updateLatestMessageForUsers(referenceId: String, message: ArrayList<Int>,
                                            fromId: String, toId: String,
                                            updateLatestMessageForUserSuccess: () -> Unit) {
        databaseManager.updateLatestMessageForUsers(referenceId, message, fromId, toId,
            latestMessageUpdateSuccessHandler = {
                updateLatestMessageForUserSuccess.invoke()
            },
            latestMessageUpdateFailureHandler = {
            })
    }

    private fun sendNotificationRequestToFCM(notificationRequest: NotificationRequest) {
        FcmNotificationApi().sendNotificationToDevice(notificationRequest)
    }

    private fun listenForChatPartnerOnlineStatusAndSendFcmNotification(message: String, toId: String,
                                                                       deviceRegToken: String) {
        // send a notification to user partner if they are not online
        databaseManager.listenForChatPartnerOnlineStatus(toId, userActiveStatusChangedHandler = { userOnline ->
            if (!userOnline) {
                val notificationRequest = NotificationRequest(currentUser!!.username, message,
                    deviceRegToken, currentUser!!)
                sendNotificationRequestToFCM(notificationRequest)
            }
        })
    }

    private fun encryptUserMessage(message: String, publicKey: ArrayList<Int>) : ByteArray? {

        // convert key to array of bytes
        val publicKeyListBytes : ArrayList<Byte> = arrayListOf()

        publicKey.forEach {
            publicKeyListBytes.add(it.toByte())
        }

        return EncryptionHelper().encrypt(message.toByteArray(), publicKeyListBytes.toByteArray())
    }

    private fun decryptUserMessage(encryptedMessage: ArrayList<Int>, user: User) : String? {
        val messageListBytes : ArrayList<Byte> = arrayListOf()

        val userKeyListBytes: ArrayList<Byte> = arrayListOf()

        // convert message to array of bytes
        encryptedMessage.forEach {
            messageListBytes.add(it.toByte())
        }

        // convert user key to array of bytes
        user.publicKey.forEach {
            userKeyListBytes.add(it.toByte())
        }

        // decrypt message
        return EncryptionHelper().decrypt(messageListBytes.toByteArray(), userKeyListBytes.toByteArray())
    }

    private fun saveEncryptedMessageToDatabase(encryptedMessage: ByteArray?, fromId: String?, toId: String) {

        // convert message to array of ints
        val encryptedMessageListInt : ArrayList<Int> = arrayListOf()

        // realtime database does not allow us to store in bytes
        encryptedMessage!!.forEach {
            encryptedMessageListInt.add(it.toInt())
        }

        databaseManager.saveUserMessageToDatabase(encryptedMessageListInt, fromId!!, toId, { referenceId ->
            view.updateChatLogRecyclerViewPosition(chatLogAdapter)
            updateLatestMessageForUsers(referenceId, encryptedMessageListInt, fromId, toId, updateLatestMessageForUserSuccess = {})
        }, messagesSentFailureHandler = {
            view.displayUnableToSendMessageError()
        })
    }

    // we want to group messages that were sent on the same date
    private fun checkIfCurrentMessageIsOnTheSameDayAsTheLastMessage(currentMessageDateText: String) : Boolean {
        return (currentMessageDateText == lastMessageDateText)
    }
}