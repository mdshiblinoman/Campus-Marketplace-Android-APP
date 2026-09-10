package com.example.campusmarketplace.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.campusmarketplace.admin.AdminViewModel
import com.example.campusmarketplace.admin.MarketplaceUser
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.chat.Chat
import com.example.campusmarketplace.chat.ChatViewModel
import com.example.campusmarketplace.products.Product
import com.example.campusmarketplace.products.ProductViewModel
import com.example.campusmarketplace.products.Report
import com.example.campusmarketplace.profile.ProfileScreen
import com.example.campusmarketplace.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale

private val productCategories = listOf(
    "Books",
    "Calculators",
    "Laptops",
    "Mobile Phones",
    "Tablets",
    "Accessories",
    "Other Gadgets"
)

private val productConditions = listOf(
    "New",
    "Like New",
    "Used - Good",
    "Used - Fair",
    "Needs Repair"
)

private val contactPreferences = listOf(
    "In-app chat",
    "Phone call",
    "SMS",
    "Email"
)

private enum class ProductSortOption(val label: String) {
    Newest("Newest"),
    PriceLowToHigh("Price: low to high"),
    PriceHighToLow("Price: high to low"),
    Name("Name")
}

private fun primaryImageUrl(product: Product): String {
    return product.imageUrls.firstOrNull().orEmpty().ifEmpty { product.imageUrl }
}

private fun formatProductPrice(price: Double): String {
    return "৳${DecimalFormat("#,##0.##").format(price)}"
}

private fun productStatus(product: Product): String {
    return if (product.isSold) "Sold" else "Available"
}

private fun matchesProductSearch(
    product: Product,
    sellerName: String,
    sellerDepartment: String,
    query: String
): Boolean {
    val keywords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (keywords.isEmpty()) return true

    val searchableText = listOf(
        product.name,
        product.description,
        product.category,
        product.condition,
        product.location,
        product.contactPreference,
        productStatus(product),
        sellerName,
        sellerDepartment
    ).joinToString(" ").lowercase()

    return keywords.all { searchableText.contains(it) }
}

private fun formatListingDate(timestamp: Long): String {
    if (timestamp == 0L) return "Date unavailable"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}

sealed class BottomNavItem(val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Icons.Default.Home, "Home")
    object Wishlist : BottomNavItem(Icons.Default.Favorite, "Wishlist")
    object MyProducts : BottomNavItem(Icons.Default.Inventory, "My Products")
    object Chats : BottomNavItem(Icons.AutoMirrored.Filled.Chat, "Chats")
    object Profile : BottomNavItem(Icons.Default.Person, "Profile")
    object Admin : BottomNavItem(Icons.Default.AdminPanelSettings, "Admin")
}

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val productViewModel: ProductViewModel = viewModel(key = currentUserId)
    val chatViewModel: ChatViewModel = viewModel(key = currentUserId)
    val adminViewModel: AdminViewModel = viewModel(key = "admin_$currentUserId")
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOfNotNull(
        BottomNavItem.Home,
        BottomNavItem.Wishlist,
        BottomNavItem.MyProducts,
        BottomNavItem.Chats,
        BottomNavItem.Profile,
        BottomNavItem.Admin.takeIf { adminViewModel.isAdmin.value }
    )

    LaunchedEffect(adminViewModel.isAdmin.value, selectedItem) {
        if (selectedItem > items.lastIndex) {
            selectedItem = 0
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            when (items[selectedItem]) {
                BottomNavItem.Home -> HomeScreen(productViewModel, chatViewModel, authViewModel)
                BottomNavItem.Wishlist -> WishlistScreen(productViewModel, chatViewModel, authViewModel)
                BottomNavItem.MyProducts -> MyProductsScreen(productViewModel)
                BottomNavItem.Chats -> ContactsScreen(chatViewModel, authViewModel)
                BottomNavItem.Admin -> AdminScreen(adminViewModel)
                BottomNavItem.Profile -> {
                    val profileViewModel: ProfileViewModel = viewModel(key = currentUserId)
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onBack = { selectedItem = 0 }, // Go to Home tab on back
                        onSignOut = { authViewModel.signOut() }
                    )
                }
            }
        }
    }
}


