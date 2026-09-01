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
    var wishlistProducts = mutableStateListOf<Product>()
    
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    private var allProductsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userProductsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var wishlistListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadAllProducts()
        loadUserProducts()
        loadWishlist()
    }

    fun loadAllProducts() {
        isLoading.value = true
        errorMessage.value = null
        
        allProductsListener?.remove()
        // Listen to all products without complex filters initially to ensure visibility
        allProductsListener = db.collection("products")
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    errorMessage.value = "Firestore Error: ${e.message}"
                    android.util.Log.e("ProductViewModel", "Error loading products", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fromCache = snapshot.metadata.isFromCache
                    android.util.Log.d("ProductViewModel", "Products from ${if (fromCache) "Cache" else "Server"}. Total docs: ${snapshot.size()}")
                    
                    val productsList = mutableListOf<Product>()
                    for (doc in snapshot.documents) {
                        try {
                            // Manual mapping for better resilience to missing fields
                            val product = Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "Unnamed Product",
                                price = doc.getDouble("price") ?: 0.0,
                                category = doc.getString("category") ?: "Unknown",
                                description = doc.getString("description") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                ownerId = doc.getString("ownerId") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                isSold = doc.getBoolean("isSold") ?: doc.getBoolean("sold") ?: false
                            )
                            productsList.add(product)
                        } catch (ex: Exception) {
                            android.util.Log.e("ProductViewModel", "Error mapping document ${doc.id}", ex)
                        }
                    }
                    
                    allProducts.clear()
                    // Filter unsold and sort newest first
                    allProducts.addAll(productsList.filter { !it.isSold }.sortedByDescending { it.createdAt })
                    android.util.Log.d("ProductViewModel", "Displaying ${allProducts.size} unsold products")
                }
            }
    }

    fun refreshProducts() {
        // Simple refresh
        loadAllProducts()
        loadUserProducts()
    }
    
    fun forceSync() {
        // Disable cache temporarily or force a get() from server
        isLoading.value = true
        db.collection("products").get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { 
                refreshProducts() 
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Sync failed: ${e.message}"
                isLoading.value = false
            }
    }

    fun loadUserProducts() {
        val userId = auth.currentUser?.uid ?: return
        userProductsListener?.remove()
        userProductsListener = db.collection("products")
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

    fun loadWishlist() {
        val userId = auth.currentUser?.uid ?: return
        wishlistListener?.remove()
        wishlistListener = db.collection("users").document(userId).collection("wishlist")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    wishlistProducts.clear()
                    val products = snapshot.toObjects(Product::class.java)
                    wishlistProducts.addAll(products)
                }
            }
    }

    fun toggleWishlist(product: Product) {
        val userId = auth.currentUser?.uid ?: return
        val productRef = db.collection("users").document(userId).collection("wishlist").document(product.id)
        
        productRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                productRef.delete()
            } else {
                productRef.set(product)
            }
        }
    }

    fun isFavorite(productId: String): Boolean {
        return wishlistProducts.any { it.id == productId }
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

    fun reportProduct(productId: String, reason: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val reportId = db.collection("reports").document().id
        val report = Report(
            id = reportId,
            productId = productId,
            reporterId = userId,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )

        db.collection("reports").document(reportId).set(report)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
                if (!task.isSuccessful) {
                    errorMessage.value = "Failed to submit report: ${task.exception?.message}"
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        allProductsListener?.remove()
        userProductsListener?.remove()
        wishlistListener?.remove()
    }
}
