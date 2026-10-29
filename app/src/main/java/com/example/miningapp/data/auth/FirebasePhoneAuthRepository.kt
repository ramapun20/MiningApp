package com.example.miningapp.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class FirebasePhoneAuthRepository(
    private val auth: FirebaseAuth
) {
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun signOut() {
        auth.signOut()
    }

    fun requestCode(
        activity: Activity,
        phoneNumber: String,
        forceResend: Boolean = false,
        onCodeSent: () -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        var automaticallyVerified = false
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                automaticallyVerified = true
                onAutoVerified(credential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                onFailure(error)
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = id
                resendToken = token
                if (!automaticallyVerified) onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply {
                if (forceResend) {
                    resendToken?.let { setForceResendingToken(it) }
                }
            }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(
        code: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val id = verificationId
        if (id == null) {
            onFailure(IllegalStateException("Verification session expired. Request a new code."))
            return
        }

        val credential = PhoneAuthProvider.getCredential(id, code)
        signInWithCredential(credential, onSuccess, onFailure)
    }

    fun signInWithCredential(
        credential: PhoneAuthCredential,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                onSuccess(result.additionalUserInfo?.isNewUser == true)
            }
            .addOnFailureListener { onFailure(it) }
    }
}
