package com.davidogrady.irishexchange.createprofile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.activities.MainActivity
import com.davidogrady.irishexchange.constants.ProfileSettings
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.models.UserProfile
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.google.android.material.textfield.TextInputEditText
import java.lang.NumberFormatException

class CreateProfileActivity : AppCompatActivity(), CreateProfileContract.View {

    override lateinit var presenter: CreateProfileContract.Presenter

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

        val createProfilePresenter = CreateProfilePresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase))

        irishLevelDropdown = findViewById(R.id.filled_exposed_dropdown_irish_level)
        irishDialectDropdown = findViewById(R.id.filled_exposed_dropdown_irish_dialect)
        genderDropdown = findViewById(R.id.filled_exposed_dropdown_gender)
        locationEditText = findViewById(R.id.text_input_edit_text_location)
        ageEditText = findViewById(R.id.text_input_edit_text_age)
        bioEditText = findViewById(R.id.text_input_edit_text_bio)

        setupDropDownLists()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav_menu_edit_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_confirm_edit_profile -> {
                createUserProfileDetails()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun navigateToLoginScreen() {
        val userLoginIntent = Intent(this, UserLoginActivity::class.java)
        startActivity(userLoginIntent)
        finish()
    }

    override fun showMissingFieldsMessage() {
        Toast.makeText(this, R.string.incorrect_missing_fields_message, Toast.LENGTH_SHORT).show()
    }

    override fun showCreateProfileSuccessMessage() {
        Toast.makeText(this, R.string.save_profile_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showCreateProfileFailureMessage() {
        Toast.makeText(this, R.string.save_profile_failure_message, Toast.LENGTH_SHORT).show()
    }

    override fun showNetworkUnavailableMessage() {
        Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToRegistrationScreen() {
        val navigateToRegScreenIntent = Intent(this, CreateProfileActivity::class.java)
        finish()
        startActivity(navigateToRegScreenIntent)
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

    override fun navigateToMainActivity() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        finish()
        startActivity(mainActivityIntent)
    }

    private fun isUserProfileValid(userProfile: UserProfile) : Boolean {

        return (!userProfile.irishLevel.isNullOrEmpty()
                && !userProfile.irishDialect.isNullOrEmpty()
                && !userProfile.gender.isNullOrEmpty()
                && !userProfile.location.isNullOrEmpty()
                && (userProfile.age > 0)
                && (userProfile.age < 112)
                && !userProfile.irishLevel.isNullOrEmpty())
    }

    private fun createUserProfileDetails() {
        val irishLevel = irishLevelDropdown.text.toString()
        val irishDialect = irishDialectDropdown.text.toString()
        val gender = genderDropdown.text.toString()
        val location = locationEditText.text.toString()
        val age = try { ageEditText.text.toString().toInt() } catch (e: NumberFormatException) { 0 }
        val bio = bioEditText.text.toString()

        val userProfile = UserProfile(irishLevel, irishDialect, gender, location, age, bio)

        if (NetworkAvailability().isInternetAvailable(this)) {
            if (presenter.isUserLoggedIn()) {
                if (isUserProfileValid(userProfile))
                    presenter.createUserProfile(userProfile)
                else
                    showMissingFieldsMessage()
            }
            else {
                navigateToMainActivity()
            }
        }
        else
            showNetworkUnavailableMessage()
    }
}
