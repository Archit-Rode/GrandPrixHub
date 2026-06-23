package com.example.grandprixhub

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // 🏎️ Make sure to add this import!

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveUserPreferences(driverId: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false) // Handle the null case safely
            return
        }

        val userMap = hashMapOf(
            "favDriver" to driverId
        )

        db.collection("users").document(userId)
            // 🏎️ FIXED: Added SetOptions.merge() to update fields without wiping others
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}