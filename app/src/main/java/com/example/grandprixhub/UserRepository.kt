package com.example.grandprixhub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Inside UserRepository.kt
    fun saveUserPreferences(driverId: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userMap = hashMapOf(
            "favDriver" to driverId
            // Removed favTeam to match your onboarding UI
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}