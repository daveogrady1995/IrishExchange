package com.davidogrady.irishexchange.chatlog

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.activities.MainActivity
import com.davidogrady.irishexchange.constants.BundleKeys.Companion.USER_KEY_CHAT_LOG
import com.davidogrady.irishexchange.constants.BundleKeys.Companion.USER_KEY_CHAT_LOG_NOTIFICATION
import com.davidogrady.irishexchange.constants.BundleKeys.Companion.USER_KEY_VIEW_PROFILE
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.davidogrady.irishexchange.viewprofile.ViewProfileActivity
import com.google.android.material.tabs.TabLayout
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder


class ChatLogActivity : AppCompatActivity(), ChatLogContract.View {

    private val defaultScreenWidth = 1080

    private lateinit var recyclerViewChatLog: RecyclerView
    private lateinit var recyclerViewHelperChatLog: RecyclerView
    private lateinit var sendButtonChatLog: ImageView
    private lateinit var irishHelperButtonChatLog: ImageView
    private lateinit var editTextChatLogChatLog: TextView
    private lateinit var chatLogActivityRootView: View
    private lateinit var progressBarHolder: FrameLayout
    private lateinit var linearLayoutIrishHelper: LinearLayout
    private lateinit var tabLayout: TabLayout

    private lateinit var userPartner: User
    private var loggedInUserUid: String? = null

    private var irishHelperVisible = false

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    override lateinit var presenter: ChatLogContract.Presenter

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        // display back button
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        recyclerViewChatLog = findViewById(R.id.recyclerview_chat_log)
        recyclerViewHelperChatLog = findViewById(R.id.recyclerview_irish_helper)
        sendButtonChatLog = findViewById(R.id.send_button_chat_log)
        irishHelperButtonChatLog = findViewById(R.id.dictionary_button_chat_log)
        editTextChatLogChatLog = findViewById(R.id.edittext_chat_log)
        chatLogActivityRootView = findViewById(R.id.activityRoot)
        progressBarHolder = findViewById(R.id.progressBarHolderChatLog)
        linearLayoutIrishHelper = findViewById(R.id.linear_layout_irish_helper)
        tabLayout = findViewById(R.id.tab_layout)

        sharedPreferencesManager =
            SharedPreferencesManager(
                applicationContext,
                "LoggedInUser")

