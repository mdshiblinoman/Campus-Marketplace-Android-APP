package com.example.campusmarketplace.products

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.notifications.NotificationRepository
import com.example.campusmarketplace.notifications.NotificationType
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
    var sellerAverageRatings = mutableStateMapOf<String, Double>()
    var sellerReviewCounts = mutableStateMapOf<String, Int>()
    
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
                    // Only approved, available listings are published in normal browsing.
                    allProducts.addAll(productsList.filter { isPublished(it) }.sortedByDescending { it.createdAt })
                    loadSellerNames(productsList.map { it.ownerId })
                    loadSellerRatings(productsList.map { it.ownerId })
                    android.util.Log.d("ProductViewModel", "Displaying ${allProducts.size} available products")
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
                    val products = snapshot.documents.mapNotNull { doc -> mapProduct(doc) }
                    userProducts.addAll(products.sortedByDescending { it.createdAt })
                    loadSellerNames(products.map { it.ownerId })
                    loadSellerRatings(products.map { it.ownerId })
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
                                    loadSellerRatings(wishlistProducts.map { it.ownerId })
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
            val storedIsSold = doc.getBoolean("isSold") ?: doc.getBoolean("sold") ?: false
            val availabilityStatus = normalizeAvailabilityStatus(doc.getString("availabilityStatus"), storedIsSold)
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
                condition = normalizeCondition(doc.getString("condition")),
                location = doc.getString("location") ?: "",
                contactPreference = doc.getString("contactPreference") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                approvalStatus = doc.getString("approvalStatus") ?: ProductApprovalStatus.Approved,
                availabilityStatus = availabilityStatus,
                reviewedAt = doc.getLong("reviewedAt") ?: 0L,
                reviewedBy = doc.getString("reviewedBy") ?: "",
                publishedAt = doc.getLong("publishedAt") ?: 0L,
                rejectionReason = doc.getString("rejectionReason") ?: "",
                isSold = availabilityStatus == ProductAvailabilityStatus.Sold
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

    fun sellerRatingSummary(ownerId: String): String {
        val average = sellerAverageRatings[ownerId] ?: return "No reviews yet"
        val count = sellerReviewCounts[ownerId] ?: 0
        if (count == 0) return "No reviews yet"
        return String.format(java.util.Locale.getDefault(), "%.1f/5 (%d)", average, count)
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

    private fun loadSellerRatings(ownerIds: List<String>) {
        ownerIds
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ownerId ->
                db.collection("seller_reviews")
                    .whereEqualTo("sellerId", ownerId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val ratings = snapshot.documents.mapNotNull { document ->
                            document.getLong("rating")?.toInt()?.takeIf { it in 1..5 }
                        }
                        sellerReviewCounts[ownerId] = ratings.size
                        sellerAverageRatings[ownerId] = if (ratings.isEmpty()) {
                            0.0
                        } else {
                            ratings.average()
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
            "condition" to normalizeCondition(condition),
            "location" to location.trim(),
            "contactPreference" to contactPreference,
            "ownerId" to userId,
            "imageUrl" to imageUrls.firstOrNull().orEmpty(),
            "imageUrls" to imageUrls,
            "imageCount" to imageUrls.size,
            "isSold" to false,
            "availabilityStatus" to ProductAvailabilityStatus.Available,
            "approvalStatus" to ProductApprovalStatus.Pending,
            "reviewedAt" to 0L,
            "reviewedBy" to "",
            "publishedAt" to 0L,
            "rejectionReason" to "",
            "createdAt" to now,
            "updatedAt" to now
        )
        
        db.collection("products").document(id).set(productMap)
            .addOnCompleteListener { 
                isLoading.value = false
                imageUploadStatus.value = null
                if (!it.isSuccessful) {
                    errorMessage.value = "Failed to save product: ${it.exception?.message}"
                } else {
                    notifyAdminsOfPendingProduct(
                        productId = id,
                        productTitle = name.trim(),
                        sellerId = userId
                    )
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
                val retainedUrls = product.imageUrls.ifEmpty {
                    listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() })
                }
                val finalUrls = (retainedUrls + uploadedUrls).distinct().take(6)
                val updatedProduct = product.copy(
                    name = product.name.trim(),
                    description = product.description.trim(),
                    condition = normalizeCondition(product.condition),
                    location = product.location.trim(),
                    imageUrl = finalUrls.firstOrNull().orEmpty(),
                    imageUrls = finalUrls,
                    approvalStatus = ProductApprovalStatus.Pending,
                    reviewedAt = 0L,
                    reviewedBy = "",
                    publishedAt = 0L,
                    rejectionReason = ""
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
                        "availabilityStatus" to normalizeAvailabilityStatus(
                            updatedProduct.availabilityStatus,
                            updatedProduct.isSold
                        ),
                        "approvalStatus" to updatedProduct.approvalStatus,
                        "reviewedAt" to updatedProduct.reviewedAt,
                        "reviewedBy" to updatedProduct.reviewedBy,
                        "publishedAt" to updatedProduct.publishedAt,
                        "rejectionReason" to updatedProduct.rejectionReason,
                        "createdAt" to updatedProduct.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                    .addOnCompleteListener {
                        isLoading.value = false
                        imageUploadStatus.value = null
                        if (!it.isSuccessful) {
                            errorMessage.value = "Failed to update product: ${it.exception?.message}"
                        } else {
                            notifyAdminsOfPendingProduct(
                                productId = updatedProduct.id,
                                productTitle = updatedProduct.name,
                                sellerId = updatedProduct.ownerId
                            )
                            notifyWishlistUsers(
                                product = updatedProduct,
                                exceptUserId = updatedProduct.ownerId,
                                title = "Product Status Changed",
                                message = "\"${updatedProduct.name}\" was updated by the seller.",
                                type = NotificationType.ProductStatus
                            )
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

    private fun isPublished(product: Product): Boolean {
        return !product.isSold &&
            normalizeAvailabilityStatus(product.availabilityStatus, product.isSold) == ProductAvailabilityStatus.Available &&
            product.approvalStatus == ProductApprovalStatus.Approved
    }

    private fun normalizeCondition(condition: String?): String {
        return condition?.trim()?.takeIf { it.isNotBlank() } ?: ProductCondition.Used
    }

    private fun normalizeAvailabilityStatus(status: String?, isSold: Boolean): String {
        if (isSold) return ProductAvailabilityStatus.Sold
        return when (status?.trim()?.lowercase()) {
            ProductAvailabilityStatus.Reserved -> ProductAvailabilityStatus.Reserved
            ProductAvailabilityStatus.Sold -> ProductAvailabilityStatus.Sold
            ProductAvailabilityStatus.Removed -> ProductAvailabilityStatus.Removed
            else -> ProductAvailabilityStatus.Available
        }
    }

    fun deleteProduct(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (productId.isBlank()) return

        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId").orEmpty()
                if (ownerId != userId) {
                    errorMessage.value = "You can only delete your own listings."
                    return@addOnSuccessListener
                }

                productRef.delete()
                    .addOnFailureListener { errorMessage.value = it.message }
            }
            .addOnFailureListener { errorMessage.value = it.message }
    }

    fun markAsSold(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (productId.isBlank()) return

        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val product = mapProduct(document)
                if (product == null) {
                    errorMessage.value = "Listing not found."
                    return@addOnSuccessListener
                }
                if (product.ownerId != userId) {
                    errorMessage.value = "You can only mark your own listings as sold."
                    return@addOnSuccessListener
                }
                val currentStatus = normalizeAvailabilityStatus(product.availabilityStatus, product.isSold)
                if (currentStatus == ProductAvailabilityStatus.Sold) {
                    errorMessage.value = "\"${product.name}\" is already marked as sold."
                    return@addOnSuccessListener
                }
                if (currentStatus == ProductAvailabilityStatus.Removed) {
                    errorMessage.value = "\"${product.name}\" has been removed."
                    return@addOnSuccessListener
                }

                productRef.update(
                    mapOf(
                        "isSold" to true,
                        "availabilityStatus" to ProductAvailabilityStatus.Sold,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                    .addOnSuccessListener {
                        NotificationRepository.notifyUser(
                            recipientId = product.ownerId,
                            title = "Product Sold",
                            message = "Your listing \"${product.name}\" was marked as sold.",
                            type = NotificationType.ProductSold,
                            relatedId = product.id,
                            relatedTitle = product.name,
                            createdBy = userId
                        )
                        notifyWishlistUsers(
                            product = product,
                            exceptUserId = product.ownerId,
                            title = "Product Sold",
                            message = "\"${product.name}\" has been marked as sold.",
                            type = NotificationType.ProductSold
                        )
                    }
                    .addOnFailureListener { errorMessage.value = it.message }
            }
            .addOnFailureListener { errorMessage.value = it.message }
    }

    fun markAsReserved(productId: String) {
        updateAvailabilityStatus(
            productId = productId,
            targetStatus = ProductAvailabilityStatus.Reserved,
            successTitle = "Product Reserved",
            successMessage = "Your listing was marked as reserved."
        )
    }

    fun markAsAvailable(productId: String) {
        updateAvailabilityStatus(
            productId = productId,
            targetStatus = ProductAvailabilityStatus.Available,
            successTitle = "Product Available",
            successMessage = "Your listing is available again."
        )
    }

    private fun updateAvailabilityStatus(
        productId: String,
        targetStatus: String,
        successTitle: String,
        successMessage: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        if (productId.isBlank()) return

        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val product = mapProduct(document)
                if (product == null) {
                    errorMessage.value = "Listing not found."
                    return@addOnSuccessListener
                }
                if (product.ownerId != userId) {
                    errorMessage.value = "You can only update your own listings."
                    return@addOnSuccessListener
                }

                val currentStatus = normalizeAvailabilityStatus(product.availabilityStatus, product.isSold)
                if (currentStatus == ProductAvailabilityStatus.Sold) {
                    errorMessage.value = "\"${product.name}\" is already sold."
                    return@addOnSuccessListener
                }
                if (currentStatus == ProductAvailabilityStatus.Removed) {
                    errorMessage.value = "\"${product.name}\" has been removed."
                    return@addOnSuccessListener
                }

                productRef.update(
                    mapOf(
                        "isSold" to (targetStatus == ProductAvailabilityStatus.Sold),
                        "availabilityStatus" to targetStatus,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                    .addOnSuccessListener {
                        NotificationRepository.notifyUser(
                            recipientId = product.ownerId,
                            title = successTitle,
                            message = successMessage.replace("your listing", "\"${product.name}\""),
                            type = NotificationType.ProductStatus,
                            relatedId = product.id,
                            relatedTitle = product.name,
                            createdBy = userId
                        )
                        notifyWishlistUsers(
                            product = product,
                            exceptUserId = product.ownerId,
                            title = "Product Status Changed",
                            message = "\"${product.name}\" is now ${targetStatus.replaceFirstChar { it.uppercase() }}.",
                            type = NotificationType.ProductStatus
                        )
                    }
                    .addOnFailureListener { errorMessage.value = it.message }
            }
            .addOnFailureListener { errorMessage.value = it.message }
    }

    private fun notifyWishlistUsers(
        product: Product,
        exceptUserId: String,
        title: String,
        message: String,
        type: String
    ) {
        if (product.id.isBlank()) return
        db.collection("wishlist")
            .whereEqualTo("productId", product.id)
            .get()
            .addOnSuccessListener { snapshot ->
                val recipientIds = snapshot.documents
                    .mapNotNull { it.getString("userId") }
                    .filter { it != exceptUserId }

                NotificationRepository.notifyUsers(
                    recipientIds = recipientIds,
                    title = title,
                    message = message,
                    type = type,
                    relatedId = product.id,
                    relatedTitle = product.name,
                    createdBy = auth.currentUser?.uid.orEmpty()
                )
            }
    }

    fun reportProduct(productId: String, reason: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (productId.isBlank() || reason.isBlank()) {
            errorMessage.value = "Choose a report reason."
            onComplete(false)
            return
        }

        db.collection("products").document(productId).get()
            .addOnSuccessListener { productDocument ->
                val product = mapProduct(productDocument)
                if (product == null) {
                    errorMessage.value = "Listing not found."
                    onComplete(false)
                    return@addOnSuccessListener
                }
                if (product.ownerId == userId) {
                    errorMessage.value = "You cannot report your own listing."
                    onComplete(false)
                    return@addOnSuccessListener
                }

                saveReport(
                    product = product,
                    reporterId = userId,
                    reason = reason,
                    onComplete = onComplete
                )
            }
            .addOnFailureListener {
                errorMessage.value = "Failed to load listing: ${it.message}"
                onComplete(false)
            }
    }

    fun submitSellerReview(
        product: Product,
        rating: Int,
        comment: String,
        onComplete: (Boolean) -> Unit
    ) {
        val reviewerId = auth.currentUser?.uid ?: return
        when {
            product.id.isBlank() || product.ownerId.isBlank() -> {
                errorMessage.value = "Listing or seller information is missing."
                onComplete(false)
                return
            }
            product.ownerId == reviewerId -> {
                errorMessage.value = "You cannot review yourself."
                onComplete(false)
                return
            }
            !product.isSold -> {
                errorMessage.value = "You can review the seller after the listing is marked sold."
                onComplete(false)
                return
            }
            rating !in 1..5 -> {
                errorMessage.value = "Choose a rating from 1 to 5 stars."
                onComplete(false)
                return
            }
            comment.trim().length < 5 -> {
                errorMessage.value = "Write a short review comment."
                onComplete(false)
                return
            }
        }

        val reviewId = "${product.ownerId}_${reviewerId}_${product.id}"
        val now = System.currentTimeMillis()
        val review = SellerReview(
            id = reviewId,
            sellerId = product.ownerId,
            reviewerId = reviewerId,
            productId = product.id,
            productTitle = product.name,
            rating = rating,
            comment = comment.trim(),
            createdAt = now,
            updatedAt = now
        )

        db.collection("seller_reviews").document(reviewId).set(review)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loadSellerRatings(listOf(product.ownerId))
                    NotificationRepository.notifyUser(
                        recipientId = product.ownerId,
                        title = "New Seller Review",
                        message = "You received $rating/5 stars for \"${product.name}\".",
                        type = NotificationType.ProductStatus,
                        relatedId = product.id,
                        relatedTitle = product.name,
                        createdBy = reviewerId
                    )
                } else {
                    errorMessage.value = "Failed to submit review: ${task.exception?.message}"
                }
                onComplete(task.isSuccessful)
            }
    }

    private fun saveReport(
        product: Product,
        reporterId: String,
        reason: String,
        onComplete: (Boolean) -> Unit
    ) {
        val reportId = db.collection("reports").document().id
        val report = Report(
            id = reportId,
            productId = product.id,
            productTitle = product.name,
            sellerId = product.ownerId,
            reporterId = reporterId,
            reason = reason.trim(),
            timestamp = System.currentTimeMillis()
        )

        db.collection("reports").document(reportId).set(report)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
                if (!task.isSuccessful) {
                    errorMessage.value = "Failed to submit report: ${task.exception?.message}"
                } else {
                    notifyAdminsOfReport(report)
                }
            }
    }

    private fun notifyAdminsOfReport(report: Report) {
        db.collection("users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { snapshot ->
                NotificationRepository.notifyUsers(
                    recipientIds = snapshot.documents.map { it.id },
                    title = "Important Admin Notification",
                    message = "New report for \"${report.productTitle}\": ${report.reason}",
                    type = NotificationType.Admin,
                    relatedId = report.id,
                    relatedTitle = report.productTitle,
                    createdBy = report.reporterId
                )
            }
            .addOnFailureListener {
                errorMessage.value = "Report submitted, but admins could not be notified: ${it.message}"
            }
    }

    private fun notifyAdminsOfPendingProduct(
        productId: String,
        productTitle: String,
        sellerId: String
    ) {
        db.collection("users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { snapshot ->
                NotificationRepository.notifyUsers(
                    recipientIds = snapshot.documents.map { it.id },
                    title = "Product Pending Approval",
                    message = "\"$productTitle\" is waiting for admin review.",
                    type = NotificationType.Admin,
                    relatedId = productId,
                    relatedTitle = productTitle,
                    createdBy = sellerId
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        allProductsListener?.remove()
        userProductsListener?.remove()
        wishlistListener?.remove()
    }
}
