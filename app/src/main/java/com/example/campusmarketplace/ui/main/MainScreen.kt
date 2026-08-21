package com.example.campusmarketplace.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.profile.ProfileScreen
import com.example.campusmarketplace.profile.ProfileViewModel

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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home Page")
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
