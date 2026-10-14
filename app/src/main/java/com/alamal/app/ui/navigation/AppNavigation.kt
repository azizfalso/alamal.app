package com.alamal.app.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alamal.app.R
import com.alamal.app.ui.screens.archive.ArchiveScreen
import com.alamal.app.ui.screens.credit.CreditFormScreen
import com.alamal.app.ui.screens.home.HomeScreen
import com.alamal.app.ui.screens.payment.PaymentScreen
import com.alamal.app.ui.screens.placeholder.PlaceholderScreen
import com.alamal.app.ui.screens.remote.RemoteWorkFormScreen
import com.alamal.app.ui.screens.settings.SettingsScreen
import com.alamal.app.ui.screens.workday.WorkDayFormScreen

private data class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val bottomRoutes = setOf(
    Routes.HOME, Routes.RECORDS, Routes.ARCHIVE, Routes.SETTINGS
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val items = listOf(
        BottomNavItem(Routes.HOME, R.string.nav_home, Icons.Default.Home),
        BottomNavItem(Routes.RECORDS, R.string.nav_records, Icons.AutoMirrored.Filled.List),
        BottomNavItem(Routes.ARCHIVE, R.string.nav_archive, Icons.Default.Archive),
        BottomNavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes

    var lastBackPress by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = currentRoute == Routes.HOME) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPress = now
            Toast.makeText(context, R.string.press_back_again, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute == item.route) return@NavigationBarItem
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = stringResource(item.labelRes))
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkDayForm = { dayId ->
                        navController.navigate(Routes.workDayForm(dayId))
                    },
                    onOpenRemoteWorkForm = {
                        navController.navigate(Routes.remoteWorkForm(-1L))
                    },
                    onOpenCreditForm = {
                        navController.navigate(Routes.creditForm(-1L))
                    },
                    onOpenPayment = {
                        navController.navigate(Routes.PAYMENT)
                    },
                )
            }
            composable(Routes.RECORDS) {
                PlaceholderScreen(title = stringResource(R.string.nav_records))
            }
            composable(Routes.ARCHIVE) { ArchiveScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(Routes.PAYMENT) {
                PaymentScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Routes.workDayFormPattern(),
                arguments = listOf(
                    navArgument(Routes.ARG_DAY_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                val dayId = entry.arguments?.getLong(Routes.ARG_DAY_ID) ?: -1L
                WorkDayFormScreen(
                    dayId = dayId,
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.remoteWorkFormPattern(),
                arguments = listOf(
                    navArgument(Routes.ARG_ITEM_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                val itemId = entry.arguments?.getLong(Routes.ARG_ITEM_ID) ?: -1L
                RemoteWorkFormScreen(
                    itemId = itemId,
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.creditFormPattern(),
                arguments = listOf(
                    navArgument(Routes.ARG_ITEM_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                val itemId = entry.arguments?.getLong(Routes.ARG_ITEM_ID) ?: -1L
                CreditFormScreen(
                    itemId = itemId,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
