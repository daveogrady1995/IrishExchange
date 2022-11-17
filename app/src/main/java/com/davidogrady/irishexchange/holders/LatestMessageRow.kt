package com.davidogrady.irishexchange.holders

import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.models.ChatMessage
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.util.EncryptionHelper
import com.davidogrady.irishexchange.util.MessageDateFormatter
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessageRow(val latestMessage: ChatMessage,
                       val userPartnerId: String,
                       val loggedInUser: User,
                       private val databaseManager: DatabaseManager): Item<ViewHolder>() {
    var chatPartnerUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        databaseManager.fetchUserFromDatabase(userPartnerId, fetchUserSuccessHandler = {
            // display message date depending on current date
            val formattedDateString = MessageDateFormatter()
                .getDisplayMessageDate(latestMessage.currentDateTimeText)

            chatPartnerUser = it

            val decryptedMessage: String

            // start decrypting message
            if (latestMessage.fromId == loggedInUser.uid)
                decryptedMessage = decryptLatestMessage(latestMessage.encryptedMessage, loggedInUser) ?: return@fetchUserFromDatabase
            else
                decryptedMessage = decryptLatestMessage(latestMessage.encryptedMessage, chatPartnerUser!!) ?: return@fetchUserFromDatabase

            val messageRead = latestMessage.messageRead

            if (messageRead != null) {
                if(messageRead)
                    bindReadMessageToView(viewHolder, decryptedMessage)
                else
                    bindUnreadMessageToView(viewHolder, decryptedMessage)
            } else
                bindReadMessageToView(viewHolder, decryptedMessage)

            if (formattedDateString.isNotEmpty())
                viewHolder.itemView.message_textview_latest_message_date_time.text =
                    formattedDateString

            val targetImageView = viewHolder.itemView.imageview_latest_message
            Picasso.get().load(chatPartnerUser?.profileImageUrl)
                .error(R.drawable.ic_user_generic)
                .placeholder(R.drawable.ic_user_generic)
                .centerCrop()
                .resize(200,200)
                .into(targetImageView)

        }, fetchUserFailureHandler = {
        })

    }

    private fun bindReadMessageToView(viewHolder: ViewHolder, decryptedMessage: String?) {
        viewHolder.itemView.username_textview_latest_message.text = chatPartnerUser!!.username
        viewHolder.itemView.username_textview_latest_message_unread.text = ""
        viewHolder.itemView.message_textview_latest_message_unread.text = ""

        if (latestMessage.fromId == loggedInUser.uid) {
            // "you" is the current users message
            viewHolder.itemView.message_textview_latest_message.text = "You: ${decryptedMessage}"
        }
        else {
            viewHolder.itemView.message_textview_latest_message.text = decryptedMessage
        }

    }

    private fun bindUnreadMessageToView(viewHolder: ViewHolder, decryptedMessage: String?) {
        viewHolder.itemView.username_textview_latest_message.text = ""
        viewHolder.itemView.message_textview_latest_message.text = ""
        viewHolder.itemView.username_textview_latest_message_unread.text = chatPartnerUser!!.username
        viewHolder.itemView.message_textview_latest_message_unread.text = decryptedMessage
    }

    private fun decryptLatestMessage(encryptedMessage: ArrayList<Int>, user: User) : String? {
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

    override fun getLayout(): Int {
        return R.layout.latest_message_row
    }
}