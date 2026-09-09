package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.LocationEntity
import com.example.services.MosqueSilentMonitorService
import com.example.ui.screens.AddEditLocationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.LocationViewModel

enum class AppDestination(val title: String, val icon: @Composable () -> Unit) {
    HOME("Home", { Icon(Icons.Default.Home, contentDescription = "Home") }),
    LOCATIONS("Locations", { Icon(Icons.Default.LocationOn, contentDescription = "Locations") }),
    SCHEDULES("Schedules", { Icon(Icons.Default.Schedule, contentDescription = "Schedules") }),
    PRAYERS("Prayers", { Icon(Icons.Default.CalendarMonth, contentDescription = "Prayers") }),
    SETTINGS("Settings", { Icon(Icons.Default.Settings, contentDescription = "Settings") })
}

class MainActivity : ComponentActivity() {

    private val locationViewModel: LocationViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PrayerSilentApp(
                    homeViewModel = homeViewModel,
                    locationViewModel = locationViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSilentApp(
    homeViewModel: HomeViewModel,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var editingLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var isAddingLocation by remember { mutableStateOf(false) }

    // Request necessary runtime permissions on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            locationViewModel.refreshCurrentGps()
            MosqueSilentMonitorService.start(context)
        }
    }

    LaunchedEffect(Unit) {
        val permsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permsToRequest.toTypedArray())
    }

    if (isAddingLocation || editingLocation != null) {
        AddEditLocationScreen(
            locationViewModel = locationViewModel,
            existingLocation = editingLocation,
            onNavigateBack = {
                isAddingLocation = false
                editingLocation = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    AppDestination.values().forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = destination.icon,
                            label = { Text(destination.title) },
                            modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_navigation"
                ) { destination ->
                    when (destination) {
                        AppDestination.HOME -> HomeScreen(
                            homeViewModel = homeViewModel,
                            locationViewModel = locationViewModel,
                            onNavigateToLocations = { currentDestination = AppDestination.LOCATIONS },
                            onNavigateToAddLocation = { isAddingLocation = true },
                            onNavigateToSchedules = { currentDestination = AppDestination.SCHEDULES },
                            onNavigateToPrayerTimes = { currentDestination = AppDestination.PRAYERS }
                        )
                        AppDestination.LOCATIONS -> LocationsScreen(
                            locationViewModel = locationViewModel,
                            onNavigateToAddLocation = { isAddingLocation = true },
                            onNavigateToEditLocation = { location -> editingLocation = location }
                        )
                        AppDestination.SCHEDULES -> SchedulesScreen()
                        AppDestination.PRAYERS -> PrayerTimesScreen(homeViewModel = homeViewModel)
                        AppDestination.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }
}
