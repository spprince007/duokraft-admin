package se.duokraft.admin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import se.duokraft.admin.DuokraftApp
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.ui.content.ContentEditScreen
import se.duokraft.admin.ui.content.ContentListScreen
import se.duokraft.admin.ui.dashboard.DashboardScreen
import se.duokraft.admin.ui.login.LoginScreen
import se.duokraft.admin.ui.media.MediaScreen
import se.duokraft.admin.ui.more.MoreScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val MEDIA = "media?upload={upload}"
    const val MORE = "more"
    const val CONTENT = "content/{type}"
    const val EDIT = "edit/{type}/{id}"

    fun content(type: String) = "content/$type"
    fun edit(type: String, id: Long = 0) = "edit/$type/$id"
    fun media(upload: Boolean = false) = "media?upload=$upload"
}

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as DuokraftApp).container

private data class Tab(val route: String, val match: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    Tab(Routes.content("page"), "content/page", "Pages", Icons.Filled.Description),
    Tab(Routes.content("post"), "content/post", "Posts", Icons.Filled.Article),
    Tab(Routes.media(), "media", "Media", Icons.Filled.PhotoLibrary),
    Tab(Routes.MORE, Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@Composable
fun AppNav() {
    val container = appContainer()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val currentPath = when (route) {
        Routes.CONTENT -> "content/" + backStack?.arguments?.getString("type")
        Routes.MEDIA -> "media"
        else -> route
    }
    val showTabs = route != null && route != Routes.LOGIN && route != Routes.EDIT

    // The website rejected our stored login (revoked or expired): back to the login screen.
    LaunchedEffect(Unit) {
        container.session.expired.collect {
            container.site.forget()
            nav.navigate(Routes.LOGIN) { popUpTo(0) }
            snackbar.showSnackbar("Your login has expired. Please log in again.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showTabs) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentPath == tab.match,
                        onClick = { nav.goTab(tab.route) },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (container.session.isLoggedIn) Routes.DASHBOARD else Routes.LOGIN,
            modifier = Modifier.padding(bottom = if (showTabs) padding.calculateBottomPadding() else 0.dp),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(onLoggedIn = { nav.navigate(Routes.DASHBOARD) { popUpTo(0) } })
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    snackbar = snackbar,
                    openList = { type -> nav.goTab(Routes.content(type)) },
                    openEditor = { type, id -> nav.navigate(Routes.edit(type, id)) },
                    openMedia = { upload -> nav.goTab(Routes.media(upload)) },
                )
            }
            composable(Routes.CONTENT, arguments = listOf(navArgument("type") { type = NavType.StringType })) { entry ->
                val type = entry.arguments?.getString("type") ?: "page"
                ContentListScreen(
                    typeSlug = type,
                    snackbar = snackbar,
                    openEditor = { id -> nav.navigate(Routes.edit(type, id)) },
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.LongType },
                ),
            ) { entry ->
                ContentEditScreen(
                    typeSlug = entry.arguments?.getString("type") ?: "page",
                    id = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                    snackbar = snackbar,
                    onClose = { nav.popBackStack() },
                )
            }
            composable(
                Routes.MEDIA,
                arguments = listOf(navArgument("upload") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                MediaScreen(snackbar = snackbar, startUpload = entry.arguments?.getBoolean("upload") == true)
            }
            composable(Routes.MORE) {
                MoreScreen(
                    snackbar = snackbar,
                    openList = { type -> nav.navigate(Routes.content(type)) },
                    onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
                )
            }
        }
    }
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
