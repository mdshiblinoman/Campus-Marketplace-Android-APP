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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.campusmarketplace.notifications.NotificationType
import com.example.campusmarketplace.notifications.NotificationViewModel
import com.example.campusmarketplace.notifications.NotificationsScreen
import com.example.campusmarketplace.products.Product
import com.example.campusmarketplace.products.ProductApprovalStatus
import com.example.campusmarketplace.products.ProductAvailabilityStatus
import com.example.campusmarketplace.products.ProductCondition
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

private val categoryFilterOptions = listOf("All") + productCategories

private val productConditions = listOf(
    ProductCondition.New,
    ProductCondition.LikeNew,
    ProductCondition.Good,
    ProductCondition.Fair,
    ProductCondition.Used
)

private val contactPreferences = listOf(
    "In-app chat",
    "Phone call",
    "SMS",
    "Email"
)

private enum class ProductSortOption(val label: String) {
    Newest("Newest"),
    Oldest("Oldest"),
    LowestPrice("Lowest Price"),
    HighestPrice("Highest Price")
}

private enum class MyProductsFilter(val label: String) {
    All("All"),
    Available("Available"),
    Reserved("Reserved"),
    Sold("Sold"),
    Removed("Removed")
}

private enum class AdminUserFilter(val label: String) {
    All("All"),
    Active("Active"),
    Blocked("Blocked")
}

private enum class AdminReportFilter(val label: String, val status: String?) {
    All("All", null),
    Pending("Pending", "pending"),
    Reviewed("Reviewed", "reviewed"),
    Resolved("Resolved", "resolved")
}

private enum class AdminApprovalFilter(val label: String, val status: String?) {
    All("All", null),
    Pending("Pending", ProductApprovalStatus.Pending),
    Published("Published", ProductApprovalStatus.Approved),
    Rejected("Rejected", ProductApprovalStatus.Rejected)
}

private fun primaryImageUrl(product: Product): String {
    return product.imageUrls.firstOrNull().orEmpty().ifEmpty { product.imageUrl }
}

private fun formatProductPrice(price: Double): String {
    return "৳${DecimalFormat("#,##0.##").format(price)}"
}

private fun productStatus(product: Product): String {
    return when (productAvailabilityStatus(product)) {
        ProductAvailabilityStatus.Reserved -> "Reserved"
        ProductAvailabilityStatus.Sold -> "Sold"
        ProductAvailabilityStatus.Removed -> "Removed"
        else -> "Available"
    }
}

private fun productAvailabilityStatus(product: Product): String {
    if (product.isSold) return ProductAvailabilityStatus.Sold
    return when (product.availabilityStatus.trim().lowercase()) {
        ProductAvailabilityStatus.Reserved -> ProductAvailabilityStatus.Reserved
        ProductAvailabilityStatus.Sold -> ProductAvailabilityStatus.Sold
        ProductAvailabilityStatus.Removed -> ProductAvailabilityStatus.Removed
        else -> ProductAvailabilityStatus.Available
    }
}

private fun productConditionLabel(product: Product): String {
    return product.condition.takeIf { it.isNotBlank() } ?: ProductCondition.Used
}

private fun productApprovalLabel(product: Product): String {
    return when (product.approvalStatus) {
        ProductApprovalStatus.Pending -> "Pending Review"
        ProductApprovalStatus.Rejected -> "Rejected"
        else -> "Published"
    }
}

private fun isProductPublished(product: Product): Boolean {
    return productAvailabilityStatus(product) == ProductAvailabilityStatus.Available &&
        product.approvalStatus == ProductApprovalStatus.Approved
}

