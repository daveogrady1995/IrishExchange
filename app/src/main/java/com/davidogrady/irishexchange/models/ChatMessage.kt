package com.davidogrady.irishexchange.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val referenceId: String,
    val encryptedMessage: ArrayList<Int>,
    val fromId: String,
    val toId: String,
    val currentTimeMillis: Long,
    val currentDateTimeText: String,
    var messageRead: Boolean?) {
    constructor() : this(
        "",
        arrayListOf(),
        "",
        "",
        0,
        "",
        null)
}


