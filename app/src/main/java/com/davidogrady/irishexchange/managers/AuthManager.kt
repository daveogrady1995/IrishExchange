package com.davidogrady.irishexchange.managers
import com.google.firebase.auth.FirebaseAuth

class AuthManager(private val mAuth: FirebaseAuth) {

    fun registerUserWithEmailAndPassword(email: String, password: String) =  mAuth.createUserWithEmailAndPassword(email, password)

    fun loginUserWithEmailAndPassword(email: String, password: String) =  mAuth.signInWithEmailAndPassword(email, password)

    fun logOutCurrentUser() {
        mAuth.signOut()
    }

    fun sendPasswordResetToEmail(email: String,
                                 emailSentSuccessHandler: () -> Unit,
                                 emailSentFailureHandler: (errorMessage: String) -> Unit) {
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener {
            emailSentSuccessHandler.invoke()
        }.addOnFailureListener {
            emailSentFailureHandler.invoke(it.message!!)
        }
    }

    fun getUidForCurrentUser() : String? = mAuth.uid

    fun isUserLoggedIn() : Boolean {
        val userLoggedIn = (mAuth.uid != null)
        return userLoggedIn
    }
}