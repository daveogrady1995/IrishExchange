package com.davidogrady.irishexchange.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class User(
    var email: String,
    var username: String,
    var uid: String,
    var profileImageUrl: String,
    var userOnline: Boolean?,
    var deviceRegToken: String,
    var publicKey: ArrayList<Int>,
    var blockedUsers: ArrayList<String>,
    var reportedUsers: ArrayList<String>,
    var userProfile: @RawValue UserProfile?): Parcelable {

    constructor() :
            this("",
                "",
                "",
                "",
                null,
                "",
                arrayListOf(),
                arrayListOf(),
                arrayListOf(),
               null)
}
