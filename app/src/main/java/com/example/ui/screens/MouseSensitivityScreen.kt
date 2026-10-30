package com.example.ui.screens

import android.view.KeyEvent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSwipeConfig
import com.example.ui.components.GamingCard
import com.example.ui.components.KeyBadge
import com.example.ui.components.NeonButton
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun MouseSensitivityScreen(
    cameraConfig: CameraSwipeConfig,
    isMouseLocked: Boolean,
    onToggleMouseLock: () -> Unit,
    onUpdateCameraConfig: (CameraSwipeConfig) -> Unit
) {
    val scrollState = rememberScrollState()

    var sensX by remember(cameraConfig.sensitivityX) { mutableFloatStateOf(cameraConfig.sensitivityX) }
    var sensY by remember(cameraConfig.sensitivityY) { mutableFloatStateOf(cameraConfig.sensitivityY) }
    var deadzone by remember(cameraConfig.deadzone) { mutableFloatStateOf(cameraConfig.deadzone) }
    var smoothing by remember(cameraConfig.smoothingFactor) { mutableFloatStateOf(cameraConfig.smoothingFactor) }
    var invertY by remember(cameraConfig.invertY) { mutableStateOf(cameraConfig.invertY) }

    val hotkeyOptions = listOf(
        Pair(KeyEvent.KEYCODE_GRAVE, "~ (Tilde)"),
        Pair(KeyEvent.KEYCODE_F1, "F1"),
        Pair(KeyEvent.KEYCODE_ALT_LEFT, "Alt"),
        Pair(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl"),
        Pair(KeyEvent.KEYCODE_TAB, "Tab")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Mouse Lock Status Card
        GamingCard(
            borderColor = if (isMouseLocked) NeonGreen else CyberAmber.copy(alpha = 0.5f),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMouseLocked) NeonGreen.copy(alpha = 0.2f) else CyberAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = null,
                                tint = if (isMouseLocked) NeonGreen else CyberAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FPS Mouse-to-Swipe Engine",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isMouseLocked) "Mouse cursor captured & rotating camera" else "Mouse cursor visible (Windows/Android mode)",
                                fontSize = 12.sp,
                                color = if (isMouseLocked) NeonGreen else TextSecondary
                            )
                        }
                    }

                    NeonButton(
                        text = if (isMouseLocked) "Unlock" else "Lock Mouse",
                        onClick = onToggleMouseLock,
                        isPrimary = !isMouseLocked
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mouse Lock Hotkey Selector
                Text(
                    text = "Mouse Lock Toggle Hotkey:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotkeyOptions.forEach { (keyCode, label) ->
                        val isSelected = cameraConfig.lockToggleKeyCode == keyCode
                        KeyBadge(
                            keyLabel = label,
                            color = if (isSelected) CyberCyan else TextTertiary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onUpdateCameraConfig(
                                    cameraConfig.copy(
                                        lockToggleKeyCode = keyCode,
                                        lockToggleKeyLabel = label
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Sensitivity Sliders Card
        GamingCard(borderColor = CyberCyan.copy(alpha = 0.4f)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sensitivity & Calibration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal (X) Sensitivity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Horizontal Sensitivity (X)", fontSize = 13.sp, color = TextPrimary)
                    Text(
                        "${String.format("%.2f", sensX)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Slider(
                    value = sensX,
                    onValueChange = {
                        sensX = it
                        onUpdateCameraConfig(cameraConfig.copy(sensitivityX = it))
                    },
                    valueRange = 0.2f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vertical (Y) Sensitivity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vertical Sensitivity (Y)", fontSize = 13.sp, color = TextPrimary)
                    Text(
                        "${String.format("%.2f", sensY)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Slider(
                    value = sensY,
                    onValueChange = {
                        sensY = it
                        onUpdateCameraConfig(cameraConfig.copy(sensitivityY = it))
                    },
                    valueRange = 0.2f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Invert Y Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invert Vertical Pitch (Y-Axis)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Invert mouse up/down camera direction", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = invertY,
                        onCheckedChange = {
                            invertY = it
                            onUpdateCameraConfig(cameraConfig.copy(invertY = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.4f)
                        )
                    )
                }

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Deadzone Filter Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Deadzone Filter", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Filters out micro-movements to eliminate sensor noise & aim jitter", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(
                        "${String.format("%.1f", deadzone)} px",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber
                    )
                }
                Slider(
                    value = deadzone,
                    onValueChange = {
                        deadzone = it
                        onUpdateCameraConfig(cameraConfig.copy(deadzone = it))
                    },
                    valueRange = 0.0f..4.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Smoothing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Movement Smoothing", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Stabilizes rapid mouse swipes for smooth 60fps tracking", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(
                        "${(smoothing * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
                Slider(
                    value = smoothing,
                    onValueChange = {
                        smoothing = it
                        onUpdateCameraConfig(cameraConfig.copy(smoothingFactor = it))
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                )
            }
        }

        // Camera Swipe Zone Configuration Card
        GamingCard(borderColor = CyberAmber.copy(alpha = 0.3f)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Camera Swipe Zone (Right Screen Half)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "The touchscreen area where mouse deltas are dispatched as swipe gestures. When the virtual finger reaches the edge, it instantly re-centers so you can spin 360° continuously.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Left Bound: ${(cameraConfig.zoneLeft * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = cameraConfig.zoneLeft,
                            onValueChange = { onUpdateCameraConfig(cameraConfig.copy(zoneLeft = it)) },
                            valueRange = 0.30f..0.70f,
                            colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Right Bound: ${(cameraConfig.zoneRight * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = cameraConfig.zoneRight,
                            onValueChange = { onUpdateCameraConfig(cameraConfig.copy(zoneRight = it)) },
                            valueRange = 0.80f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                        )
                    }
                }
            }
        }
    }
}
