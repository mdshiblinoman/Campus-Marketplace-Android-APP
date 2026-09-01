package com.example.campusmarketplace.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

object DatabaseUtils {
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun wipeAllData(onComplete: (Boolean) -> Unit) {
        Log.d("DatabaseUtils", "Starting master database wipe...")
        
        val collections = listOf("users", "products", "chats", "reports", "blacklisted_users")
        var collectionsDeleted = 0
        var totalCollections = collections.size

        // 1. Wipe Firestore Collections
        collections.forEach { collectionName ->
            db.collection(collectionName).get().addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener {
                    collectionsDeleted++
                    Log.d("DatabaseUtils", "Wiped collection: $collectionName")
                    if (collectionsDeleted == totalCollections) {
                        // 2. Wipe Realtime Database
                        rtdb.removeValue().addOnCompleteListener { rtdbTask ->
                            Log.d("DatabaseUtils", "Wiped Realtime Database: ${rtdbTask.isSuccessful}")
                            
                            // 3. Sign out current user
                            auth.signOut()
                            
                            onComplete(true)
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e("DatabaseUtils", "Failed to fetch collection: $collectionName", it)
                collectionsDeleted++
                if (collectionsDeleted == totalCollections) {
                    onComplete(false)
                }
            }
        }
    }
}
