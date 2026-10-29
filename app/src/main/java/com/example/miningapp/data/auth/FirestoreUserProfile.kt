package com.example.miningapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

fun loadCurrentUserProfile(
    onSuccess: (String?, String?) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onFailure(IllegalStateException("No authenticated Firebase user is available."))
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .get()
        .addOnSuccessListener { snapshot ->
            onSuccess(
                snapshot.getString("fullName"),
                snapshot.getString("email")
            )
        }
        .addOnFailureListener(onFailure)
}

fun saveCurrentUserToFirestore(
    fullName: String? = null,
    email: String? = null,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onFailure(IllegalStateException("No authenticated Firebase user is available."))
        return
    }

    val phone = user.phoneNumber
    if (phone.isNullOrBlank()) {
        onFailure(IllegalStateException("The authenticated Firebase user has no phone number."))
        return
    }

    val userDocument = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)

    userDocument.get()
        .addOnSuccessListener { snapshot ->
            val userData = hashMapOf<String, Any>(
                "uid" to user.uid,
                "phone" to phone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snapshot.exists()) {
                userData["createdAt"] = FieldValue.serverTimestamp()
            }
            fullName?.takeIf(String::isNotBlank)?.let { userData["fullName"] = it.trim() }
            email?.takeIf(String::isNotBlank)?.let { userData["email"] = it.trim() }

            userDocument.set(userData, SetOptions.merge())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener(onFailure)
        }
        .addOnFailureListener(onFailure)
}
