package com.badkrist.euroscorer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


class PhoneNumberActivity : Activity(){
    val TAG: String = "PhoneNumberActivity"
    lateinit var phoneNumberEditText: EditText
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number)

        val user = mAuth.currentUser
        if (user != null) {
            goToNextActivity()
        } else {
            phoneNumberEditText = findViewById(R.id.editText_phoneNumber)
            var phoneNumberButton: Button = findViewById(R.id.button_sendPhoneNumber)
            val phoneNumberRegex =
                "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$".toRegex()
            phoneNumberEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    phoneNumberButton.isEnabled =
                        phoneNumberRegex matches phoneNumberEditText.text.toString()
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }

        val sendPhoneNumberButton: Button = findViewById(R.id.button_sendPhoneNumber)
        sendPhoneNumberButton.setOnClickListener { sendPhoneNumber() }
    }

    private fun sendPhoneNumber(){
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")
                Toast.makeText(this@PhoneNumberActivity, R.string.identified, Toast.LENGTH_SHORT)
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(this@PhoneNumberActivity, R.string.error_identification_bad_format, Toast.LENGTH_LONG)
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Toast.makeText(this@PhoneNumberActivity, R.string.error_identification, Toast.LENGTH_LONG)
                }

                // Show a message and update the UI
                // ...
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")
                setContentView(R.layout.activity_verification_code);
                var sendVerificationCodeButton: Button = this@PhoneNumberActivity.findViewById(R.id.button_sendVerificationCode)
                var verificationCodeEditText: EditText = this@PhoneNumberActivity.findViewById(R.id.editText_verificationCode)

                sendVerificationCodeButton.setOnClickListener{
                    val credential =
                        PhoneAuthProvider.getCredential(verificationId, verificationCodeEditText.text.toString())
                        signInWithPhoneAuthCredential(credential)
                }
                // Save verification ID and resending token so we can use them later
                //storedVerificationId = verificationId
                //resendToken = token

                // ...
            }
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumberEditText.text.toString(), // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks) // OnVerificationStateChangedCallbacks
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    goToNextActivity()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this@PhoneNumberActivity, R.string.error_identification_bad_code, Toast.LENGTH_LONG)
                    }
                }
            }
    }

    private fun goToNextActivity() {
        val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
        startActivity(mainActivityIntent)
        finish()
    }
}