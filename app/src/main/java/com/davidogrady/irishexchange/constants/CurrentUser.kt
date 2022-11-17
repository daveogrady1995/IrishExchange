package com.davidogrady.irishexchange.constants

import com.davidogrady.irishexchange.models.User

class CurrentUser {
    companion object {
        var currentUser: User? = null
        var STORAGE_PERMISSIONS_REQUEST_CODE: Int = 1
    }
}