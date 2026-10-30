package com.example.ui.screens

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GestureInjector
import com.example.engine.VisualTouchEvent
import com.example.model.MappingProfile
import com.example.service.KeymapperAccessibilityService
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
import kotlinx.coroutines.flow.collectLatest

data class SandboxLog(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isMouse: Boolean = false,
    val time: String
)

@Composable
fun TestSandboxScreen(
    profile: MappingProfile,
    isMouseLocked: Boolean,
    onToggleMouseLock: () -> Unit
) {
    val logs = remember { mutableStateListOf<SandboxLog>() }
    var eventCount by remember { mutableIntStateOf(0) }
    var lastDx by remember { mutableFloatStateOf(0f) }
    var lastDy by remember { mutableFloatStateOf(0f) }

    // Live visual ripple touches
    val touchPoints = remember { mutableStateListOf<VisualTouchEvent>() }

    // Listen to GestureInjector touch events
    LaunchedEffect(Unit) {
        GestureInjector.instance.visualTouchFlow.collectLatest { event ->
            touchPoints.add(event)
            if (touchPoints.size > 25) {
                touchPoints.removeAt(0)
            }
            eventCount++
            val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            val timeStr = sdf.format(java.util.Date(event.timestamp))
            val msg = if (event.isMouseSwipe) {
                "Mouse Swipe at (${event.x.toInt()}, ${event.y.toInt()})"
            } else {
                "Touch Gesture [${event.label ?: "Tap"}] at (${event.x.toInt()}, ${event.y.toInt()})"
            }
            logs.add(0, SandboxLog(message = msg, isMouse = event.isMouseSwipe, time = timeStr))
            if (logs.size > 50) logs.removeAt(logs.lastIndex)
        }
    }

    // Listen to key events from service
    LaunchedEffect(Unit) {
        KeymapperAccessibilityService.lastKeyEventFlow.collectLatest { keyMsg ->
            if (keyMsg != null) {
                val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date())
                logs.add(0, SandboxLog(message = keyMsg, isMouse = false, time = timeStr))
                if (logs.size > 50) logs.removeAt(logs.lastIndex)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Sandbox Status Bar
        GamingCard(borderColor = CyberCyan.copy(alpha = 0.5f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Live Calibration Sandbox", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Test Bluetooth keyboard & mouse gesture injection in real time", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(
                        text = if (isMouseLocked) "Unlock Mouse" else "Lock Mouse",
                        onClick = onToggleMouseLock,
                        isPrimary = !isMouseLocked
                    )
                }
            }
        }

        // Live Virtual Screen Touchpad (where taps and swipes visually appear)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF060910))
                .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                    val timeStr = sdf.format(java.util.Date())
                    logs.add(0, SandboxLog(message = "Local Key: ${keyEvent.key}", isMouse = false, time = timeStr))
                    true
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        lastDx = dragAmount.x
                        lastDy = dragAmount.y
                        GestureInjector.instance.injectMouseDelta(dragAmount.x, dragAmount.y, profile.camera)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val screenW = GestureInjector.instance.screenWidth.toFloat().coerceAtLeast(1f)
                val screenH = GestureInjector.instance.screenHeight.toFloat().coerceAtLeast(1f)

                // Draw center crosshair
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(canvasW / 2f, 0f),
                    end = Offset(canvasW / 2f, canvasH),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(0f, canvasH / 2f),
                    end = Offset(canvasW, canvasH / 2f),
                    strokeWidth = 1f
                )

                // Draw recent touch ripples
                touchPoints.takeLast(15).forEach { touch ->
                    val cx = (touch.x / screenW) * canvasW
                    val cy = (touch.y / screenH) * canvasH
                    val color = if (touch.isMouseSwipe) CyberAmber else CyberCyan

                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = 24f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = Offset(cx, cy)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "VIRTUAL TOUCH DISPLAY (MOVE MOUSE OR TAP HERE)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Text(
                    text = "Events: $eventCount | Delta: (${lastDx.toInt()}, ${lastDy.toInt()})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Live Event Log List
        GamingCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Input Log (${logs.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = { logs.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Logs", tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connect a Bluetooth keyboard/mouse and press any key or move the mouse to see live signals.",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceHighlight, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.message,
                                    fontSize = 12.sp,
                                    color = if (log.isMouse) CyberAmber else CyberCyan,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = log.time,
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
