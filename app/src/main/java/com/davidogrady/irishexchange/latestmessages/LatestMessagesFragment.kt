package com.davidogrady.irishexchange.latestmessages

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.davidogrady.irishexchange.createprofile.CreateProfileActivity
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder


class LatestMessagesFragment : Fragment(), LatestMessagesContract.View {

    private lateinit var progressBarHolder: FrameLayout
    private lateinit var recyclerViewLatestMessages: RecyclerView
    private lateinit var noNewMessagesTextViewHolder: FrameLayout

    override lateinit var presenter: LatestMessagesContract.Presenter

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val latestMessagesPresenter = LatestMessagesPresenter(
            this,
            AuthManager((activity!!.applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((activity!!.applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(activity!!.applicationContext as IrishExchangeApplication, "LoggedInUser"))

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_latest_messages, container, false)

        progressBarHolder = view.findViewById(R.id.progressbar_holder_latest_messages)
        noNewMessagesTextViewHolder = view.findViewById(R.id.no_messages_textview_holder_latest_messages)
        recyclerViewLatestMessages = view.findViewById(R.id.recyclerview_latest_messages)

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (NetworkAvailability().isInternetAvailable(activity as Context)) {
                presenter.updateUserActiveStatus(loggedInUserUid)
            } else {
                showNetworkUnavailableMessage()
            }

            presenter.fetchCurrentUserAndListenForLatestMessages(loggedInUserUid)

        } else
            navigateToLoginScreen()

        return view
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


    override fun navigateToLoginScreen() {
        val navigateToLoginScreenIntent = Intent(activity, UserLoginActivity::class.java)
        activity!!.finish()
        startActivity(navigateToLoginScreenIntent)
    }

    override fun updateLatestMessagesRecyclerView(adapter: GroupAdapter<ViewHolder>) {
        recyclerViewLatestMessages.adapter = adapter
    }

    override fun showLoadingIndicator() {
        progressBarHolder.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        progressBarHolder.visibility = View.GONE
    }

    override fun showNoNewMessagesText() {
        noNewMessagesTextViewHolder.visibility = View.VISIBLE
    }

    override fun hideNoNewMessagesText() {
        noNewMessagesTextViewHolder.visibility = View.GONE
    }

    override fun showNetworkUnavailableMessage() {
        Toast.makeText(activity, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToChatLogActivityWithLatestMessagesRowBundle(chatPartnerUser: User) {
        val chatLogIntent = Intent(activity, ChatLogActivity::class.java)
        chatLogIntent.putExtra(USER_KEY_CHAT_LOG, chatPartnerUser)
        startActivity(chatLogIntent)
    }

    override fun navigateToCreateProfileActivity() {
        val createProfileScreenIntent = Intent(activity, CreateProfileActivity::class.java)
        activity!!.finish()
        startActivity(createProfileScreenIntent)
    }

    override fun isNetworkAvailable() : Boolean {
        return NetworkAvailability().isInternetAvailable(activity!!.applicationContext)
    }

    override fun deleteMessageAlertDialog(adapterView: View, adapterItem: Item<ViewHolder>) {
        val builder: AlertDialog.Builder = activity?.let {
            AlertDialog.Builder(it)
        } ?: return

        builder.setMessage("Would you like to delete this message?")
            .setTitle("Delete message")

        builder.apply {
            setPositiveButton(R.string.user_log_out_dialog_yes) { _, _ ->
                removeRecyclerViewItemHighlight(adapterView)
                presenter.deleteLatestMessageFromDatabase(adapterItem)
            }
            setNegativeButton(R.string.user_log_out_dialog_no) { _, _ -> }
        }
        builder.setOnDismissListener {
            removeRecyclerViewItemHighlight(adapterView)
        }
        val dialog: AlertDialog? = builder.create()

        dialog!!.show()
    }

    override fun showMessageDeletedToast() {
        Toast.makeText(activity, "Message has been deleted", Toast.LENGTH_SHORT).show()
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

     private fun removeRecyclerViewItemHighlight(adapterView: View) {
         adapterView.setBackgroundColor(Color.parseColor("#FFFFFF"))
     }

}
