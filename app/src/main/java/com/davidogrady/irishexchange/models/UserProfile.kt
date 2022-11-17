package com.davidogrady.irishexchange.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserProfile(
    var irishLevel: String,
    var irishDialect: String,
    var gender: String,
    var location: String,
    var age: Int,
    var bio: String) : Parcelable {

    constructor() : this("", "", "", "", 0, "")
}
