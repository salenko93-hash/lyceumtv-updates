package ua.edu.cunl.lyceummobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import ua.edu.cunl.lyceummobile.LyceumViewModel
import ua.edu.cunl.lyceummobile.ui.theme.Navy

private enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Головна", Icons.Default.Home),
    TIMETABLE("Розклад", Icons.Default.CalendarMonth),
    ANNOUNCEMENTS("Оголошення", Icons.Default.Campaign),
    MEMORY("Пам'ять", Icons.Default.LocalFireDepartment),
    SETTINGS("Налаштування", Icons.Default.Settings)
}

@Composable
fun LyceumApp(model: LyceumViewModel) {
    val state by model.state.collectAsState()

    when {
        model.isAlarm -> EmergencyScreen(model, state)
        model.isAllClear -> AllClearScreen()
        model.isSilence -> MinuteSilenceScreen(model, state, fullScreen = true)
        else -> {
            var selected by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
            val tab = AppTab.entries.firstOrNull { it.name == selected } ?: AppTab.HOME

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == tab,
                                onClick = { selected = item.name },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                },
                containerColor = Navy
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (tab) {
                        AppTab.HOME -> DashboardScreen(model, state)
                        AppTab.TIMETABLE -> TimetableScreen(model, state)
                        AppTab.ANNOUNCEMENTS -> AnnouncementsScreen(state)
                        AppTab.MEMORY -> MinuteSilenceScreen(
                            model, state, fullScreen = false
                        )
                        AppTab.SETTINGS -> SettingsScreen(model, state)
                    }
                }
            }
        }
    }
}
