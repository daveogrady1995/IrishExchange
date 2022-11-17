package com.davidogrady.irishexchange.managers

import com.davidogrady.irishexchange.models.ChatMessage
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.models.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DatabaseManager(private val mDatabase: FirebaseDatabase) {

    fun saveUserToFirebaseDatabase(user: User, downloadImageUrl: String) : Task<Void> {
        val ref = mDatabase.getReference("/users/${user.uid}")
        user.profileImageUrl = downloadImageUrl

        return ref.setValue(user)
    }

    fun fetchUsersFromDatabase() = mDatabase.getReference("/users")

    fun saveUserMessageToDatabase(encryptedMessage: ArrayList<Int>, fromId: String, toId: String,
                                  messagesSentSuccessHandler: (referenceKey: String) -> Unit,
                                  messagesSentFailureHandler: () -> Unit) {
        // push creates an automatic node to save some data in
        // we need to create two different nodes for both users to see the messages
        val fromReference = mDatabase.getReference("/user-messages/$fromId/$toId").push()
        val toReference = mDatabase.getReference("/user-messages/$toId/$fromId").push()

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val messageDateTimeString = sdf.format(cal.time)

        val fromMessage = ChatMessage(fromReference.key.toString(), encryptedMessage,
            fromId, toId, System.currentTimeMillis() / 1000,
            messageDateTimeString, true)

        val toMessage = ChatMessage(toReference.key.toString(), encryptedMessage,
            fromId, toId, System.currentTimeMillis() / 1000,
            messageDateTimeString, false)

        fromReference.setValue(fromMessage).addOnSuccessListener {
            toReference.setValue(toMessage).addOnSuccessListener {
                messagesSentSuccessHandler.invoke(fromReference.key.toString())
            }.addOnFailureListener {
                messagesSentFailureHandler.invoke()
            }
        }.addOnFailureListener {
            messagesSentFailureHandler.invoke()
        }

    }

    // this will show the latest interaction between both users (from id / to id)
    fun updateLatestMessageForUsers(referenceId: String, encryptedMessage: ArrayList<Int>, fromId: String, toId: String,
                                    latestMessageUpdateSuccessHandler: () -> Unit,
                                    latestMessageUpdateFailureHandler: () -> Unit) {

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val messageDateTimeString = sdf.format(cal.time)

        val fromChatMessage = ChatMessage(referenceId, encryptedMessage,
            fromId, toId, System.currentTimeMillis() / 1000,
            messageDateTimeString, true)

        val toChatMessage = ChatMessage(referenceId, encryptedMessage,
            fromId, toId, System.currentTimeMillis() / 1000,
            messageDateTimeString, false)

        val latestMessageFromRef = mDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessageToRef = mDatabase.getReference("/latest-messages/$toId/$fromId")

        latestMessageFromRef.setValue(fromChatMessage).addOnSuccessListener {
            // flip and also do the reverse reference for other user
            latestMessageToRef.setValue(toChatMessage)
            latestMessageUpdateSuccessHandler.invoke()

        }.addOnFailureListener {
            latestMessageUpdateFailureHandler.invoke()
        }
    }

    fun removeLatestMessageForUser(fromId: String, toId: String) {
        val latestMessagesRef = mDatabase
            .getReference("/latest-messages/${fromId}/${toId}")


        latestMessagesRef.removeValue().addOnSuccessListener {
            //userMessagesRef.child("messageRead").setValue(messageRead)
        }
    }

    // when a user wants to block another ensure all latest messages between them are removed
    fun removeLatestMessageForBothUsers(fromId: String, toId: String,
                                        removeLatestMessageForBothUsersSuccessHandler: () -> Unit,
                                        removeLatestMessageForBothUsersFailureHandler: () -> Unit) {
        val latestMessagesRefFromUser = mDatabase
            .getReference("/latest-messages/${fromId}/${toId}")

        val latestMessagesRefToUser = mDatabase
            .getReference("/latest-messages/${toId}/${fromId}")


        latestMessagesRefFromUser.removeValue().addOnSuccessListener {
            latestMessagesRefToUser.removeValue().addOnSuccessListener {
                removeLatestMessageForBothUsersSuccessHandler.invoke()
            }.addOnFailureListener {
                removeLatestMessageForBothUsersFailureHandler.invoke()
            }
        }
    }

    fun listenForChatLogMessages(fromId: String, toId: String) : DatabaseReference {

        val chatLogMessagesRef =  mDatabase.getReference("/user-messages/$fromId/$toId")

        return chatLogMessagesRef
    }

    fun checkIfUserHasLatestMessagesInDatabase(fromId: String,
                                               userHasMessagesHandler: () -> Unit,
                                               userHasNoMessagesHandler: () -> Unit) {

        val latestMessagesRef =  mDatabase.getReference("/latest-messages/$fromId")

        latestMessagesRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if (chatMessage == null)
                    userHasNoMessagesHandler.invoke()
                else
                    userHasMessagesHandler.invoke()
            }
        })

    }

    fun checkIfUserHasNoChatLogMessagesInDatabase(fromId: String, toId: String,
                                                  userHasMessagesHandler: () -> Unit,
                                                  userHasNoMessagesHandler: () -> Unit) {

        val chatLogMessagesRef =  mDatabase.getReference("/user-messages/$fromId/$toId")

        chatLogMessagesRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if (chatMessage == null)
                    userHasNoMessagesHandler.invoke()
                else
                    userHasMessagesHandler.invoke()
            }
        })

    }

    fun removeChatLogMessagesEventListener(chatLogMessagesReference: DatabaseReference,
                                           chatLogMessagesChildEventListener: ChildEventListener) {
        chatLogMessagesReference.removeEventListener(chatLogMessagesChildEventListener)
    }

    fun updateMessageReadStatusToUserMessage(referenceId: String, fromId: String, toId: String,
                                             messageRead: Boolean) {
        val latestMessagesRef = mDatabase
            .getReference("/latest-messages/${toId}/${fromId}")

        val userMessagesRef = mDatabase
            .getReference("/user-messages/${toId}/${fromId}/${referenceId}")


        latestMessagesRef.child("messageRead").setValue(messageRead).addOnSuccessListener {
            //userMessagesRef.child("messageRead").setValue(messageRead)
        }
    }

    fun fetchUserFromDatabase(uid: String, fetchUserSuccessHandler: (user: User) -> Unit,
                               fetchUserFailureHandler: () -> Unit)  {

        val ref = mDatabase.getReference("/users/$uid")
        ref.keepSynced(true)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                if (user != null)
                    fetchUserSuccessHandler.invoke(user)
                else
                    fetchUserFailureHandler.invoke()
            }
        })
    }

    // handy to listen for changes in my profile screen
    fun fetchUserFromDatabaseAndListenForChanges(uid: String, fetchUserSuccessHandler: (user: User) -> Unit,
                              fetchUserFailureHandler: () -> Unit)  {

        val ref = mDatabase.getReference("/users/$uid")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                if (user != null)
                    fetchUserSuccessHandler.invoke(user)
                else
                    fetchUserFailureHandler.invoke()
            }
        })
    }

    fun listenForLatestMessages(fromId: String) : DatabaseReference {
        val latestMessagesRef = mDatabase.getReference("/latest-messages/$fromId")
        return latestMessagesRef
    }

    fun reportUser(user: User, reportUserSuccessHandler: () -> Unit,
                   reportUserFailureHandler: () -> Unit) {
        val ref = mDatabase.getReference("reportedUsers").push()
        ref.setValue(user).addOnSuccessListener {
            reportUserSuccessHandler.invoke()
        }.addOnFailureListener {
            reportUserFailureHandler.invoke()
        }
    }

    fun saveProfileDetailsForUser(userProfile: UserProfile, uid: String,
                                  saveProfileDetailsForUserSuccessHandler: () -> Unit,
                                  saveProfileDetailsForUserFailureHandler: () -> Unit) {
        val ref = mDatabase.getReference("/users/${uid}/userProfile")

        ref.setValue(userProfile).addOnSuccessListener {
            saveProfileDetailsForUserSuccessHandler.invoke()
        }.addOnFailureListener {
            saveProfileDetailsForUserFailureHandler.invoke()
        }
    }

    fun updateProfileImageUrlForUser(uid: String, profileImageUrl: String,
                                     updateProfileImageUrlForUserSuccessHandler: () -> Unit,
                                     updateProfileImageUrlForUserFailureHandler: () -> Unit) {
        val ref = mDatabase.getReference("/users/${uid}/profileImageUrl")

        ref.setValue(profileImageUrl).addOnSuccessListener {
            updateProfileImageUrlForUserSuccessHandler.invoke()
        }.addOnFailureListener {
            updateProfileImageUrlForUserFailureHandler.invoke()
        }
    }

    fun updateReportedUsersArrayForUser(uid: String, reportedUsers: ArrayList<String>,
                                        updateReportedUsersArraySuccessHandler: () -> Unit,
                                        updateReportedUsersArrayFailureHandler: () -> Unit) {
        val ref = mDatabase.getReference("/users/${uid}/reportedUsers")

        ref.setValue(reportedUsers).addOnSuccessListener {
            updateReportedUsersArraySuccessHandler.invoke()
        }.addOnFailureListener {
            updateReportedUsersArrayFailureHandler.invoke()
        }
    }

    fun updateBlockedUsersArrayForUsers(loggedInUserUid: String,
                                       loggedInUserBlockedUsers: ArrayList<String>,
                                        updateBlockedUsersArraySuccessHandler: () -> Unit,
                                        updateBlockedUsersArrayFailureHandler: () -> Unit) {

        val loggedInUserRef = mDatabase.getReference("/users/${loggedInUserUid}/blockedUsers")

        loggedInUserRef.setValue(loggedInUserBlockedUsers).addOnSuccessListener {
            updateBlockedUsersArraySuccessHandler.invoke()
        }.addOnFailureListener {
            updateBlockedUsersArrayFailureHandler.invoke()
        }
    }

    fun updateUserActiveStatus(uid: String) {
        val userPresenceRef = mDatabase.getReference("/users/${uid}/userOnline")
        userPresenceRef.onDisconnect().setValue(false)
        userPresenceRef.setValue(true)
    }

    fun updateUserInactiveStatus(uid: String) {
        val userPresenceRef = mDatabase.getReference("/users/${uid}/userOnline")
        userPresenceRef.setValue(false)
    }

    fun updateUserDeviceRegToken(uid: String, token: String) {
        val userPresenceRef = mDatabase.getReference("/users/${uid}/deviceRegToken")
        userPresenceRef.setValue(token)
    }

    fun listenForChatPartnerOnlineStatus(uid: String,
                                         userActiveStatusChangedHandler: (userOnline: Boolean) -> Unit) {
        val chatPartnerOnlineStatus =  mDatabase.getReference("/users/${uid}/userOnline")

        chatPartnerOnlineStatus.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val userOnline = p0.value as Boolean
                userActiveStatusChangedHandler.invoke(userOnline)
            }
            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }
}