        val chatLogPresenter = ChatLogPresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(applicationContext as IrishExchangeApplication, "LoggedInUser"))

        // The callback can be enabled or disabled here or in handleOnBackPressed()

        loggedInUserUid = presenter.getLoggedInUserUid()
        val currentUserUid = loggedInUserUid

        if (currentUserUid != null) {

            userPartner = getUserBundleDataFromPreviousActivity()!!

            // if we came here from a notification we need to tell the user to turn on their
            // network in order to fetch latest messages. maybe a fix needed in future
            if (checkIfUserHasPassedNotificationBundle() && !isNetworkAvailable())
                displayNotificationNetworkUnavailableMessage()

            presenter.fetchUserPartnersAndListenForMessages(userPartner, currentUserUid)

            presenter.fetchUserFromDatabase(currentUserUid)
        } else
            navigateToLoginScreen()

        sendButtonChatLog.setOnClickListener {
            val message = editTextChatLogChatLog.text.toString()
            if (message.isNotBlank())
                presenter.performSendMessage(userPartner, message)
        }

        irishHelperButtonChatLog.setOnClickListener {
            toggleIrishHelperVisibility()
            hideKeyboard()
        }

        editTextChatLogChatLog.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus)
                scrollToBottom()
        }

        editTextChatLogChatLog.setOnClickListener {
            editTextChatLogChatLog.requestFocus()
            linearLayoutIrishHelper.visibility = View.GONE
            scrollToBottom()
        }

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab) {
                presenter.updateFlashcardsInIrishHelperTab(tab.position)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

        })

        val backButtonCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() { // Handle the back button event
                    // if irish helper is open close it otherwise leave the activity
                    if (irishHelperVisible)
                        toggleIrishHelperVisibility()
                    else {
                        presenter.removeChatLogMessagesListener()
                        // if this activity is root of the stack navigate to main activity
                        if (isTaskRoot)
                            navigateToMainActivity()
                        else
                            finish()
                    }
                }
            }
        onBackPressedDispatcher.addCallback(this, backButtonCallback)

        supportActionBar?.title = userPartner.username

        val screenWidth = getScreenWidth()

        // irish helper recycler view grid config
        if (screenWidth < defaultScreenWidth)
            setRecyclerViewGridConfig(2)
        else
            setRecyclerViewGridConfig(3)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav_menu_chat_log, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                presenter.removeChatLogMessagesListener()
                // if this activity is root of the stack navigate to main activity
                if (isTaskRoot)
                    navigateToMainActivity()
                else
                    finish()
            }
            R.id.menu_view_profile_user_chat_log_screen -> {
                val chatPartner = presenter.getChatPartner()
                if (chatPartner != null)
                    navigateToViewProfileActivityWithUserBundle(chatPartner)
            }

            R.id.menu_block_user_chat_log_screen -> {
                showBlockUserAlertDialog()
            }

            R.id.menu_report_user_chat_log_screen -> {
                showReportUserAlertDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateChatLogRecyclerView(adapter: GroupAdapter<ViewHolder>) {
        recyclerViewChatLog.adapter = adapter
    }

    override fun updateIrishHelperGridView(adapter: GroupAdapter<ViewHolder>) {
        recyclerViewHelperChatLog.adapter = adapter
    }

    override fun displayUnableToFetchMessagesError() {
        Toast.makeText(this, R.string.fetch_chat_log_failure_message, Toast.LENGTH_SHORT).show()
    }

    override fun displayUnableToSendMessageError() {
        Toast.makeText(this, R.string.chat_log_message_sent_failure, Toast.LENGTH_SHORT).show()
    }

    override fun displayNotificationNetworkUnavailableMessage() {
        Toast.makeText(this, R.string.network_unavailable_message_notification, Toast.LENGTH_LONG).show()
    }

    override fun displayReportUserSuccessMessage() {
        Toast.makeText(this, R.string.report_user_success_message, Toast.LENGTH_LONG).show()
    }

    override fun displayReportUserFailureMessage() {
        Toast.makeText(this, R.string.report_user_failure_message, Toast.LENGTH_LONG).show()
    }

    override fun displayBlockedUserSuccessMessage() {
        Toast.makeText(this, R.string.block_user_success_message, Toast.LENGTH_LONG).show()
    }

    override fun displayBlockedUserFailureMessage() {
        Toast.makeText(this, R.string.block_user_failure_message, Toast.LENGTH_LONG).show()
    }

    override fun displayAlreadyReportedUserMessage() {
        Toast.makeText(this, R.string.already_reported_user_message, Toast.LENGTH_LONG).show()
    }

    override fun displayAlreadyBlockedUserMessage() {
        Toast.makeText(this, R.string.already_blocked_user_message, Toast.LENGTH_LONG).show()
    }

    override fun clearChatLogInput() {
        editTextChatLogChatLog.text = ""
    }

    override fun updateChatLogRecyclerViewPosition(adapter: GroupAdapter<ViewHolder>) {
        recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onDestroy() {
        presenter.removeChatLogMessagesListener()
        super.onDestroy()
    }

    override fun isNetworkAvailable() : Boolean {
        return NetworkAvailability().isInternetAvailable(this)
    }

    override fun showLoadingIndicator() {
        progressBarHolder.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        progressBarHolder.visibility = View.GONE
    }

    override fun getUserPartner() : User? {
        return userPartner
    }

    private fun setRecyclerViewGridConfig(columns: Int) {
        recyclerViewHelperChatLog.layoutManager = GridLayoutManager(this, columns)
    }

    private fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = this.currentFocus
        view?.let { v ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private fun toggleIrishHelperVisibility() {
        if (!irishHelperVisible) {
            linearLayoutIrishHelper.visibility = View.VISIBLE
            irishHelperVisible = true
            scrollToBottom()
        } else {
            linearLayoutIrishHelper.visibility = View.GONE
            irishHelperVisible = false
        }


    }

    private fun showReportUserAlertDialog() {

        val builder = AlertDialog.Builder(this)

        builder.setMessage(R.string.report_user_dialog_message)
            .setTitle(R.string.report_user_dialog_title)

        builder.apply {
            setPositiveButton(R.string.report_user_dialog_yes) { _, _ ->
                if (isNetworkAvailable())
                    presenter.reportUser()
                else
                    displayNetworkUnavailableMessage()
            }
            setNegativeButton(R.string.report_user_dialog_no) { _, _ -> }
        }
        val dialog: AlertDialog? = builder.create()

        dialog!!.show()
    }

    private fun showBlockUserAlertDialog() {

        val builder = AlertDialog.Builder(this)

        builder.setMessage(R.string.block_user_dialog_message)
            .setTitle(R.string.block_user_dialog_title)

        builder.apply {
            setPositiveButton(R.string.report_user_dialog_yes) { _, _ ->
                if (isNetworkAvailable())
                    presenter.blockUser()
                else
                    displayNetworkUnavailableMessage()
            }
            setNegativeButton(R.string.report_user_dialog_no) { _, _ -> }
        }
        val dialog: AlertDialog? = builder.create()

        dialog!!.show()
    }

    private fun displayNetworkUnavailableMessage() {
        Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToViewProfileActivityWithUserBundle(chatPartnerUser: User) {
        val navigateToViewProfileScreenIntent = Intent(this, ViewProfileActivity::class.java)
        navigateToViewProfileScreenIntent.putExtra(USER_KEY_VIEW_PROFILE, chatPartnerUser)
        startActivity(navigateToViewProfileScreenIntent)
    }

    private fun navigateToLoginScreen() {
        val navigateToLoginScreenIntent = Intent(this, UserLoginActivity::class.java)
        finish()
        startActivity(navigateToLoginScreenIntent)
    }

    override fun navigateToMainActivity() {
        val navigateToMainActivityIntent = Intent(this, MainActivity::class.java)
        finish()
        startActivity(navigateToMainActivityIntent)
    }

    private fun getUserBundleDataFromPreviousActivity() : User? {
        return intent.getParcelableExtra(USER_KEY_CHAT_LOG)
    }

    override fun checkIfUserHasPassedNotificationBundle() : Boolean {
        return intent.getBooleanExtra(USER_KEY_CHAT_LOG_NOTIFICATION, false)
    }

    private fun scrollToBottom() {
        // wait for keyboard to show up first
        Handler().postDelayed({ recyclerViewChatLog.scrollToPosition(
            recyclerViewChatLog.adapter!!.itemCount - 1) }, 500)
    }

}
