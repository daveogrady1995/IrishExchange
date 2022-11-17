package com.davidogrady.irishexchange.viewprofile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.constants.BundleKeys
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.userregistration.UserRegistrationActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class ViewProfileActivity : AppCompatActivity(), ViewProfileContract.View {

    private lateinit var progressBarHolderFragment: FrameLayout
    private lateinit var selectPhotoImageView: CircleImageView
    private lateinit var textviewUsername: TextView
    private lateinit var textviewAgeGenderTown: TextView
    private lateinit var textviewIrishLevel: TextView
    private lateinit var textviewIrishDialect: TextView
    private lateinit var textviewAboutMe: TextView

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    override lateinit var presenter: ViewProfileContract.Presenter

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
        setContentView(R.layout.activity_view_profile)
        // display back button
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        sharedPreferencesManager =
            SharedPreferencesManager(
                applicationContext,
                "LoggedInUser")

        val viewProfilePresenter = ViewProfilePresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            SharedPreferencesManager(applicationContext as IrishExchangeApplication, "LoggedInUser"))

        progressBarHolderFragment = findViewById(R.id.progressbar_holder_view_profile)
        selectPhotoImageView = findViewById(R.id.view_profile_select_photo_image_view)
        textviewUsername = findViewById(R.id.textview_viewprofile_username)
        textviewAgeGenderTown = findViewById(R.id.textview_viewprofile_age_gender_town)
        textviewIrishLevel = findViewById(R.id.textview_viewprofile_irish_level)
        textviewIrishDialect = findViewById(R.id.textview_viewprofile_irish_dialect)
        textviewAboutMe = findViewById(R.id.textview_viewprofile_about_me)

        showLoadingIndicatorFragment()

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (!NetworkAvailability().isInternetAvailable(this as Context))
                showNetworkUnavailableMessage()

            val userToView = getUserBundleDataFromPreviousActivity() ?: return

            supportActionBar?.title = userToView.username


            presenter.displayProfileDetails(userToView)

        } else
            navigateToRegistrationScreen()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

/*    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_nav_menu_my_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_show_edit_profile_screen -> {
                navigateToEditProfileScreen()
            }
        }
        return super.onOptionsItemSelected(item)
    }*/

    override fun showLoadingIndicatorFragment() {
        progressBarHolderFragment.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicatorFragment() {
        progressBarHolderFragment.visibility = View.GONE
    }

    override fun showNetworkUnavailableMessage() {
        Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToRegistrationScreen() {
        val navigateToRegScreenIntent = Intent(this, UserRegistrationActivity::class.java)
        finish()
        startActivity(navigateToRegScreenIntent)
    }

    override fun populateViewWithUserProfileDetails(user: User) {
        if (user.userProfile != null) {
            textviewUsername.text = user.username
            textviewAgeGenderTown.text = "${user.userProfile!!.age} " +
                    "${user.userProfile!!.gender}, " +
                    "${user.userProfile!!.location}"
            textviewIrishLevel.text = user.userProfile!!.irishLevel
            textviewIrishDialect.text = user.userProfile!!.irishDialect
            textviewAboutMe.text = user.userProfile!!.bio
            renderFirebaseImageIntoView(user.profileImageUrl)
        }
    }

    override fun displayUnableToRetrieveProfileErrorMessage() {
        Toast.makeText(this, R.string.fetch_user_profile_error_message, Toast.LENGTH_SHORT).show()
    }

    override fun renderFirebaseImageIntoView(profileImageUrl: String) {
        Picasso.get()
            .load(profileImageUrl)
            .error(R.drawable.ic_user_generic)
            .placeholder(R.drawable.ic_user_generic)
            .centerCrop()
            .resize(1000,1000)
            .into(selectPhotoImageView)
    }

    private fun getUserBundleDataFromPreviousActivity() : User? {
        return intent.getParcelableExtra(BundleKeys.USER_KEY_VIEW_PROFILE)
    }

    private fun loadPhotoFromGallery() {
        val selectPhotoIntent = Intent()
        selectPhotoIntent.type = "image/*"
        selectPhotoIntent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(selectPhotoIntent, "Select Picture"), 1)
    }

    private fun networkUnavailableUploadImageMessage() {
        Toast.makeText(this, R.string.network_unavailable_upload_image_message, Toast.LENGTH_SHORT).show()
    }

}
