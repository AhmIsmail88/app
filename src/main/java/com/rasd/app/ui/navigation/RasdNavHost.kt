package com.rasd.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rasd.app.R
import com.rasd.app.ui.screens.about.AboutScreen
import com.rasd.app.ui.screens.addPeriod.AddPeriodScreen
import com.rasd.app.ui.screens.assessment.AssessmentScreen
import com.rasd.app.ui.screens.attendance.AttendanceScreen
import com.rasd.app.ui.screens.behavior.BehaviorScreen
import com.rasd.app.ui.screens.classes.ClassListScreen
import com.rasd.app.ui.screens.dashboard.DashboardScreen
import com.rasd.app.ui.screens.reports.ReportsScreen
import com.rasd.app.ui.screens.schedule.ScheduleScreen
import com.rasd.app.ui.screens.settings.SettingsScreen
import com.rasd.app.ui.screens.students.StudentListScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val CLASSES = "classes"
    const val STUDENTS = "students"
    const val ATTENDANCE = "attendance"
    const val BEHAVIOR = "behavior"
    const val ASSESSMENT = "assessment"
    const val REPORTS = "reports"
    const val SCHEDULE = "schedule"
    const val ADD_PERIOD = "add_period"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopDest(val route: String, val label: Int, val icon: ImageVector)

private val topDestinations = listOf(
    TopDest(Routes.DASHBOARD, R.string.nav_home, Icons.Outlined.Home),
    TopDest(Routes.ATTENDANCE, R.string.nav_attendance, Icons.Outlined.CheckCircle),
    TopDest(Routes.STUDENTS, R.string.nav_students, Icons.Outlined.People),
    TopDest(Routes.REPORTS, R.string.nav_reports, Icons.Outlined.BarChart)
)

@Composable
private fun titleFor(route: String?): String = when (route) {
    Routes.CLASSES -> stringResource(R.string.title_classes)
    Routes.STUDENTS -> stringResource(R.string.title_students)
    Routes.ATTENDANCE -> stringResource(R.string.title_attendance)
    Routes.BEHAVIOR -> stringResource(R.string.title_behavior)
    Routes.ASSESSMENT -> stringResource(R.string.title_assessments)
    Routes.REPORTS -> stringResource(R.string.title_reports)
    Routes.SCHEDULE -> stringResource(R.string.title_schedule)
    Routes.ADD_PERIOD -> stringResource(R.string.title_add_period)
    Routes.SETTINGS -> stringResource(R.string.title_settings)
    Routes.ABOUT -> stringResource(R.string.title_about)
    else -> stringResource(R.string.app_name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RasdNavHost() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route
    val isTop = topDestinations.any { it.route == current }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(current), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (!isTop) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (current != Routes.SETTINGS) {
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.title_settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (isTop) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    topDestinations.forEach { dest ->
                        val label = stringResource(dest.label)
                        NavigationBarItem(
                            selected = current == dest.route,
                            onClick = { nav.navigateToTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(pad)
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(onNavigate = { nav.navigate(it) }) }
            composable(Routes.ATTENDANCE) { AttendanceScreen() }
            composable(Routes.BEHAVIOR) { BehaviorScreen() }
            composable(Routes.ASSESSMENT) { AssessmentScreen() }
            composable(Routes.CLASSES) { ClassListScreen() }
            composable(Routes.STUDENTS) { StudentListScreen() }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.SCHEDULE) { ScheduleScreen(onAddPeriod = { nav.navigate(Routes.ADD_PERIOD) }) }
            composable(Routes.ADD_PERIOD) { AddPeriodScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
