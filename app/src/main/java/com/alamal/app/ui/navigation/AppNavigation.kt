package com.alamal.app.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alamal.app.R
import com.alamal.app.data.AppRepository
import com.alamal.app.data.UndoAction
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.screens.archive.ArchiveScreen
import com.alamal.app.ui.screens.credit.CreditFormScreen
import com.alamal.app.ui.screens.home.HomeScreen
import com.alamal.app.ui.screens.payment.PaymentScreen
import com.alamal.app.ui.screens.records.RecordsScreen
import com.alamal.app.ui.screens.remote.RemoteWorkFormScreen
import com.alamal.app.ui.screens.settings.SettingsScreen
import com.alamal.app.ui.screens.stats.StatsScreen
import com.alamal.app.ui.screens.workday.WorkDayFormScreen
import com.alamal.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val bottomRoutes = setOf(
    Routes.HOME, Routes.RECORDS, Routes.ARCHIVE, Routes.SETTINGS
)

@Composable
fun AppNavigation(
    pendingShortcutAction: MutableState<String?>? = null,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val repo = remember { AppRepository.get(context) }

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

    // ---- Undo Snackbar pipeline ----
    LaunchedEffect(Unit) {
        repo.pendingUndo.collect { action ->
            val message = when (action) {
                is UndoAction.DeletedWorkDay -> "تم حذف يوم العمل"
                is UndoAction.DeletedRemoteWork -> "تم حذف العمل عن بُعد"
                is UndoAction.DeletedCredit -> "تم حذف الكريدي"
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "تراجع",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                repo.undoDelete(action)
            }
        }
    }

    // Handle shortcut actions from the launcher icon.
    if (pendingShortcutAction != null) {
        LaunchedEffect(pendingShortcutAction.value) {
            val action = pendingShortcutAction.value ?: return@LaunchedEffect
            val settingsRepo = SettingsRepository(context)
            when (action) {
                "clock_in" -> {
                    val open = repo.getOpenWorkDay()
                    if (open == null) {
                        val s = settingsRepo.settings.first()
                        repo.clockIn(s.hourlyRate)
                        Toast.makeText(context, "تم تسجيل الدخول", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "أنت بالفعل في العمل", Toast.LENGTH_SHORT).show()
                    }
                }
                "clock_out" -> {
                    val open = repo.getOpenWorkDay()
                    if (open != null) {
                        navController.navigate(Routes.workDayForm(open.id))
                    } else {
                        Toast.makeText(context, "لا يوجد يوم عمل مفتوح", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            pendingShortcutAction.value = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                FloatingBottomBar(
                    items = items,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (currentRoute == route) return@FloatingBottomBar
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    onOpenStats = {
                        navController.navigate(Routes.STATS)
                    },
                )
            }
            composable(Routes.RECORDS) {
                RecordsScreen(
                    onOpenWorkDay = { id -> navController.navigate(Routes.workDayForm(id)) },
                    onOpenRemote = { id -> navController.navigate(Routes.remoteWorkForm(id)) },
                    onOpenCredit = { id -> navController.navigate(Routes.creditForm(id)) },
                )
            }
            composable(Routes.ARCHIVE) { ArchiveScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(Routes.PAYMENT) {
                PaymentScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
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

@Composable
private fun FloatingBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val border = LocalExtendedColors.current.border

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                .border(1.dp, border, shape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                FloatingNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item.route) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalExtendedColors.current.accent
    val accentOn = LocalExtendedColors.current.accentOn
    val muted = LocalExtendedColors.current.muted

    val bgColor = if (selected) accent else Color.Transparent
    val contentColor = if (selected) accentOn else muted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (selected) 16.dp else 14.dp,
                vertical = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.labelRes),
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
