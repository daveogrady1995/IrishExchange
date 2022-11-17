package com.davidogrady.irishexchange.resetpassword

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davidogrady.irishexchange.IrishExchangeApplication
import com.davidogrady.irishexchange.R
import com.davidogrady.irishexchange.managers.AuthManager
import com.davidogrady.irishexchange.managers.DatabaseManager
import com.davidogrady.irishexchange.util.NetworkAvailability


class ResetPasswordActivity : AppCompatActivity(), ResetPasswordContract.View {

    override lateinit var presenter: ResetPasswordContract.Presenter
    private lateinit var resetPasswordEmail: TextView
    private lateinit var resetPasswordButton: Button

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
        setContentView(R.layout.activity_reset_password)

        supportActionBar?.hide()

        val resetPasswordPresenter = ResetPasswordPresenter(
            this,
            AuthManager((applicationContext as IrishExchangeApplication).mAuth),
            DatabaseManager((applicationContext as IrishExchangeApplication).mDatabase)
        )

        resetPasswordEmail = findViewById(R.id.email_edittext_reset_password)
        resetPasswordButton = findViewById(R.id.reset_button_reset_password)

        resetPasswordButton.setOnClickListener {
            if (NetworkAvailability().isInternetAvailable(this)) {
                val email = resetPasswordEmail.text
                if (!email.isNullOrBlank())
                    presenter.sendPasswordRequestToEmail(email.toString())
            }
        }
    }

    override fun showPasswordRequestEmailSentSuccessToast() {
        Toast.makeText(this, R.string.password_request_success_message, Toast.LENGTH_SHORT).show()
    }

    override fun showPasswordRequestEmailFailureToast(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

}
