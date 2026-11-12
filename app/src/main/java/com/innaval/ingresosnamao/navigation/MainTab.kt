package com.innaval.ingresosnamao.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    ORDERS("Meus pedidos", Icons.AutoMirrored.Filled.List),
    CART("Carrinho", Icons.Default.ShoppingCart),
    PROFILE("Meu perfil", Icons.Default.Person),
}

@Composable
internal fun BottomNavigation(selected: MainTab, enabled: Boolean, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                enabled = enabled,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
