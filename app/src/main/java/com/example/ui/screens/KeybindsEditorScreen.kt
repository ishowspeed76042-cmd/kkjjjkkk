package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileRepository
import com.example.model.ActionType
import com.example.model.BindingCategory
import com.example.model.JoystickConfig
import com.example.model.KeyBinding
import com.example.model.MappingProfile
import com.example.ui.components.GamingCard
import com.example.ui.components.KeyBadge
import com.example.ui.components.NeonButton
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.roundToInt

@Composable
fun KeybindsEditorScreen(
    profile: MappingProfile,
    onUpdateBinding: (KeyBinding) -> Unit,
    onDeleteBinding: (String) -> Unit,
    onUpdateJoystick: (JoystickConfig) -> Unit,
    onUpdateGlobalScale: (Float) -> Unit,
    onResetDefaults: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("All", "Combat", "Movement", "Tactical", "Inventory", "HUD Preview")

    var editingBinding by remember { mutableStateOf<KeyBinding?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = DarkSurfaceElevated,
            contentColor = CyberCyan,
            edgePadding = 8.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategoryIndex == index) CyberCyan else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedCategoryIndex == 5) {
            // Interactive In-App Screen HUD Preview
            InteractiveHudPreview(
                profile = profile,
                onSelectBinding = { editingBinding = it },
                onUpdateBindingPosition = { binding, nx, ny ->
                    onUpdateBinding(binding.copy(targetX = nx, targetY = ny))
                },
                onUpdateJoystickPosition = { nx, ny ->
                    onUpdateJoystick(profile.joystick.copy(centerX = nx, centerY = ny))
                }
            )
        } else {
            // Global Button Size Scale Card
            GamingCard(borderColor = CyberCyan.copy(alpha = 0.35f)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Button Size Scale (छोटे-बड़े बटन्स)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Enlarge or shrink all on-screen mapping pins", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text(
                            "${(profile.globalButtonScale * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Slider(
                        value = profile.globalButtonScale,
                        onValueChange = { onUpdateGlobalScale(it) },
                        valueRange = 0.6f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header with Add Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keybindings (${profile.bindings.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onResetDefaults) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Defaults", tint = TextSecondary)
                    }

                    NeonButton(
                        text = "+ Add Key",
                        onClick = { isAddingNew = true },
                        isPrimary = true,
                        testTag = "add_keybind_btn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter bindings
            val filteredBindings = remember(profile.bindings, selectedCategoryIndex) {
                when (selectedCategoryIndex) {
                    1 -> profile.bindings.filter { it.category == BindingCategory.COMBAT }
                    2 -> profile.bindings.filter { it.category == BindingCategory.MOVEMENT }
                    3 -> profile.bindings.filter { it.category == BindingCategory.TACTICAL }
                    4 -> profile.bindings.filter { it.category == BindingCategory.INVENTORY }
                    else -> profile.bindings
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Joystick Config Card (if on All or Movement)
                if (selectedCategoryIndex == 0 || selectedCategoryIndex == 2) {
                    item {
                        JoystickConfigCard(
                            joystick = profile.joystick,
                            onUpdate = onUpdateJoystick
                        )
                    }
                }

                items(filteredBindings, key = { it.id }) { binding ->
                    BindingRowCard(
                        binding = binding,
                        globalScale = profile.globalButtonScale,
                        onEdit = { editingBinding = binding },
                        onDelete = { onDeleteBinding(binding.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Edit Binding Dialog
    editingBinding?.let { binding ->
        EditBindingDialog(
            binding = binding,
            onDismiss = { editingBinding = null },
            onSave = { updated ->
                onUpdateBinding(updated)
                editingBinding = null
            }
        )
    }

    // Add New Binding Dialog
    if (isAddingNew) {
        EditBindingDialog(
            binding = KeyBinding(
                id = "custom_${System.currentTimeMillis()}",
                name = "New Action",
                keyCode = KeyEvent.KEYCODE_T,
                keyLabel = "T",
                targetX = 0.50f,
                targetY = 0.50f,
                actionType = ActionType.TAP,
                category = BindingCategory.TACTICAL,
                size = 1.0f
            ),
            isNew = true,
            onDismiss = { isAddingNew = false },
            onSave = { newBinding ->
                onUpdateBinding(newBinding)
                isAddingNew = false
            }
        )
    }
}

@Composable
fun InteractiveHudPreview(
    profile: MappingProfile,
    onSelectBinding: (KeyBinding) -> Unit,
    onUpdateBindingPosition: (KeyBinding, Float, Float) -> Unit,
    onUpdateJoystickPosition: (Float, Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Interactive HUD Layout (Free Fire)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Drag keys on the screen to align with your in-game layout. Tap a key to rebind or resize.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Screen simulation canvas (Landscape 16:9 ratio typical for mobile gaming)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF070A10))
                .border(BorderStroke(2.dp, CyberCyan.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Camera Swipe Zone visual indicator
                val cam = profile.camera
                val camLeft = cam.zoneLeft * widthPx
                val camTop = cam.zoneTop * heightPx
                val camW = (cam.zoneRight - cam.zoneLeft) * widthPx
                val camH = (cam.zoneBottom - cam.zoneTop) * heightPx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(camLeft.roundToInt(), camTop.roundToInt()) }
                        .size((camW / 1.dp.value).dp, (camH / 1.dp.value).dp)
                        .border(BorderStroke(1.dp, CyberAmber.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                        .background(CyberAmber.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = "Mouse Look Zone",
                        color = CyberAmber.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Joystick Pin (WASD)
                val joy = profile.joystick
                if (joy.enabled) {
                    var joyOffsetX by remember(joy.centerX) { mutableFloatStateOf(joy.centerX * widthPx) }
                    var joyOffsetY by remember(joy.centerY) { mutableFloatStateOf(joy.centerY * heightPx) }
                    val joySizeDp = (56 * profile.globalButtonScale).dp

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((joyOffsetX - joySizeDp.value / 2).roundToInt(), (joyOffsetY - joySizeDp.value / 2).roundToInt()) }
                            .size(joySizeDp)
                            .background(CyberCyan.copy(alpha = 0.25f), CircleShape)
                            .border(BorderStroke(2.dp, CyberCyan), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        onUpdateJoystickPosition(
                                            (joyOffsetX / widthPx).coerceIn(0f, 1f),
                                            (joyOffsetY / heightPx).coerceIn(0f, 1f)
                                        )
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    joyOffsetX = (joyOffsetX + dragAmount.x).coerceIn(30f, widthPx - 30f)
                                    joyOffsetY = (joyOffsetY + dragAmount.y).coerceIn(30f, heightPx - 30f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("WASD", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Draggable key badges with dynamic size
                profile.bindings.forEach { binding ->
                    var pinX by remember(binding.targetX) { mutableFloatStateOf(binding.targetX * widthPx) }
                    var pinY by remember(binding.targetY) { mutableFloatStateOf(binding.targetY * heightPx) }

                    val pinColor = when (binding.id) {
                        "fire" -> NeonRed
                        "aim" -> CyberAmber
                        "gloo_wall" -> NeonGreen
                        "skill" -> NeonPurple
                        else -> CyberCyan
                    }

                    val pinSizeDp = (40 * binding.size * profile.globalButtonScale).dp.coerceIn(24.dp, 80.dp)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((pinX - pinSizeDp.value / 2).roundToInt(), (pinY - pinSizeDp.value / 2).roundToInt()) }
                            .size(pinSizeDp)
                            .background(DarkSurfaceElevated.copy(alpha = 0.85f), CircleShape)
                            .border(BorderStroke(1.5.dp, pinColor), CircleShape)
                            .clickable { onSelectBinding(binding) }
                            .pointerInput(binding.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        onUpdateBindingPosition(
                                            binding,
                                            (pinX / widthPx).coerceIn(0f, 1f),
                                            (pinY / heightPx).coerceIn(0f, 1f)
                                        )
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    pinX = (pinX + dragAmount.x).coerceIn(20f, widthPx - 20f)
                                    pinY = (pinY + dragAmount.y).coerceIn(20f, heightPx - 20f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = binding.keyLabel,
                            color = pinColor,
                            fontSize = (11f * binding.size * profile.globalButtonScale).coerceIn(8f, 16f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BindingRowCard(
    binding: KeyBinding,
    globalScale: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GamingCard(
        borderColor = when (binding.category) {
            BindingCategory.COMBAT -> NeonRed.copy(alpha = 0.4f)
            BindingCategory.MOVEMENT -> CyberCyan.copy(alpha = 0.4f)
            BindingCategory.TACTICAL -> CyberAmber.copy(alpha = 0.4f)
            BindingCategory.INVENTORY -> NeonPurple.copy(alpha = 0.4f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyBadge(
                    keyLabel = binding.keyLabel,
                    color = when (binding.category) {
                        BindingCategory.COMBAT -> NeonRed
                        BindingCategory.MOVEMENT -> CyberCyan
                        BindingCategory.TACTICAL -> CyberAmber
                        BindingCategory.INVENTORY -> NeonPurple
                    },
                    onClick = onEdit
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = binding.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Size: ${(binding.size * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = CyberAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Pos: (${(binding.targetX * 100).toInt()}%, ${(binding.targetY * 100).toInt()}%) • ${binding.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyberCyan, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun JoystickConfigCard(
    joystick: JoystickConfig,
    onUpdate: (JoystickConfig) -> Unit
) {
    GamingCard(borderColor = CyberCyan.copy(alpha = 0.5f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gamepad, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("WASD Movement Joystick (Free Fire Drag)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Smooth touch drag outward so character walks & sprints without stopping", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyBadge("W (Up)", color = CyberCyan, modifier = Modifier.weight(1f))
                KeyBadge("A (Left)", color = CyberCyan, modifier = Modifier.weight(1f))
                KeyBadge("S (Down)", color = CyberCyan, modifier = Modifier.weight(1f))
                KeyBadge("D (Right)", color = CyberCyan, modifier = Modifier.weight(1f))
                KeyBadge("Shift", color = CyberAmber, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Drag Distance / Sprint Reach Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Drag Distance / Sprint Reach (हल्का सा ड्रैग)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Increases drag reach to activate full walk / sprint in Free Fire", fontSize = 10.sp, color = TextSecondary)
                }
                Text("${(joystick.dragDistance * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
            Slider(
                value = joystick.dragDistance,
                onValueChange = { onUpdate(joystick.copy(dragDistance = it)) },
                valueRange = 0.08f..0.25f,
                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Center X Slider
            Text(
                text = "Center X: ${(joystick.centerX * 100).toInt()}%",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Slider(
                value = joystick.centerX,
                onValueChange = { onUpdate(joystick.copy(centerX = it)) },
                valueRange = 0.05f..0.50f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )

            // Center Y Slider
            Text(
                text = "Center Y: ${(joystick.centerY * 100).toInt()}%",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Slider(
                value = joystick.centerY,
                onValueChange = { onUpdate(joystick.copy(centerY = it)) },
                valueRange = 0.40f..0.95f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
        }
    }
}

@Composable
fun EditBindingDialog(
    binding: KeyBinding,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (KeyBinding) -> Unit
) {
    var name by remember { mutableStateOf(binding.name) }
    var keyLabel by remember { mutableStateOf(binding.keyLabel) }
    var keyCode by remember { mutableIntStateOf(binding.keyCode) }
    var targetX by remember { mutableFloatStateOf(binding.targetX) }
    var targetY by remember { mutableFloatStateOf(binding.targetY) }
    var size by remember { mutableFloatStateOf(binding.size) }
    var category by remember { mutableStateOf(binding.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text(
                text = if (isNew) "Add Keybinding" else "Edit Key: ${binding.name}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Action Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = keyLabel,
                        onValueChange = {
                            keyLabel = it.take(6)
                            if (it.isNotEmpty()) {
                                val char = it.first().uppercaseChar()
                                keyCode = when (char) {
                                    in 'A'..'Z' -> KeyEvent.KEYCODE_A + (char - 'A')
                                    in '0'..'9' -> KeyEvent.KEYCODE_0 + (char - '0')
                                    ' ' -> KeyEvent.KEYCODE_SPACE
                                    else -> keyCode
                                }
                            }
                        },
                        label = { Text("Key Label", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    // Quick mouse button selectors
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mouse Click:", fontSize = 11.sp, color = TextTertiary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            KeyBadge(
                                "L-Click",
                                color = NeonRed,
                                onClick = {
                                    keyLabel = "L-Click"
                                    keyCode = ProfileRepository.KEYCODE_MOUSE_LEFT
                                }
                            )
                            KeyBadge(
                                "R-Click",
                                color = CyberAmber,
                                onClick = {
                                    keyLabel = "R-Click"
                                    keyCode = ProfileRepository.KEYCODE_MOUSE_RIGHT
                                }
                            )
                        }
                    }
                }

                // Button Size Scale Slider (बटन्स के साइज छोटे-बड़े)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Button Size Scale (बटन साइज)", fontSize = 12.sp, color = TextSecondary)
                    Text("${(size * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                }
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 0.6f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                )

                // Coordinate Sliders
                Text("Target X: ${(targetX * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = targetX,
                    onValueChange = { targetX = it },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Text("Target Y: ${(targetY * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = targetY,
                    onValueChange = { targetY = it },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )
            }
        },
        confirmButton = {
            NeonButton(
                text = "Save Binding",
                onClick = {
                    onSave(
                        binding.copy(
                            name = name.ifBlank { "Custom Action" },
                            keyLabel = keyLabel.ifBlank { "?" },
                            keyCode = keyCode,
                            targetX = targetX,
                            targetY = targetY,
                            size = size,
                            category = category
                        )
                    )
                },
                isPrimary = true
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
