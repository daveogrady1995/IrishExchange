package com.davidogrady.irishexchange.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.chatlog.ChatLogActivity
import com.davidogrady.irishexchange.constants.BundleKeys
import com.davidogrady.irishexchange.models.User
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val ADMIN_CHANNEL_ID = "admin_channel"
    private lateinit var messageJsonObject: JSONObject

    // keep track of who is sending notifications for this session
    private var notificationSenders = ArrayList<String>()

    // called whenever a new notification is received
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val isAppInForeground = IrishExchangeApplication().isAppInForeground

        val messageJson = remoteMessage.data["data"]

        try {
            messageJsonObject = JSONObject(messageJson!!)
        } catch (e: Exception) {
            Log.e("FCM Parse JSON", "Could not parse JSON")
        }

        val chatLogIntent = Intent(this, ChatLogActivity::class.java)

        val fromUser = Gson().fromJson(messageJsonObject.getString("fromUser"), User::class.java)
        // pass user partner to intent so we can navigate to that conversation
        chatLogIntent.putExtra(BundleKeys.USER_KEY_CHAT_LOG, fromUser)
        chatLogIntent.putExtra(BundleKeys.USER_KEY_CHAT_LOG_NOTIFICATION, true)
        chatLogIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        chatLogIntent.action = Intent.ACTION_MAIN
        chatLogIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID: Int = Random().nextInt(3000)

        /*
        Apps targeting SDK 26 or above (Android O) must implement notification channels and add its notifications
        to at least one of them. Therefore, confirm if version is Oreo or higher, then setup notification channel
      */
        /*
        Apps targeting SDK 26 or above (Android O) must implement notification channels and add its notifications
        to at least one of them. Therefore, confirm if version is Oreo or higher, then setup notification channel
      */if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupChannels(notificationManager)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, chatLogIntent,
            PendingIntent.FLAG_ONE_SHOT
        )

       /* val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.ic_menu_gallery
        )*/

        val notificationSoundUri: Uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(messageJsonObject.getString("title"))
                .setContentText(messageJsonObject.getString("body"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())

        // if the same user is sending the notification do not sound / vibrate more than once
        if (!notificationSenders.contains(fromUser.uid)) {
            notificationBuilder.apply {
                setSound(notificationSoundUri)
                setVibrate(LongArray(0))
            }
            notificationSenders.add(fromUser.uid)
        }


        /*insertUserProfilePicassoImageIntoBitmap(bitmapLoadedComplete = {
            bitmapUserProfile = it
            if (bitmapUserProfile != null)
                notificationBuilder.setLargeIcon(bitmapUserProfile)
        })*/

        val url = URL(fromUser.profileImageUrl)
        var userProfileBitmap: Bitmap?
        try {
            userProfileBitmap = BitmapFactory.decodeStream(url.openStream())
        } catch (e: Exception) {
            userProfileBitmap = null
        }

        if (userProfileBitmap != null)
            notificationBuilder.setLargeIcon(userProfileBitmap)


        //Set notification color to match your app color template
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.color = ContextCompat.getColor(this, R.color.colorPrimary)
        }

        if (!notificationSenders.contains(fromUser.uid))
            notificationSenders.add(fromUser.uid)

        notificationManager.notify(notificationID, notificationBuilder.build())

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(notificationManager: NotificationManager?) {
        val adminChannelName: CharSequence = "New notification"
        val adminChannelDescription = "Device to devie notification"
        val adminChannel: NotificationChannel
        adminChannel = NotificationChannel(
            ADMIN_CHANNEL_ID,
            adminChannelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        adminChannel.description = adminChannelDescription
        adminChannel.enableLights(true)
        adminChannel.lightColor = Color.RED
        adminChannel.enableVibration(true)
        notificationManager?.createNotificationChannel(adminChannel)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }
}

