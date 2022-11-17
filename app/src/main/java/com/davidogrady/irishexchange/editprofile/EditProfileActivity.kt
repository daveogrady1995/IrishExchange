package com.davidogrady.irishexchange.editprofile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.constants.ProfileSettings
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.models.UserProfile
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.google.android.material.textfield.TextInputEditText
import java.lang.NumberFormatException

class EditProfileActivity : AppCompatActivity(), EditProfileContract.View {

    override lateinit var presenter: EditProfileContract.Presenter
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    private lateinit var progressBarHolderEditProfile: FrameLayout
    private lateinit var irishLevelDropdown: AutoCompleteTextView
    private lateinit var irishDialectDropdown: AutoCompleteTextView
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var locationEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var bioEditText: TextInputEditText

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
        setContentView(R.layout.activity_create_profile)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val EditProfilePresenter = EditProfilePresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(applicationContext as IrishExchangeApplication, "LoggedInUser")
        )

        progressBarHolderEditProfile = findViewById(R.id.progressbar_holder_edit_profile)
        irishLevelDropdown = findViewById(R.id.filled_exposed_dropdown_irish_level)
        irishDialectDropdown = findViewById(R.id.filled_exposed_dropdown_irish_dialect)
        genderDropdown = findViewById(R.id.filled_exposed_dropdown_gender)
        locationEditText = findViewById(R.id.text_input_edit_text_location)
        ageEditText = findViewById(R.id.text_input_edit_text_age)
        bioEditText = findViewById(R.id.text_input_edit_text_bio)

        setupDropDownLists()

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (!NetworkAvailability().isInternetAvailable(this))
                showNetworkUnavailableMessage()

            presenter.fetchCurrentUserAndPopulateFields(loggedInUserUid)

        } else
            navigateToLoginScreen()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav_menu_edit_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.menu_confirm_edit_profile -> {
                editUserProfileDetails()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun populateEditProfileFields(userProfile: UserProfile) {

        // drop downs won't be pre populated before this API version
        if (Build.VERSION.SDK_INT >= 17) {
            irishLevelDropdown.setText(userProfile.irishLevel, false)
            irishDialectDropdown.setText(userProfile.irishDialect, false)
            genderDropdown.setText(userProfile.gender, false)
        }

        locationEditText.setText(userProfile.location)
        ageEditText.setText(userProfile.age.toString())
        bioEditText.setText(userProfile.bio)
    }

    override fun showEditProfileSuccessMessage() {
        Toast.makeText(this, R.string.save_profile_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showEditProfileFailureMessage() {
        Toast.makeText(this, R.string.save_profile_failure_message, Toast.LENGTH_SHORT).show()
    }

    override fun showMissingFieldsMessage() {
        Toast.makeText(this, R.string.incorrect_missing_fields_message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateBackToCreateProfileFragment() {
        finish()
    }

    override fun disableUserInteractionOnView() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun enableUserInteractionOnView() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun showLoadingIndicatorEditProfile() {
        progressBarHolderEditProfile.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicatorEditProfile() {
        progressBarHolderEditProfile.visibility = View.GONE
    }

    override fun navigateToMainActivity() {
    }

    private fun editUserProfileDetails() {
        val irishLevel = irishLevelDropdown.text.toString()
        val irishDialect = irishDialectDropdown.text.toString()
        val gender = genderDropdown.text.toString()
        val location = locationEditText.text.toString()
        val age = try { ageEditText.text.toString().toInt() } catch (e: NumberFormatException) { 0 }
        val bio = bioEditText.text.toString()

        val userProfile = UserProfile(irishLevel, irishDialect, gender, location, age, bio)

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (NetworkAvailability().isInternetAvailable(this)) {
                if (isUserProfileValid(userProfile)) {
                    showLoadingIndicatorEditProfile()
                    disableUserInteractionOnView()
                    Handler().postDelayed({presenter.createUserProfile(userProfile)}, 1000)
                }
                else
                    showMissingFieldsMessage()
            } else
                showNetworkUnavailableMessage()


        } else
            navigateToLoginScreen()
    }

    private fun isUserProfileValid(userProfile: UserProfile) : Boolean {

        return (userProfile.irishLevel.isNotEmpty()
                && userProfile.irishDialect.isNotEmpty()
                && userProfile.gender.isNotEmpty()
                && userProfile.location.isNotEmpty()
                && (userProfile.age > 0)
                && (userProfile.age < 112)
                && userProfile.irishLevel.isNotEmpty())
    }

    private fun showNetworkUnavailableMessage() {
        Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLoginScreen() {
        val navigateToLoginScreenIntent = Intent(this, UserLoginActivity::class.java)
        finish()
        startActivity(navigateToLoginScreenIntent)
    }

    private fun setupDropDownLists() {
        val irishLevelAdapter = ArrayAdapter<String>(this, R.layout.dropdown_menu_popup_item,
            ProfileSettings.irishLevels)

        val irishDialectAdapter = ArrayAdapter<String>(this, R.layout.dropdown_menu_popup_item,
            ProfileSettings.irishDialects)

        val genderAdapter = ArrayAdapter<String>(this, R.layout.dropdown_menu_popup_item,
            ProfileSettings.genders)

        irishLevelDropdown.setAdapter(irishLevelAdapter)
        irishLevelDropdown.keyListener = null

        irishDialectDropdown.setAdapter(irishDialectAdapter)
        irishDialectDropdown.keyListener = null

        genderDropdown.setAdapter(genderAdapter)
        genderDropdown.keyListener = null
    }
}
