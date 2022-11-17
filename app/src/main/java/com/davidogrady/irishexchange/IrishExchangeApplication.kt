package com.davidogrady.irishexchange

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.davidogrady.irishexchange.constants.CurrentUser
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.util.FirebaseDeviceRegistration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

// we don't want to have multiple instances that open up multiple connections to Firebase at the same time
// this class can be used to handle some of the life cycle of the entire application outside of the scope of
// activities and fragments. We can create a single instance that lives in the application and get a reference to these
// within the activities and fragments

class IrishExchangeApplication: Application(), LifecycleObserver  {
    lateinit var mAuth: FirebaseAuth
        private set
    lateinit var mStorage: FirebaseStorage
        private set
    lateinit var mDatabase: FirebaseDatabase
        private set
    lateinit var mSharedPreferencesManager: SharedPreferencesManager
        private set

    lateinit var mDatabaseManager: DatabaseManager
        private set

     var isAppInForeground = false

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()

        mDatabase = FirebaseDatabase.getInstance()
        // support offline capabilities saves data into cache
        mDatabase.setPersistenceEnabled(true)

        mDatabaseManager = DatabaseManager(mDatabase)

        mSharedPreferencesManager =
            SharedPreferencesManager(
                applicationContext,
                "DeviceRegistrationToken")

        fetchAndListenForCurrentUser()
    }

    // when the app is in foreground tell Firebase that the user is now active on the app
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun appInForeground() {
        isAppInForeground = true
        val currentUserUid = mAuth.uid
        if (!currentUserUid.isNullOrEmpty()) {
            mDatabaseManager.updateUserActiveStatus(currentUserUid)
            getDeviceRegistrationToken()
        }
    }

    // grab current user and update the companion object when any changes occur
    // if I get time I can implement this for the entire app
    private fun fetchAndListenForCurrentUser() {
        val loggedInUid = mAuth.uid

        if (loggedInUid != null)
            mDatabaseManager.fetchUserFromDatabaseAndListenForChanges(loggedInUid, fetchUserSuccessHandler = {
                CurrentUser.currentUser = it
            }, fetchUserFailureHandler = {
            })
    }


    // when the app stops or in background tell Firebase that the user is not active on the app
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun appInBackground() {
        isAppInForeground = false
        val currentUserUid = mAuth.uid
        if (!currentUserUid.isNullOrEmpty())
            mDatabaseManager.updateUserInactiveStatus(currentUserUid)
    }

    private fun getDeviceRegistrationToken() {
        FirebaseDeviceRegistration().getDeviceRegistrationToken(
            deviceRegTokenCompleteListener = { firebaseToken ->
                // if Firebase token is different from stored token then update this value in shared preferences
                // and the database
                updateDeviceRegistrationTokenIfChanged(firebaseToken)
            })
    }

    private fun updateDeviceRegistrationTokenInDatabase(token: String) {
        val currentUserUid = mAuth.uid
        if (!currentUserUid.isNullOrEmpty())
            mDatabaseManager.updateUserDeviceRegToken(currentUserUid, token)
    }

    private fun updateDeviceRegistrationTokenIfChanged(firebaseToken: String) {
        val currentUserUid = mAuth.uid
        if (!currentUserUid.isNullOrEmpty()) {
            val storedToken = mSharedPreferencesManager.getStoredPreferenceString("deviceToken")
            if (firebaseToken != storedToken) {
                mSharedPreferencesManager.storePreferenceKey("deviceToken", firebaseToken)
                updateDeviceRegistrationTokenInDatabase(firebaseToken)
            }

        }
    }
}