private fun productStatusColor(product: Product): Color {
    return when (productAvailabilityStatus(product)) {
        ProductAvailabilityStatus.Sold, ProductAvailabilityStatus.Removed -> Color.Red
        ProductAvailabilityStatus.Reserved -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
}

@Composable
private fun ProductStatusBadge(label: String) {
    val isProblem = label == "Rejected" || label == "Sold" || label == "Removed"
    val isWaiting = label == "Pending Review" || label == "Reserved"
    Surface(
        color = when {
            isProblem -> MaterialTheme.colorScheme.errorContainer
            isWaiting -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = when {
            isProblem -> MaterialTheme.colorScheme.onErrorContainer
            isWaiting -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun matchesCategory(product: Product, selectedCategory: String): Boolean {
    return selectedCategory == "All" ||
        product.category.equals(selectedCategory, ignoreCase = true)
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

private fun formatChatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
}

sealed class BottomNavItem(val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Icons.Default.Home, "Home")
    object Wishlist : BottomNavItem(Icons.Default.Favorite, "Wishlist")
    object MyProducts : BottomNavItem(Icons.Default.Inventory, "My Products")
    object Chats : BottomNavItem(Icons.AutoMirrored.Filled.Chat, "Chats")
    object Notifications : BottomNavItem(Icons.Default.Notifications, "Alerts")
    object Profile : BottomNavItem(Icons.Default.Person, "Profile")
    object Admin : BottomNavItem(Icons.Default.AdminPanelSettings, "Admin")
}

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val productViewModel: ProductViewModel = viewModel(key = currentUserId)
    val chatViewModel: ChatViewModel = viewModel(key = currentUserId)
    val adminViewModel: AdminViewModel = viewModel(key = "admin_$currentUserId")
    val unreadNotifications = notificationViewModel.unreadCount.value
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOfNotNull(
        BottomNavItem.Home,
        BottomNavItem.Wishlist,
        BottomNavItem.MyProducts,
        BottomNavItem.Chats,
        BottomNavItem.Notifications,
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
                        icon = {
                            if (item == BottomNavItem.Notifications && unreadNotifications > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(unreadNotifications.coerceAtMost(99).toString())
                                        }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        },
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
                BottomNavItem.Notifications -> NotificationsScreen(
                    viewModel = notificationViewModel,
                    onNotificationClick = { notification ->
                        if (
                            notification.type == NotificationType.ChatMessage &&
                            notification.relatedId.isNotBlank() &&
                            notification.createdBy.isNotBlank()
                        ) {
                            authViewModel.currentChatId.value = notification.relatedId
                            authViewModel.currentChatPartnerId.value = notification.createdBy
                            authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                        }
                    }
                )
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
    var selectedProductForReview by remember { mutableStateOf<Product?>(null) }
    var selectedProductForBlock by remember { mutableStateOf<Product?>(null) }
    val viewedCategories = remember { mutableStateListOf<String>() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    val products = viewModel.allProducts
    val context = androidx.compose.ui.platform.LocalContext.current
    val sellerNameSnapshot = viewModel.sellerNames.toMap()
    val sellerDepartmentSnapshot = viewModel.sellerDepartments.toMap()
    val minimumPrice = minPrice.toDoubleOrNull()
    val maximumPrice = maxPrice.toDoubleOrNull()
    val hasPriceFilter = minPrice.isNotBlank() || maxPrice.isNotBlank()
    val isPriceRangeInvalid = minimumPrice != null && maximumPrice != null && minimumPrice > maximumPrice
    val recentlyAddedProducts = remember(products.toList()) {
        products.sortedByDescending { it.createdAt }.take(6)
    }
    val wishlistSnapshot = viewModel.wishlistProducts.toList()
    val wishlistIdsSnapshot = viewModel.wishlistProductIds.toSet()
    val viewedCategorySnapshot = viewedCategories.toList()
    val recommendedProducts = remember(
        products.toList(),
        wishlistSnapshot,
        wishlistIdsSnapshot,
        viewedCategorySnapshot,
        currentUserId
    ) {
        val categoryScores = mutableMapOf<String, Int>()
        wishlistSnapshot.forEach { product ->
            val category = product.category.trim()
            if (category.isNotBlank()) {
                categoryScores[category] = (categoryScores[category] ?: 0) + 3
            }
        }
        viewedCategorySnapshot.forEach { category ->
            categoryScores[category] = (categoryScores[category] ?: 0) + 1
        }

        if (categoryScores.isEmpty()) {
            emptyList()
        } else {
            products
                .filter { product ->
                    product.ownerId != currentUserId &&
                        product.id !in wishlistIdsSnapshot &&
                        categoryScores.containsKey(product.category.trim())
                }
                .sortedWith(
                    compareByDescending<Product> { categoryScores[it.category.trim()] ?: 0 }
                        .thenByDescending { it.createdAt }
                )
                .take(6)
        }
    }
    val showRecentlyAdded = searchQuery.isBlank() &&
        selectedCategory == "All" &&
        !hasPriceFilter &&
        recentlyAddedProducts.isNotEmpty()
    val showRecommendedProducts = searchQuery.isBlank() &&
        selectedCategory == "All" &&
        !hasPriceFilter &&
        recommendedProducts.isNotEmpty()

    fun rememberProductInterest(product: Product) {
        val category = product.category.trim()
        if (category.isNotBlank() && category != "All" && !viewedCategories.contains(category)) {
            viewedCategories.add(category)
        }
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "All" && !viewedCategories.contains(selectedCategory)) {
            viewedCategories.add(selectedCategory)
        }
    }

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
                val matchesCategory = matchesCategory(product, selectedCategory)
                val matchesMinPrice = minimumPrice == null || product.price >= minimumPrice
                val matchesMaxPrice = maximumPrice == null || product.price <= maximumPrice

                matchesSearch && matchesCategory && matchesMinPrice && matchesMaxPrice && !isPriceRangeInvalid
            }
            .let { filtered ->
                when (sortOption) {
                    ProductSortOption.Newest -> filtered.sortedByDescending { it.createdAt }
                    ProductSortOption.Oldest -> filtered.sortedBy { it.createdAt }
                    ProductSortOption.LowestPrice -> filtered.sortedBy { it.price }
                    ProductSortOption.HighestPrice -> filtered.sortedByDescending { it.price }
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
                    categoryFilterOptions.forEach { category ->
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
                            leadingIcon = if (sortOption == option) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                null
                            },
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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoryFilterOptions) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    leadingIcon = if (selectedCategory == category) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
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
                label = { Text("Minimum Price (৳)") },
                singleLine = true,
                isError = isPriceRangeInvalid
            )
            OutlinedTextField(
                value = maxPrice,
                onValueChange = { maxPrice = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.weight(1f),
                label = { Text("Maximum Price (৳)") },
                singleLine = true,
                isError = isPriceRangeInvalid,
                trailingIcon = {
                    if (hasPriceFilter) {
                        IconButton(onClick = {
                            minPrice = ""
                            maxPrice = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear price filter")
                        }
                    }
                }
            )
        }

        if (isPriceRangeInvalid) {
            Text(
                text = "Minimum price cannot be higher than maximum price.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showRecentlyAdded && !viewModel.isLoading.value) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${recentlyAddedProducts.size} latest", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(292.dp)
            ) {
                items(
                    items = recentlyAddedProducts,
                    key = { product -> "recent_${product.id}" }
                ) { product ->
                    Box(modifier = Modifier.width(190.dp)) {
                        ProductCard(
                            product = product,
                            sellerName = viewModel.sellerNameFor(product.ownerId),
                            sellerRating = viewModel.sellerRatingSummary(product.ownerId),
                            isFavorite = viewModel.isFavorite(product.id),
                            onToggleFavorite = { viewModel.toggleWishlist(product) },
                            onContactSeller = {
                                if (product.ownerId != currentUserId) {
                                    chatViewModel.startOrGetChat(
                                        partnerId = product.ownerId,
                                        productId = product.id,
                                        productTitle = product.name
                                    ) { chatId ->
                                        authViewModel.currentChatId.value = chatId
                                        authViewModel.currentChatPartnerId.value = product.ownerId
                                        authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                                    }
                                } else {
                                    Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onViewDetails = {
                                rememberProductInterest(product)
                                selectedProductForDetail = product
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showRecommendedProducts && !viewModel.isLoading.value) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recommended for You",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Category matches", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(292.dp)
            ) {
                items(
                    items = recommendedProducts,
                    key = { product -> "recommended_${product.id}" }
                ) { product ->
                    Box(modifier = Modifier.width(190.dp)) {
                        ProductCard(
                            product = product,
                            sellerName = viewModel.sellerNameFor(product.ownerId),
                            sellerRating = viewModel.sellerRatingSummary(product.ownerId),
                            isFavorite = viewModel.isFavorite(product.id),
                            onToggleFavorite = { viewModel.toggleWishlist(product) },
                            onContactSeller = {
                                rememberProductInterest(product)
                                if (product.ownerId != currentUserId) {
                                    chatViewModel.startOrGetChat(
                                        partnerId = product.ownerId,
                                        productId = product.id,
                                        productTitle = product.name
                                    ) { chatId ->
                                        authViewModel.currentChatId.value = chatId
                                        authViewModel.currentChatPartnerId.value = product.ownerId
                                        authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                                    }
                                } else {
                                    Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onViewDetails = {
                                rememberProductInterest(product)
                                selectedProductForDetail = product
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    searchQuery.isNotBlank() -> "Search Results (${filteredProducts.size})"
                    hasPriceFilter -> "Budget Results (${filteredProducts.size})"
                    selectedCategory != "All" -> "$selectedCategory (${filteredProducts.size})"
                    else -> "Available Items (${filteredProducts.size})"
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
                        imageVector = if (searchQuery.isBlank() && selectedCategory == "All") {
                            Icons.Default.Inventory
                        } else {
                            Icons.Default.SearchOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            isPriceRangeInvalid -> "Invalid price range"
                            searchQuery.isNotBlank() -> "No products match your search"
                            hasPriceFilter -> "No products found in this price range"
                            selectedCategory != "All" -> "No $selectedCategory listings found"
                            else -> "No products found"
                        },
                        color = Color.Gray
                    )
                    Text(
                        text = when {
                            isPriceRangeInvalid -> "Set a minimum price lower than the maximum price."
                            searchQuery.isNotBlank() -> "Try a different title, category, condition, location, or seller."
                            hasPriceFilter -> "Adjust the minimum or maximum price."
                            selectedCategory != "All" -> "Choose another category or clear the filter."
                            else -> "Pull fresh products from Firestore."
                        },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = {
                        when {
                            isPriceRangeInvalid || hasPriceFilter -> {
                                minPrice = ""
                                maxPrice = ""
                            }
                            searchQuery.isNotBlank() -> searchQuery = ""
                            selectedCategory != "All" -> selectedCategory = "All"
                            else -> viewModel.refreshProducts()
                        }
                    }) {
                        Text(
                            when {
                                isPriceRangeInvalid || hasPriceFilter -> "Clear price"
                                searchQuery.isNotBlank() -> "Clear search"
                                selectedCategory != "All" -> "Clear category"
                                else -> "Tap to refresh"
                            }
                        )
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
                        sellerRating = viewModel.sellerRatingSummary(product.ownerId),
                        isFavorite = viewModel.isFavorite(product.id),
                        onToggleFavorite = { viewModel.toggleWishlist(product) },
                        onContactSeller = {
                            rememberProductInterest(product)
                            if (product.ownerId != currentUserId) {
                                chatViewModel.startOrGetChat(
                                    partnerId = product.ownerId,
                                    productId = product.id,
                                    productTitle = product.name
                                ) { chatId ->
                                    authViewModel.currentChatId.value = chatId
                                    authViewModel.currentChatPartnerId.value = product.ownerId
                                    authViewModel.navigateTo(com.example.campusmarketplace.auth.AuthScreenState.Chat)
                                }
                            } else {
                                Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewDetails = {
                            rememberProductInterest(product)
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
            sellerRating = viewModel.sellerRatingSummary(selectedProductForDetail!!.ownerId),
            isFavorite = viewModel.isFavorite(selectedProductForDetail!!.id),
            canBlockSeller = selectedProductForDetail!!.ownerId != currentUserId,
            isSellerBlocked = viewModel.hasBlockedUser(selectedProductForDetail!!.ownerId),
            isBlockedBySeller = viewModel.isBlockedByUser(selectedProductForDetail!!.ownerId),
            onToggleFavorite = { viewModel.toggleWishlist(selectedProductForDetail!!) },
            onReport = { selectedProductForReport = selectedProductForDetail },
            onReview = {
                selectedProductForReview = selectedProductForDetail
                selectedProductForDetail = null
            },
            onBlockSeller = { selectedProductForBlock = selectedProductForDetail },
            onUnblockSeller = {
                viewModel.unblockUser(selectedProductForDetail!!.ownerId) { success ->
                    if (success) {
                        Toast.makeText(context, "Seller unblocked", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { selectedProductForDetail = null },
            onChat = {
                if (selectedProductForDetail!!.ownerId != currentUserId) {
                    chatViewModel.startOrGetChat(
                        partnerId = selectedProductForDetail!!.ownerId,
                        productId = selectedProductForDetail!!.id,
                        productTitle = selectedProductForDetail!!.name
                    ) { chatId ->
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

    selectedProductForBlock?.let { product ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { selectedProductForBlock = null },
            title = { Text("Block seller?") },
            text = { Text("You will stop seeing listings from ${viewModel.sellerNameFor(product.ownerId)} and they will not be able to chat with you.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser(product.ownerId) { success ->
                            if (success) {
                                Toast.makeText(context, "Seller blocked", Toast.LENGTH_SHORT).show()
                                selectedProductForDetail = null
                            }
                        }
                        selectedProductForBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block Seller")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForBlock = null }) {
                    Text("Cancel")
                }
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

    selectedProductForReview?.let { product ->
        SellerReviewDialog(
            sellerName = viewModel.sellerNameFor(product.ownerId),
            onDismiss = { selectedProductForReview = null },
            onConfirm = { rating, comment ->
                viewModel.submitSellerReview(product, rating, comment) { success ->
                    if (success) {
                        Toast.makeText(context, "Review submitted successfully", Toast.LENGTH_SHORT).show()
                        selectedProductForReview = null
                    }
                }
            }
        )
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    sellerName: String,
    sellerDepartment: String,
    sellerRating: String,
    isFavorite: Boolean,
    canBlockSeller: Boolean,
    isSellerBlocked: Boolean,
    isBlockedBySeller: Boolean,
    onToggleFavorite: () -> Unit,
    onReport: () -> Unit,
    onReview: () -> Unit,
    onBlockSeller: () -> Unit,
    onUnblockSeller: () -> Unit,
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
                ProductDetailRow("Condition", productConditionLabel(product))
                ProductDetailRow("Posted", formatListingDate(product.createdAt))
                ProductDetailRow("Status", productStatus(product), productStatusColor(product))
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
                Text(text = sellerRating, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                if (isSellerBlocked) {
                    Text("You blocked this seller", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else if (isBlockedBySeller) {
                    Text("Messaging is unavailable with this seller", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onChat,
                enabled = isProductPublished(product) && !isSellerBlocked && !isBlockedBySeller
            ) {
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
                if (product.isSold) {
                    TextButton(onClick = onReview) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review")
                    }
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
                if (canBlockSeller) {
                    TextButton(
                        onClick = if (isSellerBlocked) onUnblockSeller else onBlockSeller,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSellerBlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (isSellerBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSellerBlocked) "Unblock" else "Block")
                    }
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
    val reasons = listOf(
        "Fake Product",
        "Scam",
        "Incorrect Information",
        "Inappropriate Content",
        "Duplicate Listing",
        "Suspicious Seller",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Listing") },
        text = {
            Column {
                Text("Why are you reporting this listing?")
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
fun SellerReviewDialog(
    sellerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    val ratingLabels = mapOf(
        1 to "Poor",
        2 to "Fair",
        3 to "Good",
        4 to "Very Good",
        5 to "Excellent"
    )
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Seller") },
        text = {
            Column {
                Text(sellerName.ifBlank { "Seller" }, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = {
                                rating = star
                                errorMessage = null
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star star",
                                tint = if (star <= rating) Color(0xFFFFB300) else Color.Gray
                            )
                        }
                    }
                }
                Text("${ratingLabels[rating]} - $rating Star", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        errorMessage = null
                    },
                    label = { Text("Review") },
                    placeholder = { Text("Very friendly and trustworthy seller.") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.trim().length < 5) {
                        errorMessage = "Write a short review comment."
                    } else {
                        onConfirm(rating, comment.trim())
                    }
                }
            ) {
                Text("Submit Review")
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
    sellerRating: String,
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
            Text(text = "Condition: ${productConditionLabel(product)}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            Text(text = "Seller: $sellerName", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            Text(text = sellerRating, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(
                text = "Status: ${productStatus(product)}",
                color = productStatusColor(product),
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
                    enabled = isProductPublished(product),
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
    var selectedProductForReview by remember { mutableStateOf<Product?>(null) }
    var selectedProductForBlock by remember { mutableStateOf<Product?>(null) }
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
                        sellerRating = viewModel.sellerRatingSummary(product.ownerId),
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleWishlist(product) },
                        onContactSeller = {
                            if (product.ownerId != currentUserId) {
                                chatViewModel.startOrGetChat(
                                    partnerId = product.ownerId,
                                    productId = product.id,
                                    productTitle = product.name
                                ) { chatId ->
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
            sellerRating = viewModel.sellerRatingSummary(selectedProductForDetail!!.ownerId),
            isFavorite = true,
            canBlockSeller = selectedProductForDetail!!.ownerId != currentUserId,
            isSellerBlocked = viewModel.hasBlockedUser(selectedProductForDetail!!.ownerId),
            isBlockedBySeller = viewModel.isBlockedByUser(selectedProductForDetail!!.ownerId),
            onToggleFavorite = { viewModel.toggleWishlist(selectedProductForDetail!!) },
            onReport = { selectedProductForReport = selectedProductForDetail },
            onReview = {
                selectedProductForReview = selectedProductForDetail
                selectedProductForDetail = null
            },
            onBlockSeller = { selectedProductForBlock = selectedProductForDetail },
            onUnblockSeller = {
                viewModel.unblockUser(selectedProductForDetail!!.ownerId) { success ->
                    if (success) {
                        Toast.makeText(context, "Seller unblocked", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { selectedProductForDetail = null },
            onChat = {
                if (selectedProductForDetail!!.ownerId != currentUserId) {
                    chatViewModel.startOrGetChat(
                        partnerId = selectedProductForDetail!!.ownerId,
                        productId = selectedProductForDetail!!.id,
                        productTitle = selectedProductForDetail!!.name
                    ) { chatId ->
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

    selectedProductForBlock?.let { product ->
        AlertDialog(
            onDismissRequest = { selectedProductForBlock = null },
            title = { Text("Block seller?") },
            text = { Text("You will stop seeing listings from ${viewModel.sellerNameFor(product.ownerId)} and they will not be able to chat with you.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser(product.ownerId) { success ->
                            if (success) {
                                Toast.makeText(context, "Seller blocked", Toast.LENGTH_SHORT).show()
                                selectedProductForDetail = null
                            }
                        }
                        selectedProductForBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block Seller")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForBlock = null }) {
                    Text("Cancel")
                }
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

    selectedProductForReview?.let { product ->
        SellerReviewDialog(
            sellerName = viewModel.sellerNameFor(product.ownerId),
            onDismiss = { selectedProductForReview = null },
            onConfirm = { rating, comment ->
                viewModel.submitSellerReview(product, rating, comment) { success ->
                    if (success) {
                        Toast.makeText(context, "Review submitted successfully", Toast.LENGTH_SHORT).show()
                        selectedProductForReview = null
                    }
                }
            }
        )
    }
}

@Composable
fun MyProductsScreen(viewModel: ProductViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }
    var productPendingSold by remember { mutableStateOf<Product?>(null) }
    var productPendingReserved by remember { mutableStateOf<Product?>(null) }
    var productPendingAvailable by remember { mutableStateOf<Product?>(null) }
    var selectedFilter by remember { mutableStateOf(MyProductsFilter.All) }
    val userProducts = viewModel.userProducts
    val availableProducts = userProducts.filter { productAvailabilityStatus(it) == ProductAvailabilityStatus.Available }
    val reservedProducts = userProducts.filter { productAvailabilityStatus(it) == ProductAvailabilityStatus.Reserved }
    val soldProducts = userProducts.filter { productAvailabilityStatus(it) == ProductAvailabilityStatus.Sold }
    val removedProducts = userProducts.filter { productAvailabilityStatus(it) == ProductAvailabilityStatus.Removed }
    val filteredProducts = when (selectedFilter) {
        MyProductsFilter.All -> userProducts
        MyProductsFilter.Available -> availableProducts
        MyProductsFilter.Reserved -> reservedProducts
        MyProductsFilter.Sold -> soldProducts
        MyProductsFilter.Removed -> removedProducts
    }
    val selectedFilterCount = when (selectedFilter) {
        MyProductsFilter.All -> userProducts.size
        MyProductsFilter.Available -> availableProducts.size
        MyProductsFilter.Reserved -> reservedProducts.size
        MyProductsFilter.Sold -> soldProducts.size
        MyProductsFilter.Removed -> removedProducts.size
    }

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
            Text(text = "My Products", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${availableProducts.size} available - ${reservedProducts.size} reserved - ${soldProducts.size} sold",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MyProductsFilter.entries) { filter ->
                    val count = when (filter) {
                        MyProductsFilter.All -> userProducts.size
                        MyProductsFilter.Available -> availableProducts.size
                        MyProductsFilter.Reserved -> reservedProducts.size
                        MyProductsFilter.Sold -> soldProducts.size
                        MyProductsFilter.Removed -> removedProducts.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)") },
                        leadingIcon = if (selectedFilter == filter) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            viewModel.errorMessage.value?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                    }
                }
            }

            if (userProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "You haven't added any products yet.", color = Color.Gray)
                        Text(text = "Tap + to post your first listing.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (selectedFilter) {
                                MyProductsFilter.Available -> Icons.Default.Storefront
                                MyProductsFilter.Reserved -> Icons.Default.Bookmark
                                MyProductsFilter.Sold -> Icons.Default.CheckCircle
                                MyProductsFilter.Removed -> Icons.Default.Delete
                                MyProductsFilter.All -> Icons.Default.Inventory
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No ${selectedFilter.label.lowercase()} listings",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "${selectedFilter.label} listings ($selectedFilterCount)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(
                        items = filteredProducts,
                        key = { product -> product.id }
                    ) { product ->
                        MyProductItem(
                            product = product,
                            sellerName = viewModel.sellerNameFor(product.ownerId),
                            onEdit = {
                                viewModel.clearProductMessages()
                                productToEdit = it
                            },
                            onDelete = { productPendingDelete = it },
                            onMarkReserved = { productPendingReserved = it },
                            onMarkAvailable = { productPendingAvailable = it },
                            onMarkSold = { productPendingSold = it }
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
            onConfirm = { name, price, category, desc, condition, location, contactPreference, imageUris, _ ->
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
            onConfirm = { name, price, category, desc, condition, location, contactPreference, imageUris, retainedImageUrls ->
                viewModel.updateProduct(productToEdit!!.copy(
                    name = name,
                    price = price,
                    category = category,
                    description = desc,
                    condition = condition,
                    location = location,
                    contactPreference = contactPreference,
                    imageUrl = retainedImageUrls.firstOrNull().orEmpty(),
                    imageUrls = retainedImageUrls
                ), imageUris) { success ->
                    if (success) {
                        productToEdit = null
                    }
                }
            }
        )
    }

    productPendingSold?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingSold = null },
            title = { Text("Mark this listing as sold?") },
            text = { Text("\"${product.name}\" will move to Sold and stop appearing in normal browsing.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsSold(product.id)
                        productPendingSold = null
                    }
                ) {
                    Text("Mark Sold")
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingSold = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    productPendingReserved?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingReserved = null },
            title = { Text("Reserve this listing?") },
            text = { Text("\"${product.name}\" will move from Available to Reserved and stop appearing in normal browsing.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsReserved(product.id)
                        productPendingReserved = null
                    }
                ) {
                    Text("Reserve")
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingReserved = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    productPendingAvailable?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingAvailable = null },
            title = { Text("Make this listing available?") },
            text = { Text("\"${product.name}\" will move back to Available after admin approval.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsAvailable(product.id)
                        productPendingAvailable = null
                    }
                ) {
                    Text("Make Available")
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingAvailable = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Are you sure you want to delete this listing?") },
            text = { Text("\"${product.name}\" will be removed from your products.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) {
                    Text("Cancel")
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
    onMarkReserved: (Product) -> Unit,
    onMarkAvailable: (Product) -> Unit,
    onMarkSold: (Product) -> Unit
) {
    val imageUrl = primaryImageUrl(product)
    val availabilityStatus = productAvailabilityStatus(product)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatProductPrice(product.price),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    ProductStatusBadge(productStatus(product))
                }
                if (availabilityStatus != ProductAvailabilityStatus.Sold) {
                    ProductStatusBadge(productApprovalLabel(product))
                }
                if (product.rejectionReason.isNotBlank()) {
                    Text("Reason: ${product.rejectionReason}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Text(text = "${product.category} - ${productConditionLabel(product)}", color = Color.Gray, fontSize = 12.sp)
                Text(text = "Seller: $sellerName", color = Color.Gray, fontSize = 12.sp)
                if (product.location.isNotBlank()) {
                    Text(text = product.location, color = Color.Gray, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { onEdit(product) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(product) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
            if (availabilityStatus == ProductAvailabilityStatus.Available) {
                IconButton(onClick = { onMarkReserved(product) }) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Mark as Reserved", tint = Color(0xFFF57C00))
                }
            }
            if (availabilityStatus == ProductAvailabilityStatus.Reserved) {
                IconButton(onClick = { onMarkAvailable(product) }) {
                    Icon(Icons.Default.Storefront, contentDescription = "Mark as Available", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (availabilityStatus == ProductAvailabilityStatus.Available || availabilityStatus == ProductAvailabilityStatus.Reserved) {
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
    onConfirm: (String, Double, String, String, String, String, String, List<Uri>, List<String>) -> Unit
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
    var retainedExistingImageUrls by remember(product?.id) { mutableStateOf(existingImageUrls) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val remainingSlots = (6 - retainedExistingImageUrls.size).coerceAtLeast(0)
        selectedImageUris = uris.take(remainingSlots)
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
                    val existingPreviewUrl = retainedExistingImageUrls.firstOrNull()
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
                    enabled = !isSubmitting && retainedExistingImageUrls.size < 6
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

                if (selectedImageUris.isNotEmpty() || retainedExistingImageUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = retainedExistingImageUrls,
                            key = { image -> image }
                        ) { image ->
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
                                if (product != null && !isSubmitting) {
                                    IconButton(
                                        onClick = {
                                            retainedExistingImageUrls = retainedExistingImageUrls.filter { it != image }
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
                        items(
                            items = selectedImageUris,
                            key = { uri -> uri.toString() }
                        ) { imageUri ->
                            Box {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                                if (!isSubmitting) {
                                    IconButton(
                                        onClick = {
                                            selectedImageUris = selectedImageUris.filter { it != imageUri }
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
                    product != null && retainedExistingImageUrls.isEmpty() && selectedImageUris.isEmpty() -> {
                        errorMessage = "Keep or upload at least one product image."
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
                            selectedImageUris,
                            retainedExistingImageUrls
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
    val tabs = listOf("Dashboard", "Reports", "Listings", "Users")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Admin Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        viewModel.message.value?.let {
            Text(
                text = it,
                color = if (
                    it.contains("removed") ||
                    it.contains("reviewed") ||
                    it.contains("enabled") ||
                    it.contains("disabled") ||
                    it.contains("blocked") ||
                    it.contains("sent") ||
                    it.contains("dismissed") ||
                    it.contains("resolved") ||
                    it.contains("approved") ||
                    it.contains("rejected") ||
                    it.contains("published")
                ) {
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
    val activeListings = viewModel.listings.count {
        productAvailabilityStatus(it) == ProductAvailabilityStatus.Available ||
            productAvailabilityStatus(it) == ProductAvailabilityStatus.Reserved
    }
    val soldListings = viewModel.listings.count {
        productAvailabilityStatus(it) == ProductAvailabilityStatus.Sold
    }
    val removedListings = viewModel.listings.count {
        productAvailabilityStatus(it) == ProductAvailabilityStatus.Removed
    }
    val pendingApprovals = viewModel.listings.count { it.approvalStatus == ProductApprovalStatus.Pending }
    val pendingReports = viewModel.reports.count { it.status == "pending" }
    val disabledUsers = viewModel.users.count { it.disabled }
    val latestReport = viewModel.reports.maxByOrNull { it.timestamp }
    val latestListing = viewModel.listings.maxByOrNull { it.createdAt }
    val latestUser = viewModel.users.maxByOrNull { it.registrationDate }
    var adminNotificationTitle by remember { mutableStateOf("") }
    var adminNotificationMessage by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            AdminMetricRow("Active listings", activeListings.toString(), Icons.Default.Storefront)
            AdminMetricRow("Sold listings", soldListings.toString(), Icons.Default.CheckCircle)
            AdminMetricRow("Removed listings", removedListings.toString(), Icons.Default.Delete)
            AdminMetricRow("Pending approvals", pendingApprovals.toString(), Icons.Default.PendingActions)
            AdminMetricRow("Pending reports", pendingReports.toString(), Icons.Default.Report)
            AdminMetricRow("Registered users", viewModel.users.size.toString(), Icons.Default.Groups)
            AdminMetricRow("Disabled users", disabledUsers.toString(), Icons.Default.Block)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marketplace Activity", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AdminActivityRow(
                        icon = Icons.Default.Report,
                        label = "Latest report",
                        value = latestReport?.let {
                            "${it.reason} - ${it.productTitle.ifBlank { "Listing unavailable" }}"
                        } ?: "No reports yet"
                    )
                    AdminActivityRow(
                        icon = Icons.Default.Storefront,
                        label = "Latest listing",
                        value = latestListing?.let {
                            "${it.name} - ${productApprovalLabel(it)}"
                        } ?: "No listings yet"
                    )
                    AdminActivityRow(
                        icon = Icons.Default.Person,
                        label = "Latest user",
                        value = latestUser?.let {
                            "${it.fullName.ifBlank { "Unnamed user" }} - ${it.department.ifBlank { "No department" }}"
                        } ?: "No users yet"
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Admin Notification", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminNotificationTitle,
                        onValueChange = { adminNotificationTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminNotificationMessage,
                        onValueChange = { adminNotificationMessage = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.sendAdminNotificationToAll(
                                title = adminNotificationTitle,
                                notificationMessage = adminNotificationMessage
                            )
                            if (adminNotificationTitle.isNotBlank() && adminNotificationMessage.isNotBlank()) {
                                adminNotificationTitle = ""
                                adminNotificationMessage = ""
                            }
                        },
                        enabled = adminNotificationTitle.isNotBlank() && adminNotificationMessage.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActivityRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, maxLines = 1)
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
    var selectedFilter by remember { mutableStateOf(AdminReportFilter.All) }
    val reports = viewModel.reports
    val filteredReports = selectedFilter.status?.let { status ->
        reports.filter { it.status.equals(status, ignoreCase = true) }
    } ?: reports

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports submitted", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminReportFilter.entries) { filter ->
                val count = filter.status?.let { status ->
                    reports.count { it.status.equals(status, ignoreCase = true) }
                } ?: reports.size
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.label} ($count)") },
                    leadingIcon = if (selectedFilter == filter) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No ${selectedFilter.label.lowercase()} reports", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = filteredReports,
                    key = { report -> report.id }
                ) { report ->
                    val product = viewModel.listings.find { it.id == report.productId }
                    val sellerId = report.sellerId.ifBlank { product?.ownerId.orEmpty() }
                    val seller = viewModel.users.find { it.uid == sellerId }
                    val reporter = viewModel.users.find { it.uid == report.reporterId }
                    AdminReportItem(
                        report = report,
                        productName = product?.name ?: report.productTitle.ifBlank { "Deleted or unavailable listing" },
                        reporterName = reporter?.fullName?.takeIf { it.isNotBlank() } ?: "Unknown reporter",
                        reporterStudentId = reporter?.studentId?.takeIf { it.isNotBlank() } ?: "Unknown",
                        sellerName = seller?.fullName?.takeIf { it.isNotBlank() } ?: "Unknown seller",
                        canBlockSeller = sellerId.isNotBlank() && seller?.disabled != true,
                        onRemoveListing = { viewModel.removeReportedListing(report.id) },
                        onBlockSeller = { viewModel.blockReportedSeller(report.id) },
                        onReviewed = { viewModel.markReportReviewed(report.id) },
                        onResolved = { viewModel.markReportResolved(report.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminReportItem(
    report: Report,
    productName: String,
    reporterName: String,
    reporterStudentId: String,
    sellerName: String,
    canBlockSeller: Boolean,
    onRemoveListing: () -> Unit,
    onBlockSeller: () -> Unit,
    onReviewed: () -> Unit,
    onResolved: () -> Unit
) {
    val normalizedStatus = report.status.lowercase()
    val canReview = normalizedStatus == "pending"
    val canResolve = normalizedStatus == "reviewed"
    val canTakeAction = normalizedStatus != "resolved" && normalizedStatus != "dismissed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Report #${report.id.takeLast(6).ifBlank { "new" }}", fontWeight = FontWeight.Bold)
                    Text("Product: $productName", fontSize = 13.sp)
                    Text("Reason: ${report.reason}", fontSize = 13.sp, color = Color.Gray)
                    Text("Reported By: Student ID $reporterStudentId", fontSize = 12.sp, color = Color.Gray)
                    Text("Reporter: $reporterName", fontSize = 12.sp, color = Color.Gray)
                    Text("Seller: $sellerName", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = reportStatusContainerColor(normalizedStatus),
                    contentColor = reportStatusContentColor(normalizedStatus),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = normalizedStatus.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text("Reported ${formatAdminDate(report.timestamp)}", fontSize = 12.sp, color = Color.Gray)
            if (report.adminAction.isNotBlank()) {
                Text("Action: ${formatAdminAction(report.adminAction)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReviewed,
                    enabled = canReview,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review", fontSize = 12.sp)
                }
                Button(
                    onClick = onResolved,
                    enabled = canResolve,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve", fontSize = 12.sp)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(
                    onClick = onRemoveListing,
                    enabled = canTakeAction,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onBlockSeller,
                    enabled = canTakeAction && canBlockSeller,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Block Seller", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatAdminAction(action: String): String {
    return action
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
private fun reportStatusContainerColor(status: String): Color {
    return when (status) {
        "pending" -> MaterialTheme.colorScheme.errorContainer
        "reviewed" -> MaterialTheme.colorScheme.secondaryContainer
        "resolved" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun reportStatusContentColor(status: String): Color {
    return when (status) {
        "pending" -> MaterialTheme.colorScheme.onErrorContainer
        "reviewed" -> MaterialTheme.colorScheme.onSecondaryContainer
        "resolved" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun AdminListingsTab(viewModel: AdminViewModel) {
    var selectedFilter by remember { mutableStateOf(AdminApprovalFilter.All) }
    val listings = viewModel.listings
    val filteredListings = selectedFilter.status?.let { status ->
        listings.filter { it.approvalStatus == status }
    } ?: listings

    if (listings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No listings found", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminApprovalFilter.entries) { filter ->
                val count = filter.status?.let { status ->
                    listings.count { it.approvalStatus == status }
                } ?: listings.size
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.label} ($count)") },
                    leadingIcon = if (selectedFilter == filter) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredListings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No ${selectedFilter.label.lowercase()} listings", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = filteredListings,
                    key = { product -> product.id }
                ) { product ->
                    val owner = viewModel.users.find { it.uid == product.ownerId }
                    AdminListingItem(
                        product = product,
                        ownerName = owner?.fullName ?: "Unknown seller",
                        ownerDisabled = owner?.disabled == true,
                        onApprove = { viewModel.approveProduct(product.id) },
                        onReject = { viewModel.rejectProduct(product.id) },
                        onRemove = { viewModel.removeListing(product.id) },
                        onBlockSeller = { viewModel.setUserDisabled(product.ownerId, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminListingItem(
    product: Product,
    ownerName: String,
    ownerDisabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
    onBlockSeller: () -> Unit
) {
    val imageUrl = primaryImageUrl(product)
    val isPending = product.approvalStatus == ProductApprovalStatus.Pending
    val isRejected = product.approvalStatus == ProductApprovalStatus.Rejected

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
                Text(productConditionLabel(product), fontSize = 12.sp, color = Color.Gray)
                Text(product.location.ifBlank { "Location not set" }, fontSize = 12.sp, color = Color.Gray)
                Text("Seller: $ownerName", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProductStatusBadge(productStatus(product))
                    ProductStatusBadge(productApprovalLabel(product))
                }
                if (product.rejectionReason.isNotBlank()) {
                    Text("Reason: ${product.rejectionReason}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onApprove,
                    enabled = isPending || isRejected
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Approve listing", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onReject,
                    enabled = isPending
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Reject listing", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove listing", tint = Color.Red)
                }
                IconButton(
                    onClick = onBlockSeller,
                    enabled = product.ownerId.isNotBlank() && !ownerDisabled
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Block seller",
                        tint = if (ownerDisabled) Color.Gray else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUsersTab(viewModel: AdminViewModel) {
    var selectedFilter by remember { mutableStateOf(AdminUserFilter.All) }
    var userPendingStatusChange by remember { mutableStateOf<MarketplaceUser?>(null) }
    val currentAdminId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val users = viewModel.users
    val activeUsers = users.filter { !it.disabled }
    val blockedUsers = users.filter { it.disabled }
    val filteredUsers = when (selectedFilter) {
        AdminUserFilter.All -> users
        AdminUserFilter.Active -> activeUsers
        AdminUserFilter.Blocked -> blockedUsers
    }

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${activeUsers.size} active - ${blockedUsers.size} blocked",
            color = Color.Gray,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminUserFilter.entries) { filter ->
                val count = when (filter) {
                    AdminUserFilter.All -> users.size
                    AdminUserFilter.Active -> activeUsers.size
                    AdminUserFilter.Blocked -> blockedUsers.size
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.label} ($count)") },
                    leadingIcon = if (selectedFilter == filter) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedFilter == AdminUserFilter.Blocked) {
                        "No blocked users"
                    } else {
                        "No active users"
                    },
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = filteredUsers,
                    key = { user -> user.uid }
                ) { user ->
                    AdminUserItem(
                        user = user,
                        canManage = user.uid != currentAdminId,
                        onChangeStatus = { userPendingStatusChange = user }
                    )
                }
            }
        }
    }

    userPendingStatusChange?.let { user ->
        val willBlock = !user.disabled
        AlertDialog(
            onDismissRequest = { userPendingStatusChange = null },
            title = { Text(if (willBlock) "Block user?" else "Unblock user?") },
            text = {
                Text(
                    if (willBlock) {
                        "${user.fullName.ifBlank { "This user" }} will lose marketplace access."
                    } else {
                        "${user.fullName.ifBlank { "This user" }} will regain marketplace access."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserDisabled(user.uid, willBlock)
                        userPendingStatusChange = null
                    },
                    colors = if (willBlock) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (willBlock) "Block User" else "Unblock User")
                }
            },
            dismissButton = {
                TextButton(onClick = { userPendingStatusChange = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminUserItem(
    user: MarketplaceUser,
    canManage: Boolean,
    onChangeStatus: () -> Unit
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
                Text(user.department.ifBlank { "Unknown" }, fontSize = 13.sp, color = Color.Gray)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Text("ID: ${user.studentId.ifBlank { "Not set" }} - ${user.role}", fontSize = 12.sp, color = Color.Gray)
                Surface(
                    color = if (user.disabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (user.disabled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (user.disabled) "Blocked" else "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (user.disabled) {
                OutlinedButton(
                    onClick = onChangeStatus,
                    enabled = canManage
                ) {
                    Text("Unblock", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onChangeStatus,
                    enabled = canManage,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block", fontSize = 12.sp)
                }
            }
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
    val isLoading = viewModel.isLoading.value
    val error = viewModel.error.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Chats", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoading && chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No active conversations", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = chats,
                    key = { chat -> chat.id }
                ) { chat ->
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val unreadCount = chat.unreadCount[currentUserId] ?: 0L
    val roleLabel = when (currentUserId) {
        chat.buyerId -> "Seller"
        chat.sellerId -> "Buyer"
        else -> "Participant"
    }
    val hasBlockedPartner = viewModel.hasBlockedUser(partnerId)
    val isBlockedByPartner = viewModel.isBlockedByUser(partnerId)
    val lastMessagePrefix = if (chat.lastSenderId == currentUserId) "You: " else ""
    val lastMessageText = chat.lastMessage.ifBlank { "No messages yet" }
    
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = partnerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (hasBlockedPartner || isBlockedByPartner) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (hasBlockedPartner) "Blocked" else "Limited",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = formatChatTimestamp(chat.lastMessageTimestamp),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                if (chat.productTitle.isNotBlank()) {
                    Text(
                        text = "$roleLabel about ${chat.productTitle}",
                        maxLines = 1,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "$lastMessagePrefix$lastMessageText",
                    maxLines = 1,
                    fontSize = 14.sp,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Text(
                        text = unreadCount.coerceAtMost(99).toString(),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
