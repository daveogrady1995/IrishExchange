package com.davidogrady.irishexchange.util

import com.davidogrady.irishexchange.constants.SecretKeys
import com.davidogrady.irishexchange.models.NotificationRequest
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception


class FcmNotificationApi {

    private val fcmGoogleApiUrl = "https://fcm.googleapis.com/fcm/send"

     fun sendNotificationToDevice(notificationRequest: NotificationRequest) {

         val messageDataObject = JSONObject()

         val fromUserJson = Gson().toJson(notificationRequest.fromUser)

         messageDataObject.put("body", notificationRequest.message)
         messageDataObject.put("title", notificationRequest.title)
         messageDataObject.put("userPhotoUrl", notificationRequest.fromUser.profileImageUrl)
         messageDataObject.put("fromUser", fromUserJson)

         // form parameters
         val formBody: RequestBody = FormBody.Builder()
             .add("to", notificationRequest.deviceRegToken)
             .add("data", messageDataObject.toString())
             .build()

         val httpClient = OkHttpClient()

         val request = Request.Builder()
             .url(fcmGoogleApiUrl)
             .addHeader("Authorization", "key=${SecretKeys.SERVER_KEY}")
             .addHeader("Content-Type", "application/json")
             .post(formBody)
             .build()

         try {
             httpClient.newCall(request).enqueue(object: Callback {
                 override fun onFailure(call: Call, e: IOException) {
                 }
                 override fun onResponse(call: Call, response: Response) {
                 }
             })

         } catch (e: Exception) { }
     }
}