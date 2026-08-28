package com.example.campusmarketplace.products

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    var userProducts = mutableStateListOf<Product>()
    var allProducts = mutableStateListOf<Product>()
    
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    init {
        loadAllProducts()
        loadUserProducts()
    }

    fun loadAllProducts() {
        isLoading.value = true
        errorMessage.value = null
        db.collection("products")
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    errorMessage.value = "Firestore Error: ${e.message}"
                    android.util.Log.e("ProductViewModel", "Error loading products", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allProducts.clear()
                    val products = snapshot.toObjects(Product::class.java)
                    // Filter unsold products in memory and sort by createdAt descending
                    val filteredAndSorted = products
                        .filter { !it.isSold }
                        .sortedByDescending { it.createdAt }
                    allProducts.addAll(filteredAndSorted)
                    android.util.Log.d("ProductViewModel", "Loaded ${allProducts.size} unsold products")
                }
            }
    }

    fun loadUserProducts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("products")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    userProducts.clear()
                    val products = snapshot.toObjects(Product::class.java)
                    userProducts.addAll(products.sortedByDescending { it.createdAt })
                }
            }
    }

    fun addProduct(name: String, price: Double, category: String, description: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true
        val docRef = db.collection("products").document()
        
        if (imageUri != null) {
            val storageRef = storage.reference.child("product_images/${docRef.id}.jpg")
            storageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        saveProduct(docRef.id, name, price, category, description, userId, downloadUri.toString())
                    } else {
                        isLoading.value = false
                        errorMessage.value = task.exception?.message ?: "Image upload failed"
                    }
                }
        } else {
            saveProduct(docRef.id, name, price, category, description, userId, "")
        }
    }

    private fun saveProduct(id: String, name: String, price: Double, category: String, description: String, userId: String, imageUrl: String) {
        // Use a map to ensure field names are exactly what we expect
        val productMap = hashMapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "category" to category,
            "description" to description,
            "ownerId" to userId,
            "imageUrl" to imageUrl,
            "isSold" to false,
            "createdAt" to System.currentTimeMillis()
        )
        
        db.collection("products").document(id).set(productMap)
            .addOnCompleteListener { 
                isLoading.value = false
                if (!it.isSuccessful) {
                    errorMessage.value = "Failed to save product: ${it.exception?.message}"
                }
            }
    }

    fun updateProduct(product: Product, newImageUri: Uri?) {
        isLoading.value = true
        if (newImageUri != null) {
            val storageRef = storage.reference.child("product_images/${product.id}.jpg")
            storageRef.putFile(newImageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        db.collection("products").document(product.id).set(product.copy(imageUrl = downloadUri.toString()))
                            .addOnCompleteListener { isLoading.value = false }
                    } else {
                        isLoading.value = false
                        errorMessage.value = task.exception?.message ?: "Image upload failed"
                    }
                }
        } else {
            db.collection("products").document(product.id).set(product)
                .addOnCompleteListener { isLoading.value = false }
        }
    }

    fun deleteProduct(productId: String) {
        db.collection("products").document(productId).delete()
            .addOnFailureListener { errorMessage.value = it.message }
    }

    fun markAsSold(productId: String) {
        db.collection("products").document(productId).update("isSold", true)
            .addOnFailureListener { errorMessage.value = it.message }
    }
}
