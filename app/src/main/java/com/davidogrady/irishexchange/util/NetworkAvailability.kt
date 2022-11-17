package com.davidogrady.irishexchange.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import java.lang.NullPointerException


class NetworkAvailability {
    @Suppress("DEPRECATION")
    fun isInternetAvailable(context: Context): Boolean {
        var activeNetworkInfo: NetworkInfo? = null

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (cm != null) {
            activeNetworkInfo = try { cm.activeNetworkInfo} catch (e: NullPointerException) { null }
        }

        if (activeNetworkInfo != null)  {
            return activeNetworkInfo.isConnected
        } else {
            return false
        }
    }
}