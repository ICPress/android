package net.crowdventures.storypop.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.R
import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.viewmodels.RegisterViewModel

class BottomNavComposable {
    companion object{
        @Composable
        fun BottomNav(navController: NavController,registerViewModel: RegisterViewModel,lifecycleOwner: LifecycleOwner) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination

            // Use MaterialTheme colors directly - they're already theme-aware
            val unselectedIconTint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            val unselectedTextColor = MaterialTheme.colors.onSurface

            BottomNavigation(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 0.dp
            ) {
                items.forEach { screen:Screen->
                    if (screen.route ==Screen.Notifications.route!!){
                        //Empty BottomNavigationItem
                        BottomNavigationItem(
                            icon = {},
                            label = {  },
                            selected = false,
                            onClick = {  },
                            enabled = false
                        )
                    }
                    BottomNavigationItem(
                        icon = {
                            screen.icon?.let {
                                Column(
                                    Modifier
                                ) {
                                    var loggedInUser by remember { mutableStateOf<AccountInfoFull?>(Constants.loggedInUser) }
                                    registerViewModel.loggedInUser.observe(lifecycleOwner) { x ->
                                        loggedInUser = x
                                    }
                                    var followedNotificationPending  by remember {
                                        mutableStateOf(false)
                                    }
                                    var userNotificationsConsumed  by remember {
                                        mutableStateOf(false)
                                    }
                                    registerViewModel.userFollowedNotificationsPending.observe(lifecycleOwner){ x->
                                        if (x!= null) followedNotificationPending = x
                                    }
                                    registerViewModel.userNotificationsConsumed.observe(lifecycleOwner){ x->
                                        if (x!= null) userNotificationsConsumed = x
                                    }
                                    val currentLoggedInUser = loggedInUser
                                    if (screen.route ==Screen.Notifications.route && currentLoggedInUser!= null && !userNotificationsConsumed && currentLoggedInUser.unreadNotifications >0u){
                                        BadgedBox(badge = {
                                            Badge (
                                                modifier = Modifier,
                                                backgroundColor = MaterialTheme.colors.error
                                            ) {
                                                Text(
                                                    currentLoggedInUser.unreadNotifications.toString(),
                                                    color = MaterialTheme.colors.onError,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }) {
                                            Screen.Notifications.icon?.let {
                                                Icon(
                                                    imageVector = it,
                                                    tint =  if (currentRoute?.route == screen.route)
                                                        MaterialTheme.colors.secondary
                                                    else
                                                        unselectedIconTint,
                                                    contentDescription = "",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                )
                                            }
                                        }
                                    }else if (screen.route ==Screen.Following.route!! && currentLoggedInUser!= null && followedNotificationPending && currentLoggedInUser.unreadFollowedStories >0u){
                                        BadgedBox(badge = {
                                            Badge (
                                                modifier = Modifier,
                                                backgroundColor = MaterialTheme.colors.secondary
                                            ) {
                                                Text(
                                                    currentLoggedInUser.unreadFollowedStories.toString(),
                                                    color = MaterialTheme.colors.onSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }) {
                                            Screen.Following.icon?.let {
                                                Icon(
                                                    imageVector = it,
                                                    tint =  if (currentRoute?.route == screen.route)
                                                        MaterialTheme.colors.secondary
                                                    else
                                                        unselectedIconTint,
                                                    contentDescription = "",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Icon(
                                            imageVector = it,
                                            tint = if (currentRoute?.route == screen.route)
                                                MaterialTheme.colors.secondary
                                            else
                                                unselectedIconTint,
                                            contentDescription = "",
                                            modifier = Modifier
                                                .size(24.dp))
                                    }
                                }
                            }
                        },
                        label ={Text(screen.title.toString(),
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally),
                            color = if (currentRoute?.route == screen.route)
                                MaterialTheme.colors.secondary
                            else
                                unselectedTextColor,
                            fontSize = 11.sp
                        )},
                        alwaysShowLabel = true,
                        selected = currentRoute?.hierarchy?.any { it.route == screen.route } == true,
                        selectedContentColor = MaterialTheme.colors.secondary,
                        unselectedContentColor = unselectedIconTint,
                        onClick = {
                            screen.route?.let { it1 ->
                                navController.navigate(it1) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}