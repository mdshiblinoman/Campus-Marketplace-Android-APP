package com.example.campusmarketplace.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.chat.Chat
import com.example.campusmarketplace.chat.ChatViewModel
import com.example.campusmarketplace.products.Product
import com.example.campusmarketplace.products.ProductViewModel
import com.example.campusmarketplace.profile.ProfileScreen
import com.example.campusmarketplace.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class BottomNavItem(val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Icons.Default.Home, "Home")
    object MyProducts : BottomNavItem(Icons.Default.Inventory, "My Products")
    object Chats : BottomNavItem(Icons.AutoMirrored.Filled.Chat, "Chats")
    object Profile : BottomNavItem(Icons.Default.Person, "Profile")
}

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val productViewModel: ProductViewModel = viewModel(key = currentUserId)
    val chatViewModel: ChatViewModel = viewModel(key = currentUserId)
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.MyProducts,
        BottomNavItem.Chats,
        BottomNavItem.Profile
    )

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
                BottomNavItem.MyProducts -> MyProductsScreen(productViewModel)
                BottomNavItem.Chats -> ContactsScreen(chatViewModel, authViewModel)
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
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    val products = viewModel.allProducts
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar and Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = { viewModel.refreshProducts() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Filter by Category") },
                        onClick = { showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Filter by Price") },
                        onClick = { showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort Products") },
                        onClick = { showFilterMenu = false }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Items (${products.size})",
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
        } else if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products found", color = Color.Gray)
                    TextButton(onClick = { viewModel.refreshProducts() }) {
                        Text("Tap to refresh")
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
                items(products.filter { it.name.contains(searchQuery, ignoreCase = true) }) { product ->
                    ProductCard(
                        product = product,
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
}

@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onChat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = product.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Price: $${product.price}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = "Category: ${product.category}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Description:", fontWeight = FontWeight.Bold)
                Text(text = if (product.description.isEmpty()) "No description provided." else product.description)
            }
        },
        confirmButton = {
            Button(onClick = onChat) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat with Seller")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun ProductCard(
    product: Product, 
    onContactSeller: () -> Unit,
    onViewDetails: () -> Unit
) {
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
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = "$${product.price}", color = MaterialTheme.colorScheme.primary)
            Text(text = product.category, fontSize = 12.sp, color = Color.Gray)
            
            if (product.isSold) {
                Text(
                    text = "SOLD",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            
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
fun MyProductsScreen(viewModel: ProductViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
                            onEdit = { productToEdit = it },
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
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, category, desc, uri ->
                viewModel.addProduct(name, price, category, desc, uri)
                showAddDialog = false
            }
        )
    }

    if (productToEdit != null) {
        ProductDialog(
            product = productToEdit,
            onDismiss = { productToEdit = null },
            onConfirm = { name, price, category, desc, uri ->
                viewModel.updateProduct(productToEdit!!.copy(
                    name = name,
                    price = price,
                    category = category,
                    description = desc
                ), uri)
                productToEdit = null
            }
        )
    }
}

@Composable
fun MyProductItem(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onMarkSold: (Product) -> Unit
) {
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
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
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
                Text(text = "$${product.price}", color = MaterialTheme.colorScheme.primary)
                if (product.isSold) {
                    Text(text = "Status: SOLD", color = Color.Red, fontSize = 12.sp)
                }
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
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (product?.imageUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = product.imageUrl,
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
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
                    label = { Text("Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() && errorMessage != null
                )
                OutlinedTextField(
                    value = price, 
                    onValueChange = { 
                        price = it
                        errorMessage = null
                    }, 
                    label = { Text("Price") }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = (price.isBlank() || price.toDoubleOrNull() == null) && errorMessage != null
                )
                OutlinedTextField(
                    value = category, 
                    onValueChange = { 
                        category = it
                        errorMessage = null
                    }, 
                    label = { Text("Category") }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = category.isBlank() && errorMessage != null
                )
                OutlinedTextField(
                    value = description, 
                    onValueChange = { 
                        description = it
                        errorMessage = null
                    }, 
                    label = { Text("Description") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 3,
                    isError = description.isBlank() && errorMessage != null
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val priceDouble = price.toDoubleOrNull()
                when {
                    name.isBlank() || price.isBlank() || category.isBlank() || description.isBlank() -> {
                        errorMessage = "All fields except the image must be filled in."
                    }
                    priceDouble == null -> {
                        errorMessage = "Please enter a valid price."
                    }
                    else -> {
                        onConfirm(name, priceDouble, category, description, selectedImageUri)
                    }
                }
            }) {
                Text("Confirm")
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
