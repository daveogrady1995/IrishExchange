package com.davidogrady.irishexchange.myprofile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.constants.CurrentUser
import com.davidogrady.irishexchange.editprofile.EditProfileActivity
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.managers.StorageManager
import com.davidogrady.irishexchange.models.User
import com.davidogrady.irishexchange.userlogin.UserLoginActivity
import com.davidogrady.irishexchange.userregistration.UserRegistrationActivity
import com.davidogrady.irishexchange.util.ImageCompressor
import com.davidogrady.irishexchange.util.NetworkAvailability
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class MyProfileFragment : Fragment(), MyProfileContract.View {

    private var selectedPhotoUri: Uri? = null

    private lateinit var progressBarHolderFragment: FrameLayout
    private lateinit var progressBarHolderUploadProfile: FrameLayout
    private lateinit var selectPhotoButton: AppCompatButton
    private lateinit var selectPhotoImageView: CircleImageView
    private lateinit var textviewUsername: TextView
    private lateinit var textviewAgeGenderTown: TextView
    private lateinit var textviewIrishLevel: TextView
    private lateinit var textviewIrishDialect: TextView
    private lateinit var textviewAboutMe: TextView

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    override lateinit var presenter: MyProfileContract.Presenter

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

        sharedPreferencesManager =
            SharedPreferencesManager(
                activity!!.applicationContext,
                "LoggedInUser")

        val viewProfilePresenter = MyProfilePresenter(
            this,
            AuthManager((activity!!.applicationContext as IrishExchangeApplication).mAuth),
            StorageManager((activity!!.applicationContext as IrishExchangeApplication).mStorage),
            DatabaseManager((activity!!.applicationContext as IrishExchangeApplication).mDatabase),
            SharedPreferencesManager(activity!!.applicationContext as IrishExchangeApplication, "LoggedInUser"))

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_profile, container, false)

        progressBarHolderFragment = view.findViewById(R.id.progressbar_holder_view_profile)
        progressBarHolderUploadProfile = view.findViewById(R.id.progressbar_holder_upload_profile)
        selectPhotoButton = view.findViewById(R.id.view_profile_select_photo_button)
        selectPhotoImageView = view.findViewById(R.id.view_profile_select_photo_image_view)
        textviewUsername = view.findViewById(R.id.textview_viewprofile_username)
        textviewAgeGenderTown = view.findViewById(R.id.textview_viewprofile_age_gender_town)
        textviewIrishLevel = view.findViewById(R.id.textview_viewprofile_irish_level)
        textviewIrishDialect = view.findViewById(R.id.textview_viewprofile_irish_dialect)
        textviewAboutMe = view.findViewById(R.id.textview_viewprofile_about_me)

        showLoadingIndicatorFragment()
        disableSelectPhotoButton()

        val loggedInUserUid = presenter.getLoggedInUserUid()

        if (!loggedInUserUid.isNullOrEmpty()) {
            if (!NetworkAvailability().isInternetAvailable(activity as Context))
                showNetworkUnavailableMessage()

            presenter.fetchUserAndDisplayProfileDetails(loggedInUserUid)

        } else
            navigateToRegistrationScreen()

        selectPhotoButton.setOnClickListener {

            if (NetworkAvailability().isInternetAvailable(activity as Context))
                if (checkUserHasStoragePermissions(activity as Context))
                    loadPhotoFromGallery()
                else
                    requestUserStoragePermission()
            else
                networkUnavailableUploadImageMessage()


        }

        return view
    }

    // function called when finish photo selector intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            // proceed and check what selected image was

            selectedPhotoUri = data.data

            val compressedImageByteArray = ImageCompressor().getCompressedImageAsByteArray(selectedPhotoUri!!,
                activity!!.contentResolver)

            //selectedPhotoUri = ImageCompressor().getCompressedImageUri(activity!!.applicationContext, compressedBitmap)

            disableUserInteractionOnView()
            showLoadingIndicatorUploadPhoto()

            if (NetworkAvailability().isInternetAvailable(activity as Context)) {
                if (presenter.isUserLoggedIn())
                    presenter.uploadImageToFirebaseStorage(compressedImageByteArray)
                else
                    navigateToLoginScreen()
            } else {
                networkUnavailableUploadImageMessage()
                hideLoadingIndicatorUploadPhoto()
                enableUserInteractionOnView()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
    }

    override fun showLoadingIndicatorFragment() {
        progressBarHolderFragment.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicatorFragment() {
        progressBarHolderFragment.visibility = View.GONE
    }

    override fun showLoadingIndicatorUploadPhoto() {
        progressBarHolderUploadProfile.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicatorUploadPhoto() {
        progressBarHolderUploadProfile.visibility = View.GONE
    }

    override fun showNetworkUnavailableMessage() {
        Toast.makeText(activity, R.string.network_unavailable_message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToRegistrationScreen() {
        val navigateToRegScreenIntent = Intent(activity, UserRegistrationActivity::class.java)
        activity!!.finish()
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
        Toast.makeText(activity, R.string.fetch_user_profile_error_message, Toast.LENGTH_SHORT).show()
    }

    override fun showImageUploadSuccessMessage() {
        Toast.makeText(activity, R.string.upload_image_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showImageUploadFailureMessage() {
        Toast.makeText(activity, R.string.upload_image_failure_message, Toast.LENGTH_SHORT).show()
    }

    override fun renderFirebaseImageIntoView(profileImageUrl: String) {
        Picasso.get()
            .load(profileImageUrl)
            .error(R.drawable.ic_user_generic)
            .placeholder(R.drawable.ic_user_generic)
            .centerCrop()
            .resize(1000,1000)
            .into(selectPhotoImageView)

        selectPhotoButton.alpha = 0f
    }

    override fun disableUserInteractionOnView() {
        activity!!.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun enableUserInteractionOnView() {
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @Suppress("DEPRECATION")
    override fun renderLocalImageIntoView() {

        val bitmap: Bitmap

        // Android Q support and above for rendering bitmap image
        if (Build.VERSION.SDK_INT > 28){
            val source: ImageDecoder.Source = ImageDecoder.createSource(activity!!.contentResolver, selectedPhotoUri!!)
            bitmap = ImageDecoder.decodeBitmap(source)
        } else{
            // all android versions below
            bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, selectedPhotoUri)
        }
        selectPhotoImageView.setImageBitmap(bitmap)
        selectPhotoButton.alpha = 0f
    }

    override fun enableSelectPhotoButton() {
        selectPhotoButton.isEnabled = true
    }

    override fun disableSelectPhotoButton() {
        selectPhotoButton.isEnabled = false
    }

    private fun loadPhotoFromGallery() {
        val selectPhotoIntent = Intent()
        selectPhotoIntent.type = "image/*"
        selectPhotoIntent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(selectPhotoIntent, "Select Picture"), 1)
    }

    private fun networkUnavailableUploadImageMessage() {
        Toast.makeText(activity, R.string.network_unavailable_upload_image_message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLoginScreen() {
        val navigateToLoginScreenIntent = Intent(activity, UserLoginActivity::class.java)
        activity!!.finish()
        startActivity(navigateToLoginScreenIntent)
    }

    private fun navigateToEditProfileScreen() {
        val navigateToEditProfileScreenIntent = Intent(activity, EditProfileActivity::class.java)
        startActivity(navigateToEditProfileScreenIntent)
    }

    private fun checkUserHasStoragePermissions(context: Context) : Boolean {

        return (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)

    }

    private fun requestUserStoragePermission() {
        ActivityCompat.requestPermissions(activity as Activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CurrentUser.STORAGE_PERMISSIONS_REQUEST_CODE)
    }

}
