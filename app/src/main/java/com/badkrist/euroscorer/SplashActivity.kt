package com.badkrist.euroscorer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


class SplashActivity : Activity() {

    private lateinit var myHandler : Handler

    private val RC_SIGN_IN = 123

    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // Show main screen after deplay
            myHandler = Handler()
            myHandler.postDelayed({
                goToMainActivity()
            }, 2.seconds.toLongMilliseconds())
        } else {
            showLogin()
        }
    }

    fun showLogin() {
        val allowedCountries = listOf(
            "AL", "AM", "AT" ,"AU", "AZ", "BE", "BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE",
            "ES",  "FI", "FR", "GB", "GE", "GR", "HR", "HU", "IE", "IL", "IS", "IT", "LT", "LV",
            "MD", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SM", "UA" )
        val phoneProvider =  AuthUI.IdpConfig.PhoneBuilder()
            .setWhitelistedCountries(allowedCountries)
            .build()
        val providers = listOf(phoneProvider)
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setIsSmartLockEnabled(!BuildConfig.DEBUG /* credentials */, true /* hints */)
            .setAvailableProviders(providers)
            .setTosAndPrivacyPolicyUrls("https://euroscorer2020.com/privacypolicy.html", "https://euroscorer2020.com/privacypolicy.html")
            .build()
        startActivityForResult(intent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode === RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode === RESULT_OK) {
                goToMainActivity()
            } else {
                // Sign in failed
            }
        }
    }

    private fun goToMainActivity(){
        val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
        startActivity(mainActivityIntent)
        finish()
    }
}