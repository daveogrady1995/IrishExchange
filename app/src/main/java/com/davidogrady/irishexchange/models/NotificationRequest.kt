package com.davidogrady.irishexchange.models

data class NotificationRequest(
    val title: String,
    val message: String,
    val deviceRegToken: String,
    val fromUser: User)