package com.lezzwatch.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.ui.about.AboutScreen
import com.lezzwatch.app.ui.channels.ChannelsScreen
import com.lezzwatch.app.ui.coffee.BuyMeACoffeeScreen
import com.lezzwatch.app.ui.home.HomeScreen
import com.lezzwatch.app.ui.settings.SettingsScreen

private data class BottomNavItem(val destination: Destination, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)

private val bottomNavItems = listOf(
    BottomNavItem(Destination.Home, Icons.Filled.Home, R.string.nav_home),
    BottomNavItem(Destination.Channels, Icons.Filled.LiveTv, R.string.nav_channels),
    BottomNavItem(Destination.Coffee, Icons.Filled.Coffee, R.string.nav_coffee),
)

/**
 * Hosts Home / Channels / Buy-Me-a-Coffee behind a bottom nav bar, plus Settings/About pushed on
 * top without their own bottom-nav tab (spec: keep the bottom nav to the essentials). The player
 * itself is a separate Activity (see [com.lezzwatch.app.player.PlayerActivity]) so it's a true
 * full-screen experience, not just another destination squeezed under this nav bar.
 */
@Composable
fun LezzwatchNavHost(onChannelSelected: (Channel) -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onChannelSelected = onChannelSelected,
                    onBrowseChannels = {
                        navController.navigate(Destination.Channels.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = { navController.navigate(Destination.Settings.route) },
                )
            }
            composable(Destination.Channels.route) {
                ChannelsScreen(onChannelSelected = onChannelSelected)
            }
            composable(Destination.Coffee.route) {
                BuyMeACoffeeScreen()
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate(Destination.About.route) },
                )
            }
            composable(Destination.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
