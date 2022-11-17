package com.davidogrady.irishexchange.util

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging

class FirebaseDeviceRegistration {

    fun getDeviceRegistrationToken(deviceRegTokenCompleteListener: (token: String) -> Unit) {
        // On initial startup of your app, the FCM SDK generates a registration token for the
        // client app instance
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    //Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                } else {
                    // This value persists across app restarts once set.
                    FirebaseMessaging.getInstance().isAutoInitEnabled = true
                    // Get new Instance ID token
                    val deviceToken = task.result?.token ?: ""
                    deviceRegTokenCompleteListener.invoke(deviceToken)
                }
            })
    }


}