@Composable
fun HomeScreen(
    viewModel: ProductViewModel, 
    chatViewModel: ChatViewModel, 
    authViewModel: AuthViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(ProductSortOption.Newest) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var selectedProductForReport by remember { mutableStateOf<Product?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    val products = viewModel.allProducts
    val context = androidx.compose.ui.platform.LocalContext.current
    val sellerNameSnapshot = viewModel.sellerNames.toMap()
    val sellerDepartmentSnapshot = viewModel.sellerDepartments.toMap()
    val filteredProducts = remember(
        products.toList(),
        sellerNameSnapshot,
        sellerDepartmentSnapshot,
        searchQuery,
        selectedCategory,
        minPrice,
        maxPrice,
        sortOption
    ) {
        val minimumPrice = minPrice.toDoubleOrNull()
        val maximumPrice = maxPrice.toDoubleOrNull()

        products
            .filter { product ->
                val sellerName = sellerNameSnapshot[product.ownerId].orEmpty()
                val sellerDepartment = sellerDepartmentSnapshot[product.ownerId].orEmpty()
                val matchesSearch = matchesProductSearch(
                    product = product,
                    sellerName = sellerName,
                    sellerDepartment = sellerDepartment,
                    query = searchQuery
                )
                val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
                val matchesMinPrice = minimumPrice == null || product.price >= minimumPrice
                val matchesMaxPrice = maximumPrice == null || product.price <= maximumPrice

                matchesSearch && matchesCategory && matchesMinPrice && matchesMaxPrice
            }
            .let { filtered ->
                when (sortOption) {
                    ProductSortOption.Newest -> filtered.sortedByDescending { it.createdAt }
                    ProductSortOption.PriceLowToHigh -> filtered.sortedBy { it.price }
                    ProductSortOption.PriceHighToLow -> filtered.sortedByDescending { it.price }
                    ProductSortOption.Name -> filtered.sortedBy { it.name.lowercase() }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by keyword...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = { viewModel.refreshProducts() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedCategory, maxLines = 1)
                }
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    (listOf("All") + productCategories).forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(sortOption.label, maxLines = 1)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    ProductSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                sortOption = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minPrice,
                onValueChange = { minPrice = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.weight(1f),
                label = { Text("Min price") },
                singleLine = true
            )
            OutlinedTextField(
                value = maxPrice,
                onValueChange = { maxPrice = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.weight(1f),
                label = { Text("Max price") },
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) {
                    "Available Items (${filteredProducts.size})"
                } else {
                    "Search Results (${filteredProducts.size})"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (products.isEmpty() && !viewModel.isLoading.value) {
                TextButton(onClick = { viewModel.forceSync() }) {
                    Text("Deep Sync", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        viewModel.errorMessage.value?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.loadAllProducts() }) {
                        Text("Retry")
                    }
                }
            }
        }
        
        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isBlank()) Icons.Default.Inventory else Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No products found" else "No products match your search",
                        color = Color.Gray
                    )
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Pull fresh products from Firestore."
                        } else {
                            "Try a different title, category, condition, location, or seller."
                        },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = {
                        if (searchQuery.isBlank()) {
                            viewModel.refreshProducts()
                        } else {
                            searchQuery = ""
                        }
                    }) {
                        Text(if (searchQuery.isBlank()) "Tap to refresh" else "Clear search")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        sellerName = viewModel.sellerNameFor(product.ownerId),
                        isFavorite = viewModel.isFavorite(product.id),
                        onToggleFavorite = { viewModel.toggleWishlist(product) },
                        onContactSeller = {
                            if (product.ownerId != currentUserId) {
                                chatViewModel.startOrGetChat(product.ownerId) { chatId ->
                                    authViewModel.currentChatId.value = chatId
                                    authViewModel.currentChatPartnerId.value = product.ownerId
                                    authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                                }
                            } else {
                                Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewDetails = {
                            selectedProductForDetail = product
                        }
                    )
                }
            }
        }
    }

    if (selectedProductForDetail != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ProductDetailDialog(
            product = selectedProductForDetail!!,
            sellerName = viewModel.sellerNameFor(selectedProductForDetail!!.ownerId),
            sellerDepartment = viewModel.sellerDepartmentFor(selectedProductForDetail!!.ownerId),
            isFavorite = viewModel.isFavorite(selectedProductForDetail!!.id),
            onToggleFavorite = { viewModel.toggleWishlist(selectedProductForDetail!!) },
            onReport = { selectedProductForReport = selectedProductForDetail },
            onDismiss = { selectedProductForDetail = null },
            onChat = {
                if (selectedProductForDetail!!.ownerId != currentUserId) {
                    chatViewModel.startOrGetChat(selectedProductForDetail!!.ownerId) { chatId ->
                        authViewModel.currentChatId.value = chatId
                        authViewModel.currentChatPartnerId.value = selectedProductForDetail!!.ownerId
                        authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                    }
                } else {
                    Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                }
                selectedProductForDetail = null
            }
        )
    }

    if (selectedProductForReport != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ReportDialog(
            onDismiss = { selectedProductForReport = null },
            onConfirm = { reason ->
                viewModel.reportProduct(selectedProductForReport!!.id, reason) { success ->
                    if (success) {
                        Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                selectedProductForReport = null
            }
        )
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    sellerName: String,
    sellerDepartment: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit,
    onChat: () -> Unit
) {
    val imageUrls = product.imageUrls.ifEmpty {
        listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = product.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Report, contentDescription = "Report", tint = Color.Gray)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (imageUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = formatProductPrice(product.price), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                ProductDetailRow("Category", product.category)
                ProductDetailRow("Condition", product.condition.ifBlank { "Not specified" })
                ProductDetailRow("Posted", formatListingDate(product.createdAt))
                ProductDetailRow("Status", productStatus(product), if (product.isSold) Color.Red else Color(0xFF2E7D32))
                ProductDetailRow("Location", product.location.ifBlank { "Not specified" })
                ProductDetailRow("Contact", product.contactPreference.ifBlank { "In-app chat" })
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Description:", fontWeight = FontWeight.Bold)
                Text(text = if (product.description.isEmpty()) "No description provided." else product.description)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Seller:", fontWeight = FontWeight.Bold)
                Text(text = sellerName)
                if (sellerDepartment.isNotBlank()) {
                    Text(text = sellerDepartment, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = onChat, enabled = !product.isSold) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat with Seller")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReport) {
                    Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
                TextButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFavorite) "Saved" else "Wishlist")
                }
            }
        }
    )
}

