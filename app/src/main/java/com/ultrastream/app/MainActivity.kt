package com.ultrastream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.ultrastream.app.ui.navigation.Screen
import com.ultrastream.app.ui.screens.addons.AddonsScreen
import com.ultrastream.app.ui.screens.details.DetailsScreen
import com.ultrastream.app.ui.screens.home.HomeScreen
import com.ultrastream.app.ui.screens.library.LibraryScreen
import com.ultrastream.app.ui.screens.player.PlayerScreen
import com.ultrastream.app.ui.screens.profile.ProfileScreen
import com.ultrastream.app.ui.screens.search.SearchScreen
import com.ultrastream.app.ui.theme.UltraStreamTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UltraStreamTheme {
                UltraStreamNavHost()
            }
        }
    }
}

@Composable
fun UltraStreamNavHost() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(
                    Screen.Home to "Home" to R.drawable.ic_home,
                    Screen.Library to "Library" to R.drawable.ic_library,
                    Screen.Search to "Search" to R.drawable.ic_search,
                    Screen.Addons to "Addons" to R.drawable.ic_addon,
                    Screen.Profile to "Profile" to R.drawable.ic_profile
                )
                items.forEach { (screen, title, iconRes) ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = ImageVector.vectorResource(id = iconRes), contentDescription = title) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen { id, type ->
                    navController.navigate(Screen.Details.pass(id, type))
                }
            }
            composable(Screen.Library.route) {
                LibraryScreen { id, type ->
                    navController.navigate(Screen.Details.pass(id, type))
                }
            }
            composable(Screen.Search.route) {
                SearchScreen { id, type ->
                    navController.navigate(Screen.Details.pass(id, type))
                }
            }
            composable(Screen.Addons.route) {
                AddonsScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.Details.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: ""
                DetailsScreen(
                    id = id,
                    type = type,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title ->
                        navController.navigate(Screen.Player.pass(url))
                    }
                )
            }
            composable(Screen.Player.route) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                PlayerScreen(
                    url = url,
                    title = "Now Playing",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
