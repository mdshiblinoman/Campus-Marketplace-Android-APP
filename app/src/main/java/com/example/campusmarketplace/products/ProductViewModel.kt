package com.example.campusmarketplace.products

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    var userProducts = mutableStateListOf<Product>()
    var allProducts = mutableStateListOf<Product>()
    var wishlistProducts = mutableStateListOf<Product>()
    var wishlistProductIds = mutableStateListOf<String>()
    var sellerNames = mutableStateMapOf<String, String>()
    var sellerDepartments = mutableStateMapOf<String, String>()
    
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)
    var imageUploadStatus = mutableStateOf<String?>(null)

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
                    
                    val productsList = snapshot.documents.mapNotNull { doc -> mapProduct(doc) }
                    
                    allProducts.clear()
                    // Filter unsold and sort newest first
                    allProducts.addAll(productsList.filter { !it.isSold }.sortedByDescending { it.createdAt })
                    loadSellerNames(productsList.map { it.ownerId })
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
                    loadSellerNames(products.map { it.ownerId })
                }
            }
    }

    fun loadWishlist() {
        val userId = auth.currentUser?.uid ?: return
        wishlistListener?.remove()
        wishlistListener = db.collection("wishlist")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val wishlistItems = snapshot.documents
                        .mapNotNull { document ->
                            val productId = document.getString("productId") ?: return@mapNotNull null
                            productId to (document.getLong("createdAt") ?: 0L)
                        }
                        .sortedByDescending { it.second }
                    val productIds = wishlistItems.map { it.first }
                    val productOrder = productIds.withIndex().associate { it.value to it.index }
                    val loadedProducts = mutableMapOf<String, Product>()

                    wishlistProductIds.clear()
                    wishlistProductIds.addAll(productIds)
                    wishlistProducts.clear()

                    if (productIds.isEmpty()) {
                        return@addSnapshotListener
                    }

                    productIds.forEach { productId ->
                        db.collection("products").document(productId).get()
                            .addOnSuccessListener { productDocument ->
                                mapProduct(productDocument)?.let { product ->
                                    loadedProducts[productId] = product
                                    wishlistProducts.clear()
                                    wishlistProducts.addAll(
                                        loadedProducts.values.sortedBy { productOrder[it.id] ?: Int.MAX_VALUE }
                                    )
                                    loadSellerNames(wishlistProducts.map { it.ownerId })
                                }
                            }
                            .addOnFailureListener {
                                errorMessage.value = "Failed to load wishlist product: ${it.message}"
                            }
                    }
                }
            }
    }

    fun toggleWishlist(product: Product) {
        val userId = auth.currentUser?.uid ?: return
        if (product.id.isBlank()) return

        val wishlistRef = db.collection("wishlist").document("${userId}_${product.id}")
        
        wishlistRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                wishlistRef.delete()
                    .addOnFailureListener { errorMessage.value = "Failed to remove favorite: ${it.message}" }
            } else {
                wishlistRef.set(
                    mapOf(
                        "userId" to userId,
                        "productId" to product.id,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                    .addOnFailureListener { errorMessage.value = "Failed to save favorite: ${it.message}" }
            }
        }.addOnFailureListener {
            errorMessage.value = "Failed to update wishlist: ${it.message}"
        }
    }

    fun isFavorite(productId: String): Boolean {
        return wishlistProductIds.contains(productId)
    }

    private fun mapProduct(doc: com.google.firebase.firestore.DocumentSnapshot): Product? {
        if (!doc.exists()) return null

        return try {
            Product(
                id = doc.id,
                name = doc.getString("name") ?: "Unnamed Product",
                price = doc.getDouble("price") ?: 0.0,
                category = doc.getString("category") ?: "Unknown",
                description = doc.getString("description") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                imageUrls = (doc.get("imageUrls") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty(),
                condition = doc.getString("condition") ?: "",
                location = doc.getString("location") ?: "",
                contactPreference = doc.getString("contactPreference") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                isSold = doc.getBoolean("isSold") ?: doc.getBoolean("sold") ?: false
            )
        } catch (ex: Exception) {
            android.util.Log.e("ProductViewModel", "Error mapping document ${doc.id}", ex)
            null
        }
    }

    fun sellerNameFor(ownerId: String): String {
        if (ownerId.isBlank()) return "Unknown seller"
        return sellerNames[ownerId] ?: "Loading seller..."
    }

    fun sellerDepartmentFor(ownerId: String): String {
        if (ownerId.isBlank()) return ""
        return sellerDepartments[ownerId].orEmpty()
    }

    private fun loadSellerNames(ownerIds: List<String>) {
        ownerIds
            .filter { it.isNotBlank() && !sellerNames.containsKey(it) }
            .distinct()
            .forEach { ownerId ->
                realtimeDb.child("users").child(ownerId).get()
                    .addOnSuccessListener { snapshot ->
                        sellerNames[ownerId] = snapshot.child("fullName").value?.toString()?.takeIf { it.isNotBlank() }
                            ?: "User ${ownerId.take(5)}"
                        sellerDepartments[ownerId] = snapshot.child("department").value?.toString().orEmpty()
                    }
                    .addOnFailureListener {
                        db.collection("users").document(ownerId).get()
                            .addOnSuccessListener { document ->
                                sellerNames[ownerId] = document.getString("fullName")?.takeIf { it.isNotBlank() }
                                    ?: "User ${ownerId.take(5)}"
                                sellerDepartments[ownerId] = document.getString("department").orEmpty()
                            }
                            .addOnFailureListener {
                                sellerNames[ownerId] = "User ${ownerId.take(5)}"
                                sellerDepartments[ownerId] = ""
                            }
                    }
            }
    }

    fun clearProductMessages() {
        errorMessage.value = null
        imageUploadStatus.value = null
    }

    fun addProduct(
        name: String,
        price: Double,
        category: String,
        description: String,
        condition: String,
        location: String,
        contactPreference: String,
        imageUris: List<Uri>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true
        errorMessage.value = null
        imageUploadStatus.value = null
        val docRef = db.collection("products").document()

        uploadProductImages(
            productId = docRef.id,
            imageUris = imageUris,
            onSuccess = { imageUrls ->
                saveProduct(
                    id = docRef.id,
                    name = name,
                    price = price,
                    category = category,
                    description = description,
                    condition = condition,
                    location = location,
                    contactPreference = contactPreference,
                    userId = userId,
                    imageUrls = imageUrls,
                    onComplete = onComplete
                )
            },
            onFailure = {
                isLoading.value = false
                imageUploadStatus.value = null
                errorMessage.value = it
                onComplete(false)
            }
        )
    }

    private fun saveProduct(
        id: String,
        name: String,
        price: Double,
        category: String,
        description: String,
        condition: String,
        location: String,
        contactPreference: String,
        userId: String,
        imageUrls: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        val now = System.currentTimeMillis()
        // Use a map to ensure field names are exactly what we expect
        val productMap = hashMapOf(
            "id" to id,
            "name" to name.trim(),
            "price" to price,
            "category" to category,
            "description" to description.trim(),
            "condition" to condition,
            "location" to location.trim(),
            "contactPreference" to contactPreference,
            "ownerId" to userId,
            "imageUrl" to imageUrls.firstOrNull().orEmpty(),
            "imageUrls" to imageUrls,
            "imageCount" to imageUrls.size,
            "isSold" to false,
            "createdAt" to now,
            "updatedAt" to now
        )
        
        db.collection("products").document(id).set(productMap)
            .addOnCompleteListener { 
                isLoading.value = false
                imageUploadStatus.value = null
                if (!it.isSuccessful) {
                    errorMessage.value = "Failed to save product: ${it.exception?.message}"
                }
                onComplete(it.isSuccessful)
            }
    }

    fun updateProduct(
        product: Product,
        newImageUris: List<Uri>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        isLoading.value = true
        errorMessage.value = null
        imageUploadStatus.value = null

        uploadProductImages(
            productId = product.id,
            imageUris = newImageUris,
            onSuccess = { uploadedUrls ->
                val existingUrls = product.imageUrls.ifEmpty {
                    listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() })
                }
                val finalUrls = uploadedUrls.ifEmpty { existingUrls }
                val updatedProduct = product.copy(
                    name = product.name.trim(),
                    description = product.description.trim(),
                    location = product.location.trim(),
                    imageUrl = finalUrls.firstOrNull().orEmpty(),
                    imageUrls = finalUrls
                )

                db.collection("products").document(product.id).set(
                    mapOf(
                        "id" to updatedProduct.id,
                        "name" to updatedProduct.name,
                        "price" to updatedProduct.price,
                        "category" to updatedProduct.category,
                        "description" to updatedProduct.description,
                        "condition" to updatedProduct.condition,
                        "location" to updatedProduct.location,
                        "contactPreference" to updatedProduct.contactPreference,
                        "ownerId" to updatedProduct.ownerId,
                        "imageUrl" to updatedProduct.imageUrl,
                        "imageUrls" to updatedProduct.imageUrls,
                        "imageCount" to updatedProduct.imageUrls.size,
                        "isSold" to updatedProduct.isSold,
                        "createdAt" to updatedProduct.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                    .addOnCompleteListener {
                        isLoading.value = false
                        imageUploadStatus.value = null
                        if (!it.isSuccessful) {
                            errorMessage.value = "Failed to update product: ${it.exception?.message}"
                        }
                        onComplete(it.isSuccessful)
                    }
            },
            onFailure = {
                isLoading.value = false
                imageUploadStatus.value = null
                errorMessage.value = it
                onComplete(false)
            }
        )
    }

    private fun uploadProductImages(
        productId: String,
        imageUris: List<Uri>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (imageUris.isEmpty()) {
            imageUploadStatus.value = null
            onSuccess(emptyList())
            return
        }

        val uploadedUrls = mutableListOf<String>()

        fun uploadAt(index: Int) {
            if (index >= imageUris.size) {
                imageUploadStatus.value = "Saving image URLs..."
                onSuccess(uploadedUrls)
                return
            }

            imageUploadStatus.value = "Uploading image ${index + 1} of ${imageUris.size}..."
            val storageRef = storage.reference
                .child("product_images/$productId/image_${index}_${System.currentTimeMillis()}.jpg")

            storageRef.putFile(imageUris[index])
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        uploadedUrls.add(task.result.toString())
                        uploadAt(index + 1)
                    } else {
                        onFailure(task.exception?.message ?: "Image upload failed")
                    }
                }
        }

        uploadAt(0)
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
