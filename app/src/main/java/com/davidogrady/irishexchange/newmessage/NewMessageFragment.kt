package com.davidogrady.irishexchange.newmessage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.chatlog.ChatLogActivity
import com.davidogrady.irishexchange.constants.BundleKeys.Companion.USER_KEY_CHAT_LOG
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.holders.UserItem
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.userregistration.UserRegistrationActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

class NewMessageFragment : Fragment(), NewMessageContract.View {

    private lateinit var progressBarHolder: FrameLayout
    private lateinit var recyclerViewNewMessage: RecyclerView
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override lateinit var presenter: NewMessageContract.Presenter

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_message, container, false)

        sharedPreferencesManager =
            SharedPreferencesManager(
                activity!!.applicationContext,
                "LoggedInUser")

        val newMessagePresenter = NewMessagePresenter(
            this,
            AuthManager((activity!!.applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((activity!!.applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(activity!!.applicationContext as IrishExchangeApplication, "LoggedInUser"))

        recyclerViewNewMessage = view.findViewById(R.id.recyclerview_newmessage)
        progressBarHolder = view.findViewById(R.id.progressbar_holder_new_message)

        showLoadingIndicator()

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (!NetworkAvailability().isInternetAvailable(activity as Context))
                showNetworkUnavailableMessage()

            presenter.fetchUsers(loggedInUserUid)

        } else
            navigateToRegistrationScreen()

        return view
    }

    override fun updateNewMessageRecyclerView(adapter: GroupAdapter<ViewHolder>) {
        recyclerViewNewMessage.adapter = adapter
    }

    override fun navigateToChatLogActivityWithUserItemBundle(userItem: UserItem) {
        val chatLogIntent = Intent(activity, ChatLogActivity::class.java)
        chatLogIntent.putExtra(USER_KEY_CHAT_LOG, userItem.user)
        startActivity(chatLogIntent)
    }

    override fun showLoadingIndicator() {
        progressBarHolder.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        progressBarHolder.visibility = View.GONE
    }

    override fun navigateToRegistrationScreen() {
        val navigateToRegScreenIntent = Intent(activity, UserRegistrationActivity::class.java)
        activity!!.finish()
        startActivity(navigateToRegScreenIntent)
    }

    override fun showNetworkUnavailableMessage() {
        Toast.makeText(activity, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_nav_menu_default, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_user_logout -> {
                userLogOutAlertDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun userLogOutAlertDialog() {
        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }

        builder!!.setMessage(R.string.user_log_out_dialog_message)
            .setTitle(R.string.user_log_out_dialog_title)

        builder.apply {
            setPositiveButton(R.string.user_log_out_dialog_yes) { _, _ ->
                presenter.logOutUser()
            }
            setNegativeButton(R.string.user_log_out_dialog_no) { _, _ -> }
        }
        val dialog: AlertDialog? = builder.create()

        dialog!!.show()
    }
}
