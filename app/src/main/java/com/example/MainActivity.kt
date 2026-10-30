package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProfileRepository
import com.example.input.InputDeviceManager
import com.example.service.KeymapperAccessibilityService
import com.example.service.KeymapperForegroundService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KeybindsEditorScreen
import com.example.ui.screens.MouseSensitivityScreen
import com.example.ui.screens.ProfileManagementScreen
import com.example.ui.screens.TestSandboxScreen
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private lateinit var repository: ProfileRepository
    private lateinit var inputDeviceManager: InputDeviceManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ProfileRepository.getInstance(applicationContext)
        inputDeviceManager = InputDeviceManager(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    repository = repository,
                    inputDeviceManager = inputDeviceManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inputDeviceManager.refreshDevices()
    }

    override fun onDestroy() {
        super.onDestroy()
        inputDeviceManager.release()
    }
}

@Composable
fun MainAppContent(
    repository: ProfileRepository,
    inputDeviceManager: InputDeviceManager
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val activeProfile by repository.activeProfile.collectAsStateWithLifecycle()
    val allProfiles by repository.profiles.collectAsStateWithLifecycle()
    val deviceState by inputDeviceManager.deviceState.collectAsStateWithLifecycle()

    val isServiceRunning by KeymapperForegroundService.isRunning.collectAsStateWithLifecycle()
    val isAccessibilityConnected by KeymapperAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
    val isMouseLocked by KeymapperAccessibilityService.isMouseLocked.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid),
        topBar = {
            TopAppBarHeader(
                profileName = activeProfile.name,
                isServiceRunning = isServiceRunning,
                isMouseLocked = isMouseLocked,
                hasKeyboard = deviceState.hasKeyboard,
                hasMouse = deviceState.hasMouse
            )
        },
        bottomBar = {
            ApexBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkVoid)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    activeProfile = activeProfile,
                    allProfiles = allProfiles,
                    deviceState = deviceState,
                    isServiceRunning = isServiceRunning,
                    isAccessibilityConnected = isAccessibilityConnected,
                    isMouseLocked = isMouseLocked,
                    onStartService = {
                        KeymapperForegroundService.startService(context)
                    },
                    onStopService = {
                        KeymapperForegroundService.stopService(context)
                    },
                    onToggleMouseLock = {
                        KeymapperAccessibilityService.toggleMouseLock()
                    },
                    onSelectProfile = { profileId ->
                        repository.setActiveProfile(profileId)
                    },
                    onToggleOverlayMode = { enabled ->
                        repository.toggleFloatingOverlay(enabled)
                    },
                    onNavigateToTab = { selectedTab = it }
                )
                1 -> KeybindsEditorScreen(
                    profile = activeProfile,
                    onUpdateBinding = { updated -> repository.updateBinding(updated) },
                    onDeleteBinding = { id -> repository.deleteBinding(id) },
                    onUpdateJoystick = { joy -> repository.updateJoystickConfig(joy) },
                    onResetDefaults = { repository.resetToDefaults() }
                )
                2 -> MouseSensitivityScreen(
                    cameraConfig = activeProfile.camera,
                    isMouseLocked = isMouseLocked,
                    onToggleMouseLock = { KeymapperAccessibilityService.toggleMouseLock() },
                    onUpdateCameraConfig = { config -> repository.updateCameraConfig(config) }
                )
                3 -> TestSandboxScreen(
                    profile = activeProfile,
                    isMouseLocked = isMouseLocked,
                    onToggleMouseLock = { KeymapperAccessibilityService.toggleMouseLock() }
                )
                4 -> ProfileManagementScreen(
                    activeProfile = activeProfile,
                    allProfiles = allProfiles,
                    onSelectProfile = { repository.setActiveProfile(it) },
                    onImportProfile = { jsonStr ->
                        val result = repository.importProfile(jsonStr)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Profile successfully imported!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to import JSON profile", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onResetDefaults = {
                        repository.resetToDefaults()
                        Toast.makeText(context, "Free Fire default profiles restored", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun TopAppBarHeader(
    profileName: String,
    isServiceRunning: Boolean,
    isMouseLocked: Boolean,
    hasKeyboard: Boolean,
    hasMouse: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "APEX",
                        color = CyberCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "MAPPER",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isServiceRunning) NeonGreen else NeonRed, CircleShape)
                    )
                }
                Text(
                    text = profileName,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Status badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HardwareBadge("KB", hasKeyboard, CyberCyan)
                HardwareBadge("MOUSE", hasMouse, CyberAmber)
                if (isMouseLocked) {
                    HardwareBadge("LOCKED", true, NeonGreen)
                }
            }
        }
    }
}

@Composable
fun HardwareBadge(label: String, isConnected: Boolean, activeColor: Color) {
    Surface(
        color = if (isConnected) activeColor.copy(alpha = 0.15f) else DarkSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(if (isConnected) activeColor else TextTertiary, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) activeColor else TextTertiary
            )
        }
    }
}

@Composable
fun ApexBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Pair("Engine", Icons.Default.Dashboard),
        Pair("Keybinds", Icons.Default.Gamepad),
        Pair("Aim/Sens", Icons.Default.Mouse),
        Pair("Sandbox", Icons.Default.BugReport),
        Pair("Profiles", Icons.Default.Settings)
    )

    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = DarkSurfaceElevated,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkVoid,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
