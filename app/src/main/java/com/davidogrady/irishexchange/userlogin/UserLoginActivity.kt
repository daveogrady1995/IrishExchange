package com.davidogrady.irishexchange.userlogin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.activities.MainActivity
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.managers.SharedPreferencesManager
import com.davidogrady.irishexchange.resetpassword.ResetPasswordActivity
import com.davidogrady.irishexchange.userregistration.UserRegistrationActivity
import kotlinx.android.synthetic.main.activity_user_login.*


class UserLoginActivity : AppCompatActivity(), UserLoginContract.View {

    private lateinit var btnLogin: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var txtNewUser: TextView
    private lateinit var txtForgotPassword: TextView
    private lateinit var progressBarHolder: FrameLayout

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    override lateinit var presenter: UserLoginContract.Presenter

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
        setContentView(R.layout.activity_user_login)

        supportActionBar?.hide()

        val userLoginPresenter = UserLoginPresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            SharedPreferencesManager(applicationContext as IrishExchangeApplication, "LoggedInUser"),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase))

        btnLogin = findViewById(R.id.login_button_login)
        etEmail = findViewById(R.id.email_edittext_login)
        etPassword = findViewById(R.id.password_edittext_login)
        txtNewUser = findViewById(R.id.new_user_text_view)
        progressBarHolder = findViewById(R.id.progressBarHolderLogin)
        txtForgotPassword = findViewById(R.id.forgot_password_text_view)

        btnLogin.setOnClickListener {
            if (isUserInputValid()) {
                presenter.loginUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
            } else {
                showMissingFieldsMessage()
            }
        }

        txtNewUser.setOnClickListener {
            navigateToUserRegistration()
            finish()
        }

        forgot_password_text_view.setOnClickListener {
            navigateToResetPasswordScreen()
        }
    }

    override fun showLoginSuccessMessage() {
        Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoginFailureMessage(errorMessage: String) {
        AlertDialog.Builder(this!!)
            .setTitle("Failure")
            .setMessage(errorMessage)
            .create()
            .show()
    }

    override fun navigateToLatestMessagesScreen() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        finish()
        startActivity(mainActivityIntent)
    }

    private fun navigateToResetPasswordScreen() {
        val resetPasswordIntent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(resetPasswordIntent)
    }

    override fun showLoadingIndicator() {
        progressBarHolder.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        progressBarHolder.visibility = View.GONE
    }


    override fun showConnectivityErrorMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun navigateToUserRegistration() {
        val userRegistrationIntent = Intent(this, UserRegistrationActivity::class.java)
        finish()
        startActivity(userRegistrationIntent)
    }

    private fun isUserInputValid() : Boolean {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        return (!email.isNullOrEmpty() && !password.isNullOrEmpty())
    }

    private fun showMissingFieldsMessage() {
        Toast.makeText(this, R.string.missing_fields_message, Toast.LENGTH_SHORT).show()
    }

}