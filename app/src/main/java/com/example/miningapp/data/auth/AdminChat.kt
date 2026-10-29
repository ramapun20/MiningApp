package com.example.miningapp.data.auth

import com.example.miningapp.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

private const val ADMIN_CHAT_COLLECTION = "supportChats"

fun listenToAdminChat(
    userId: String,
    onMessages: (List<ChatMessage>) -> Unit,
    onFailure: (Exception) -> Unit
): ListenerRegistration {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    require(currentUserId == userId) { "Only the signed-in account can open its support chat." }

    return FirebaseFirestore.getInstance()
        .collection(ADMIN_CHAT_COLLECTION)
        .document(userId)
        .collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                val senderId = document.getString("senderUid") ?: return@mapNotNull null
                val text = document.getString("text") ?: return@mapNotNull null
                val timestamp = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                ChatMessage(
                    id = document.id.hashCode().toLong(),
                    sender = document.getString("senderName") ?: "Admin",
                    text = text,
                    timestamp = timestamp,
                    isOutgoing = senderId == userId,
                    documentId = document.id
                )
            }
            onMessages(messages)
        }
}

fun sendAdminChatMessage(
    userId: String,
    senderName: String,
    text: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null || currentUser.uid != userId) {
        onFailure(IllegalStateException("Sign in to your account to message the admin."))
        return
    }

    val message = hashMapOf<String, Any>(
        "senderUid" to userId,
        "senderName" to senderName.ifBlank { currentUser.phoneNumber ?: "Account holder" },
        "text" to text.trim(),
        "createdAt" to FieldValue.serverTimestamp()
    )

    FirebaseFirestore.getInstance()
        .collection(ADMIN_CHAT_COLLECTION)
        .document(userId)
        .collection("messages")
        .add(message)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
}

fun deleteAdminChatMessage(
    userId: String,
    messageId: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId != userId) {
        onFailure(IllegalStateException("Sign in to the account that owns this chat to delete a message."))
        return
    }

    FirebaseFirestore.getInstance()
        .collection(ADMIN_CHAT_COLLECTION)
        .document(userId)
        .collection("messages")
        .document(messageId)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
}
