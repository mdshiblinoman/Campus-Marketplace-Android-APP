package com.example.campusmarketplace.products

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        db.collection("products")
            .whereEqualTo("isSold", false)
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    errorMessage.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allProducts.clear()
                    allProducts.addAll(snapshot.toObjects(Product::class.java))
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
                    userProducts.addAll(snapshot.toObjects(Product::class.java))
                }
            }
    }

    fun addProduct(name: String, price: Double, category: String, description: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("products").document()
        val product = Product(
            id = docRef.id,
            name = name,
            price = price,
            category = category,
            description = description,
            ownerId = userId,
            isSold = false,
        )
        
        docRef.set(product)
            .addOnFailureListener { errorMessage.value = it.message }
    }

    fun updateProduct(product: Product) {
        db.collection("products").document(product.id).set(product)
            .addOnFailureListener { errorMessage.value = it.message }
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
