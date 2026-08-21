package com.example.campusmarketplace.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.profile.ProfileScreen
import com.example.campusmarketplace.profile.ProfileViewModel

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String = ""
)

sealed class BottomNavItem(val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Icons.Default.Home, "Home")
    object MyProducts : BottomNavItem(Icons.Default.Inventory, "My Products")
    object Contacts : BottomNavItem(Icons.AutoMirrored.Filled.Chat, "Contacts")
    object Profile : BottomNavItem(Icons.Default.Person, "Profile")
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.MyProducts,
        BottomNavItem.Contacts,
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
                BottomNavItem.Home -> HomeScreen()
                BottomNavItem.MyProducts -> MyProductsScreen()
                BottomNavItem.Contacts -> ContactsScreen()
                BottomNavItem.Profile -> {
                    val profileViewModel: ProfileViewModel = viewModel()
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onBack = { selectedItem = 0 } // Go to Home tab on back
                    )
                }
            }
        }
    }
}


@Composable
fun HomeScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Dummy products
    val products = remember {
        listOf(
            Product("1", "Textbook", 50.0, "Books"),
            Product("2", "Lab Coat", 25.0, "Clothing"),
            Product("3", "Calculator", 15.0, "Electronics"),
            Product("4", "Backpack", 40.0, "Accessories"),
            Product("5", "Lamp", 20.0, "Furniture"),
            Product("6", "Notebook", 5.0, "Books")
        )
    }

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
        
        // Product List
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                ProductCard(product)
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = "$${product.price}", color = MaterialTheme.colorScheme.primary)
            Text(text = product.category, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MyProductsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "My Products Page")
    }
}

@Composable
fun ContactsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Contacts Page")
    }
}
