package net.crowdventures.storypop.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String?, val title: String?, val icon: ImageVector?) {
    object Stories : Screen("stories", "Home", Icons.Filled.Home)
    object Following : Screen("following", "Following", Icons.Filled.Group)
    object Notifications : Screen("notifications", "Alerts", Icons.Filled.Notifications)
    object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    object Messages : Screen("messages", "Messages", null) // exclude below from nav-items below
}
val items = listOf(
    Screen.Stories,
    Screen.Following,
    Screen.Notifications,
    Screen.Profile
)