package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileRepository
import com.example.input.InputDeviceState
import com.example.model.MappingProfile
import com.example.service.KeymapperAccessibilityService
import com.example.service.KeymapperForegroundService
import com.example.ui.components.GamingCard
import com.example.ui.components.KeyBadge
import com.example.ui.components.NeonButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    activeProfile: MappingProfile,
    allProfiles: List<MappingProfile>,
    deviceState: InputDeviceState,
    isServiceRunning: Boolean,
    isAccessibilityConnected: Boolean,
    isMouseLocked: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onToggleMouseLock: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleOverlayMode: (Boolean) -> Unit,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val hasOverlayPermission = Settings.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Engine Banner Card
        GamingCard(
            borderColor = if (isServiceRunning) NeonGreen else CyberCyan.copy(alpha = 0.5f),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Apex Engine",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                label = if (isServiceRunning) "RUNNING" else "OFFLINE",
                                isActive = isServiceRunning
                            )
                        }
                        Text(
                            text = if (isServiceRunning)
                                "Background input bridge active. Keybinds & mouse look enabled."
                            else
                                "Start engine to map Bluetooth keyboard/mouse inputs for Free Fire.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    NeonButton(
                        text = if (isServiceRunning) "Stop" else "Start Engine",
                        onClick = {
                            if (isServiceRunning) onStopService() else onStartService()
                        },
                        isPrimary = !isServiceRunning,
                        testTag = "engine_toggle_btn"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mouse Lock / Unlock FPS View Switch
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleMouseLock() },
                    color = if (isMouseLocked) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceHighlight,
                    border = BorderStroke(1.dp, if (isMouseLocked) NeonGreen else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = "Mouse Lock",
                                tint = if (isMouseLocked) NeonGreen else CyberAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isMouseLocked) "FPS Mouse Look: ACTIVE" else "FPS Mouse Look: UNLOCKED",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMouseLocked) NeonGreen else TextPrimary
                                )
                                Text(
                                    text = "Hotkey: ${activeProfile.camera.lockToggleKeyLabel} to toggle in game",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        KeyBadge(
                            keyLabel = activeProfile.camera.lockToggleKeyLabel,
                            color = if (isMouseLocked) NeonGreen else CyberAmber
                        )
                    }
                }
            }
        }

        // Dual Interaction Modes Switch (Option A vs Option B)
        GamingCard(borderColor = CyberCyan.copy(alpha = 0.3f)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Interaction Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Choose how keymappings appear on your screen:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Option A: Direct Settings Mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleOverlayMode(false) },
                    color = if (!activeProfile.floatingOverlayEnabled) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (!activeProfile.floatingOverlayEnabled) CyberCyan else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyberCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Option A: Clean Direct Settings",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (!activeProfile.floatingOverlayEnabled) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SELECTED",
                                        color = CyberCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Zero floating buttons on screen. Inputs bridge silently in the background.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option B: Floating Overlay Mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!hasOverlayPermission) {
                                requestOverlayPermission(context)
                            } else {
                                onToggleOverlayMode(true)
                            }
                        },
                    color = if (activeProfile.floatingOverlayEnabled) NeonPurple.copy(alpha = 0.15f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (activeProfile.floatingOverlayEnabled) NeonPurple else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Option B: Floating Overlay HUD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (activeProfile.floatingOverlayEnabled) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SELECTED",
                                        color = NeonPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Floating draggable key pins over Free Fire to align keys directly on game buttons.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Hardware & Permission Status
        GamingCard {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "System & Hardware Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Accessibility Status Row
                StatusRow(
                    title = "Accessibility Gesture Engine",
                    subtitle = if (isAccessibilityConnected) "Active & Ready for touch injection" else "Required to inject touch & keypresses",
                    isGood = isAccessibilityConnected,
                    actionText = if (!isAccessibilityConnected) "Enable" else null,
                    onAction = { openAccessibilitySettings(context) }
                )

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Overlay Permission Row
                StatusRow(
                    title = "Draw Over Other Apps",
                    subtitle = if (hasOverlayPermission) "Granted for floating mapping HUD" else "Required for Option B visual overlay",
                    isGood = hasOverlayPermission,
                    actionText = if (!hasOverlayPermission) "Grant" else null,
                    onAction = { requestOverlayPermission(context) }
                )

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Physical Keyboard Row
                StatusRow(
                    title = "Physical Keyboard",
                    subtitle = if (deviceState.hasKeyboard) deviceState.keyboardName ?: "Bluetooth/USB Connected" else "No physical keyboard detected",
                    isGood = deviceState.hasKeyboard,
                    actionText = null,
                    onAction = {}
                )

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Physical Mouse Row
                StatusRow(
                    title = "Physical Mouse",
                    subtitle = if (deviceState.hasMouse) deviceState.mouseName ?: "Bluetooth/USB Connected" else "No physical mouse detected",
                    isGood = deviceState.hasMouse,
                    actionText = null,
                    onAction = {}
                )
            }
        }

        // Active Profile Selector
        var profileMenuExpanded by remember { mutableStateOf(false) }

        GamingCard {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Game Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = activeProfile.name,
                            fontSize = 13.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box {
                        NeonButton(
                            text = "Switch Preset",
                            onClick = { profileMenuExpanded = true },
                            isPrimary = false
                        )

                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            allProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(profile.description, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        onSelectProfile(profile.id)
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge("Keybinds", "${activeProfile.bindings.size}", CyberCyan, Modifier.weight(1f))
                    StatBadge("WASD Joystick", if (activeProfile.joystick.enabled) "Active" else "Off", NeonGreen, Modifier.weight(1f))
                    StatBadge("Mouse Sens X/Y", "${activeProfile.camera.sensitivityX}x / ${activeProfile.camera.sensitivityY}x", CyberAmber, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToTab(1) }, // Keybinds
                        color = DarkSurfaceHighlight,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Keybinds", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToTab(2) }, // Mouse
                        color = DarkSurfaceHighlight,
                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Mouse, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aim & Sens", color = CyberAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Launch Free Fire Card
        GamingCard(borderColor = CyberAmber.copy(alpha = 0.4f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(CyberAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Launch Free Fire",
                            tint = CyberAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Launch Free Fire",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Start engine, then launch game to play with mouse & keyboard",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                NeonButton(
                    text = "Launch",
                    onClick = { launchFreeFire(context) },
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String,
    isGood: Boolean,
    actionText: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGood) NeonGreen else CyberAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }

        if (actionText != null) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() },
                color = CyberCyan.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, CyberCyan)
            ) {
                Text(
                    text = actionText,
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        color = DarkSurfaceHighlight,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = TextTertiary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

private fun requestOverlayPermission(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        openAccessibilitySettings(context)
    }
}

private fun launchFreeFire(context: Context) {
    val pm = context.packageManager
    val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
    for (pkg in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            return
        }
    }
    // Fallback: open play store for Free Fire if not installed
    try {
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dts.freefireth"))
        storeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(storeIntent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.dts.freefireth"))
        webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(webIntent)
    }
}