@Composable
private fun ProductDetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Gray
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value, color = valueColor)
    }
}


@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val reasons = listOf("Fake Listing", "Inappropriate Content", "Scam", "Other")
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Product") },
        text = {
            Column {
                Text("Why are you reporting this product?")
                Spacer(modifier = Modifier.height(16.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (reason == selectedReason),
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reason)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReason) }) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProductCard(
    product: Product, 
    sellerName: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onContactSeller: () -> Unit,
    onViewDetails: () -> Unit
) {
    val imageUrl = primaryImageUrl(product)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
                
                // Favorite Button Overlay
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = formatProductPrice(product.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(text = "Category: ${product.category}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            if (product.condition.isNotBlank()) {
                Text(text = "Condition: ${product.condition}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            Text(text = "Seller: $sellerName", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            Text(
                text = "Status: ${productStatus(product)}",
                color = if (product.isSold) Color.Red else Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("More", fontSize = 11.sp)
                }
                
                Button(
                    onClick = onContactSeller,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(
    viewModel: ProductViewModel,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel
) {
    val wishlist = viewModel.wishlistProducts
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var selectedProductForReport by remember { mutableStateOf<Product?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "My Wishlist", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (wishlist.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your wishlist is empty", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(wishlist) { product ->
                    ProductCard(
                        product = product,
                        sellerName = viewModel.sellerNameFor(product.ownerId),
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleWishlist(product) },
                        onContactSeller = {
                            if (product.ownerId != currentUserId) {
                                chatViewModel.startOrGetChat(product.ownerId) { chatId ->
                                    authViewModel.currentChatId.value = chatId
                                    authViewModel.currentChatPartnerId.value = product.ownerId
                                    authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                                }
                            } else {
                                Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewDetails = {
                            selectedProductForDetail = product
                        }
                    )
                }
            }
        }
    }

    if (selectedProductForDetail != null) {
        ProductDetailDialog(
            product = selectedProductForDetail!!,
            sellerName = viewModel.sellerNameFor(selectedProductForDetail!!.ownerId),
            sellerDepartment = viewModel.sellerDepartmentFor(selectedProductForDetail!!.ownerId),
            isFavorite = true,
            onToggleFavorite = { viewModel.toggleWishlist(selectedProductForDetail!!) },
            onReport = { selectedProductForReport = selectedProductForDetail },
            onDismiss = { selectedProductForDetail = null },
            onChat = {
                if (selectedProductForDetail!!.ownerId != currentUserId) {
                    chatViewModel.startOrGetChat(selectedProductForDetail!!.ownerId) { chatId ->
                        authViewModel.currentChatId.value = chatId
                        authViewModel.currentChatPartnerId.value = selectedProductForDetail!!.ownerId
                        authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                    }
                } else {
                    Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                }
                selectedProductForDetail = null
            }
        )
    }

    if (selectedProductForReport != null) {
        ReportDialog(
            onDismiss = { selectedProductForReport = null },
            onConfirm = { reason ->
                viewModel.reportProduct(selectedProductForReport!!.id, reason) { success ->
                    if (success) {
                        Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                selectedProductForReport = null
            }
        )
    }
}

@Composable
fun MyProductsScreen(viewModel: ProductViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.clearProductMessages()
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(text = "My Products", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (viewModel.userProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "You haven't added any products yet.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.userProducts) { product ->
                        MyProductItem(
                            product = product,
                            sellerName = viewModel.sellerNameFor(product.ownerId),
                            onEdit = {
                                viewModel.clearProductMessages()
                                productToEdit = it
                            },
                            onDelete = { viewModel.deleteProduct(it.id) },
                            onMarkSold = { viewModel.markAsSold(it.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductDialog(
            isSubmitting = viewModel.isLoading.value,
            uploadStatus = viewModel.imageUploadStatus.value,
            submitError = viewModel.errorMessage.value,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, category, desc, condition, location, contactPreference, imageUris ->
                viewModel.addProduct(
                    name = name,
                    price = price,
                    category = category,
                    description = desc,
                    condition = condition,
                    location = location,
                    contactPreference = contactPreference,
                    imageUris = imageUris
                ) { success ->
                    if (success) {
                        showAddDialog = false
                    }
                }
            }
        )
    }

    if (productToEdit != null) {
        ProductDialog(
            product = productToEdit,
            isSubmitting = viewModel.isLoading.value,
            uploadStatus = viewModel.imageUploadStatus.value,
            submitError = viewModel.errorMessage.value,
            onDismiss = { productToEdit = null },
            onConfirm = { name, price, category, desc, condition, location, contactPreference, imageUris ->
                viewModel.updateProduct(productToEdit!!.copy(
                    name = name,
                    price = price,
                    category = category,
                    description = desc,
                    condition = condition,
                    location = location,
                    contactPreference = contactPreference
                ), imageUris) { success ->
                    if (success) {
                        productToEdit = null
                    }
                }
            }
        )
    }
}

@Composable
fun MyProductItem(
    product: Product,
    sellerName: String,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onMarkSold: (Product) -> Unit
) {
    val imageUrl = primaryImageUrl(product)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold)
                Text(text = formatProductPrice(product.price), color = MaterialTheme.colorScheme.primary)
                Text(text = "${product.category} - ${product.condition.ifBlank { "Condition not set" }}", color = Color.Gray, fontSize = 12.sp)
                Text(text = "Seller: $sellerName", color = Color.Gray, fontSize = 12.sp)
                if (product.location.isNotBlank()) {
                    Text(text = product.location, color = Color.Gray, fontSize = 12.sp)
                }
                Text(
                    text = "Status: ${productStatus(product)}",
                    color = if (product.isSold) Color.Red else Color(0xFF2E7D32),
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { onEdit(product) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(product) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
            if (!product.isSold) {
                IconButton(onClick = { onMarkSold(product) }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark as Sold", tint = Color.Green)
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    product: Product? = null,
    isSubmitting: Boolean = false,
    uploadStatus: String? = null,
    submitError: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, String, String, List<Uri>) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: productCategories.first()) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var condition by remember { mutableStateOf(product?.condition?.takeIf { it.isNotBlank() } ?: productConditions[2]) }
    var showConditionMenu by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(product?.location ?: "") }
    var contactPreference by remember { mutableStateOf(product?.contactPreference?.takeIf { it.isNotBlank() } ?: contactPreferences.first()) }
    var showContactMenu by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val existingImageUrls = product?.imageUrls?.ifEmpty {
        listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() })
    }.orEmpty()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris.take(6)
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable(enabled = !isSubmitting) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val previewUri = selectedImageUris.firstOrNull()
                    val existingPreviewUrl = existingImageUrls.firstOrNull()
                    if (previewUri != null) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (existingPreviewUrl != null) {
                        AsyncImage(
                            model = existingPreviewUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                            Text("Add Photo", color = Color.Gray)
                        }
                    }
                }

                TextButton(
                    onClick = { launcher.launch("image/*") },
                    enabled = !isSubmitting
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Images")
                }

                uploadStatus?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (selectedImageUris.isNotEmpty() || existingImageUrls.size > 1) {
                    val selectedItems = selectedImageUris.map { it.toString() }
                    val previewItems = selectedItems.ifEmpty { existingImageUrls }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(previewItems) { image ->
                            Box {
                                AsyncImage(
                                    model = image,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                                if (selectedImageUris.isNotEmpty() && !isSubmitting) {
                                    IconButton(
                                        onClick = {
                                            selectedImageUris = selectedImageUris.filter { it.toString() != image }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove image",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val displayedError = errorMessage ?: submitError
                if (displayedError != null) {
                    Text(
                        text = displayedError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                OutlinedTextField(
                    value = name, 
                    onValueChange = { 
                        name = it
                        errorMessage = null 
                    }, 
                    label = { Text("Product Title") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() && errorMessage != null
                )
                OutlinedTextField(
                    value = price, 
                    onValueChange = { 
                        price = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    }, 
                    label = { Text("Price") }, 
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    isError = (price.isBlank() || price.toDoubleOrNull() == null) && errorMessage != null
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCategoryMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        productCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    errorMessage = null
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showConditionMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(condition, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showConditionMenu,
                        onDismissRequest = { showConditionMenu = false }
                    ) {
                        productConditions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    condition = option
                                    errorMessage = null
                                    showConditionMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        errorMessage = null
                    },
                    label = { Text("Location") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    isError = location.isBlank() && errorMessage != null
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showContactMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(contactPreference, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showContactMenu,
                        onDismissRequest = { showContactMenu = false }
                    ) {
                        contactPreferences.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    contactPreference = option
                                    errorMessage = null
                                    showContactMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description, 
                    onValueChange = { 
                        description = it
                        errorMessage = null
                    }, 
                    label = { Text("Description") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 3,
                    enabled = !isSubmitting,
                    isError = description.isBlank() && errorMessage != null
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                val priceDouble = price.toDoubleOrNull()
                when {
                    name.isBlank() || price.isBlank() || category.isBlank() || description.isBlank() ||
                        condition.isBlank() || location.isBlank() || contactPreference.isBlank() -> {
                        errorMessage = "All listing fields must be filled in."
                    }
                    priceDouble == null -> {
                        errorMessage = "Please enter a valid price."
                    }
                    priceDouble <= 0.0 -> {
                        errorMessage = "Price must be greater than zero."
                    }
                    product == null && selectedImageUris.isEmpty() -> {
                        errorMessage = "Upload at least one product image."
                    }
                    else -> {
                        onConfirm(
                            name.trim(),
                            priceDouble,
                            category,
                            description.trim(),
                            condition,
                            location.trim(),
                            contactPreference,
                            selectedImageUris
                        )
                    }
                }
            }) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    when {
                        isSubmitting && product == null -> "Posting..."
                        isSubmitting -> "Saving..."
                        product == null -> "Post Product"
                        else -> "Save Product"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminScreen(viewModel: AdminViewModel) {
    if (!viewModel.isAdmin.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Admin access required", color = Color.Gray)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Monitor", "Reports", "Listings", "Users")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Admin Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        viewModel.message.value?.let {
            Text(
                text = it,
                color = if (it.contains("removed") || it.contains("reviewed") || it.contains("enabled") || it.contains("disabled")) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> AdminMonitorTab(viewModel)
            1 -> AdminReportsTab(viewModel)
            2 -> AdminListingsTab(viewModel)
            3 -> AdminUsersTab(viewModel)
        }
    }
}

@Composable
private fun AdminMonitorTab(viewModel: AdminViewModel) {
    val totalListings = viewModel.listings.size
    val soldListings = viewModel.listings.count { it.isSold }
    val activeListings = totalListings - soldListings
    val pendingReports = viewModel.reports.count { it.status == "pending" }
    val disabledUsers = viewModel.users.count { it.disabled }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            AdminMetricRow("Active listings", activeListings.toString(), Icons.Default.Storefront)
            AdminMetricRow("Sold listings", soldListings.toString(), Icons.Default.CheckCircle)
            AdminMetricRow("Pending reports", pendingReports.toString(), Icons.Default.Report)
            AdminMetricRow("Registered users", viewModel.users.size.toString(), Icons.Default.Groups)
            AdminMetricRow("Disabled users", disabledUsers.toString(), Icons.Default.Block)
        }
    }
}

@Composable
private fun AdminMetricRow(label: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AdminReportsTab(viewModel: AdminViewModel) {
    val reports = viewModel.reports

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports submitted", color = Color.Gray)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(reports) { report ->
            val product = viewModel.listings.find { it.id == report.productId }
            AdminReportItem(
                report = report,
                productName = product?.name ?: "Deleted or unavailable listing",
                onRemoveListing = { viewModel.removeListing(report.productId) },
                onReviewed = { viewModel.markReportReviewed(report.id) },
                onDismiss = { viewModel.dismissReport(report.id) }
            )
        }
    }
}

@Composable
private fun AdminReportItem(
    report: Report,
    productName: String,
    onRemoveListing: () -> Unit,
    onReviewed: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(productName, fontWeight = FontWeight.Bold)
                    Text(report.reason, fontSize = 13.sp, color = Color.Gray)
                }
                Text(report.status.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }

            Text("Reported ${formatAdminDate(report.timestamp)}", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Dismiss", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onReviewed, modifier = Modifier.weight(1f)) {
                    Text("Review", fontSize = 12.sp)
                }
                Button(onClick = onRemoveListing, modifier = Modifier.weight(1f)) {
                    Text("Remove", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminListingsTab(viewModel: AdminViewModel) {
    if (viewModel.listings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No listings found", color = Color.Gray)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viewModel.listings) { product ->
            AdminListingItem(
                product = product,
                ownerName = viewModel.users.find { it.uid == product.ownerId }?.fullName ?: "Unknown seller",
                onRemove = { viewModel.removeListing(product.id) }
            )
        }
    }
}

@Composable
private fun AdminListingItem(
    product: Product,
    ownerName: String,
    onRemove: () -> Unit
) {
    val imageUrl = primaryImageUrl(product)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("${formatProductPrice(product.price)} - ${product.category}", fontSize = 13.sp, color = Color.Gray)
                Text(product.condition.ifBlank { "Condition not set" }, fontSize = 12.sp, color = Color.Gray)
                Text(product.location.ifBlank { "Location not set" }, fontSize = 12.sp, color = Color.Gray)
                Text("Seller: $ownerName", fontSize = 12.sp, color = Color.Gray)
                Text(
                    productStatus(product).uppercase(),
                    color = if (product.isSold) Color.Red else Color(0xFF2E7D32),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove listing", tint = Color.Red)
            }
        }
    }
}

@Composable
private fun AdminUsersTab(viewModel: AdminViewModel) {
    if (viewModel.users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found", color = Color.Gray)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viewModel.users) { user ->
            AdminUserItem(
                user = user,
                onToggleDisabled = { viewModel.setUserDisabled(user.uid, !user.disabled) }
            )
        }
    }
}

@Composable
private fun AdminUserItem(
    user: MarketplaceUser,
    onToggleDisabled: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (user.fullName.isBlank()) "Unnamed user" else user.fullName, fontWeight = FontWeight.Bold)
                Text(user.email, fontSize = 13.sp, color = Color.Gray)
                Text("ID: ${user.studentId.ifBlank { "Not set" }}", fontSize = 12.sp, color = Color.Gray)
                Text("${user.department.ifBlank { "No department" }} - ${user.role}", fontSize = 12.sp, color = Color.Gray)
                if (user.disabled) {
                    Text("DISABLED", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Switch(
                checked = !user.disabled,
                onCheckedChange = { onToggleDisabled() }
            )
        }
    }
}

private fun formatAdminDate(timestamp: Long): String {
    if (timestamp == 0L) return "date unavailable"
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun ContactsScreen(viewModel: ChatViewModel, authViewModel: AuthViewModel) {
    val chats = viewModel.activeChats
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Chats", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No active conversations")
            }
        } else {
            LazyColumn {
                items(chats) { chat ->
                    val partnerId = chat.participantIds.find { it != currentUserId } ?: ""
                    ChatItem(
                        chat = chat,
                        partnerId = partnerId,
                        viewModel = viewModel,
                        onClick = {
                            authViewModel.currentChatId.value = chat.id
                            authViewModel.currentChatPartnerId.value = partnerId
                            authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, partnerId: String, viewModel: ChatViewModel, onClick: () -> Unit) {
    var partnerName by remember { mutableStateOf("Loading...") }
    
    LaunchedEffect(partnerId) {
        viewModel.fetchUserName(partnerId) { name ->
            partnerName = name
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = partnerName, fontWeight = FontWeight.Bold)
                Text(
                    text = chat.lastMessage,
                    maxLines = 1,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
