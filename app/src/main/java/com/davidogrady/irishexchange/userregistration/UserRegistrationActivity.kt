package com.davidogrady.irishexchange.userregistration

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.constants.CurrentUser
import com.davidogrady.irishexchange.createprofile.CreateProfileActivity
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.managers.StorageManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.util.ImageCompressor
import de.hdodenhof.circleimageview.CircleImageView

class UserRegistrationActivity :  AppCompatActivity(), UserRegistrationContract.View {

    private lateinit var btnRegister: Button
    private lateinit var btnSelectPhoto: Button
    private lateinit var selectPhotoImageView: CircleImageView
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var txtAlreadyHaveAccount: TextView
    private lateinit var progressBarHolder: FrameLayout

    private var compressedImageByteArray: ByteArray? = null

    override lateinit var presenter: UserRegistrationContract.Presenter

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
        setContentView(R.layout.activity_user_registration)

        supportActionBar?.hide()

        val userRegistrationPresenter = UserRegistrationPresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            StorageManager((applicationContext as IrishExchangeApplication).mStorage),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(applicationContext as IrishExchangeApplication, "LoggedInUser"))

        btnRegister = findViewById(R.id.register_button_register)
        etUsername = findViewById(R.id.username_edittext_register)
        etEmail = findViewById(R.id.email_edittext_register)
        etPassword = findViewById(R.id.password_edittext_register)
        txtAlreadyHaveAccount = findViewById(R.id.already_have_account_text_view)
        btnSelectPhoto = findViewById(R.id.select_photo_button)
        selectPhotoImageView = findViewById(R.id.selectphoto_imageview_register)
        progressBarHolder = findViewById(R.id.progressBarHolderRegister)

        btnRegister.setOnClickListener {
            if(isUserInputValid()) {
                val newUser = getUserRegistrationDetails()
                presenter.registerUser(newUser, etPassword.text.toString(), compressedImageByteArray!!)
            } else
                showMissingFieldsMessage()
        }

        btnSelectPhoto.setOnClickListener {
            if (checkUserHasStoragePermissions())
                loadPhotoFromGallery()
            else
                requestUserStoragePermission()
        }

        txtAlreadyHaveAccount.setOnClickListener {
            navigateToLoginActivity()
            finish()
        }
    }

    // function called when finish photo selector intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            // proceed and check what selected image was
            Toast.makeText(this, R.string.registration_photo_selected_success_message, Toast.LENGTH_SHORT).show()

            compressedImageByteArray = ImageCompressor().getCompressedImageAsByteArray(data.data!!
                , contentResolver)

            renderLocalImageIntoView(data.data!!)
        }
    }

    override fun showRegistrationSuccessMessage() {
        Toast.makeText(this, R.string.registration_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showRegistrationFailureMessage(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("Failure")
            .setMessage(errorMessage)
            .create()
            .show()
    }

    override fun showImageUploadFailureMessage(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
    override fun navigateToCreateProfileScreen() {
        val createProfileIntent = Intent(this, CreateProfileActivity::class.java)
        finish()
        startActivity(createProfileIntent)
    }

    override fun showLoadingIndicator() {
        progressBarHolder.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        progressBarHolder.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CurrentUser.STORAGE_PERMISSIONS_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                    // permission was denied show a message to the user
                    Toast.makeText(this, R.string.allow_storage_permissions, Toast.LENGTH_LONG).show()
                } else
                    loadPhotoFromGallery()
            }
        }
    }

    private fun showMissingFieldsMessage() {
        Toast.makeText(this, R.string.missing_fields_message, Toast.LENGTH_SHORT).show()
    }

    private fun isUserInputValid() : Boolean {
        val email = etEmail.text.toString()
        val username = etUsername.text.toString()
        val password = etPassword.text.toString()

        return (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty() && compressedImageByteArray != null)
    }

    private fun getUserRegistrationDetails() : User {
        val email = etEmail.text.toString()
        val username = etUsername.text.toString()
        return User(email, username, "", "", true,
            "", arrayListOf(), arrayListOf(), arrayListOf() ,null)
    }

    private fun navigateToLoginActivity() {
        val userLoginIntent = Intent(this, UserLoginActivity::class.java)
        startActivity(userLoginIntent)
    }

    private fun loadPhotoFromGallery() {
        val selectPhotoIntent = Intent()
        selectPhotoIntent.type = "image/*"
        selectPhotoIntent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(selectPhotoIntent, "Select Picture"), 1)
    }

    @Suppress("DEPRECATION")
    private fun renderLocalImageIntoView(selectedPhotoUri: Uri) {

        val bitmap: Bitmap

        // Android Q support and above for rendering bitmap image
        if (Build.VERSION.SDK_INT > 28){
            val source: ImageDecoder.Source = ImageDecoder.createSource(contentResolver, selectedPhotoUri)
            bitmap = ImageDecoder.decodeBitmap(source)
        } else{
            // all android versions below
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
        }
        selectPhotoImageView.setImageBitmap(bitmap)
        btnSelectPhoto.alpha = 0f
    }

    private fun checkUserHasStoragePermissions() : Boolean {

        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestUserStoragePermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CurrentUser.STORAGE_PERMISSIONS_REQUEST_CODE)
    }

}