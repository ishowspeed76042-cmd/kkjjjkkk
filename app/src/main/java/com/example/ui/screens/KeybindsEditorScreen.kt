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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
    onResetDefaults: () -> Unit,
    onClearAllBindings: () -> Unit = {},
    onUpdateBindingSize: (String, Float) -> Unit = { _, _ -> },
    onLoadPresets: () -> Unit = {}
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("All", "Combat", "Movement", "Tactical", "Inventory", "HUD Preview")

    var editingBinding by remember { mutableStateOf<KeyBinding?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(10.dp))

        // Local Storage Sync Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Auto-Saved to Phone Storage",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${profile.bindings.size} custom keys",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedCategoryIndex == 5) {
            // Interactive Screen HUD Preview
            InteractiveHudPreview(
                profile = profile,
                onSelectBinding = { editingBinding = it },
                onUpdateBindingPosition = { binding, nx, ny ->
                    onUpdateBinding(binding.copy(targetX = nx, targetY = ny))
                },
                onUpdateJoystickPosition = { nx, ny ->
                    onUpdateJoystick(profile.joystick.copy(centerX = nx, centerY = ny))
                },
                onUpdateBindingSize = onUpdateBindingSize,
                onDeleteBinding = onDeleteBinding
            )
        } else {
            // Individual Sizing & Layout Guide Banner
            GamingCard(borderColor = CyberCyan.copy(alpha = 0.35f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🎯 Individual Key Resizing (हर बटन का अलग साइज)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Each button has its own independent ➖ and ➕ controls. Adjusting a button's size affects ONLY that button. Use '+ Add Key' to capture any key from your keyboard.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Bar: [+ Add Key], [Presets], [Clear All]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keybindings (${profile.bindings.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Clear all button (for clean start)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showClearConfirmDialog = true },
                        color = DarkSurfaceHighlight,
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Clear", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Presets button (Load 17 standard Free Fire keys)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLoadPresets() },
                        color = DarkSurfaceHighlight,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Presets", tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Presets", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // + Add Key Button
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

                if (filteredBindings.isEmpty()) {
                    item {
                        GamingCard(borderColor = DarkBorder) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✨ Clean Layout (No Buttons)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Screen is clear of unwanted buttons. Tap '+ Add Key' to add only the buttons you need.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                NeonButton(
                                    text = "+ Add Your First Key",
                                    onClick = { isAddingNew = true },
                                    isPrimary = true
                                )
                            }
                        }
                    }
                } else {
                    items(filteredBindings, key = { it.id }) { binding ->
                        BindingRowCard(
                            binding = binding,
                            globalScale = profile.globalButtonScale,
                            onEdit = { editingBinding = binding },
                            onDelete = { onDeleteBinding(binding.id) },
                            onSizeDelta = { delta ->
                                onUpdateBindingSize(binding.id, delta)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Clear All Buttons?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will remove all keybindings from this profile so you can build your custom layout from a clean, blank screen.\n\nYou can always restore default buttons via the Refresh button.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                NeonButton(
                    text = "Yes, Clear All",
                    onClick = {
                        onClearAllBindings()
                        showClearConfirmDialog = false
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
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
    onUpdateJoystickPosition: (Float, Float) -> Unit,
    onUpdateBindingSize: (String, Float) -> Unit,
    onDeleteBinding: (String) -> Unit
) {
    var selectedPinId by remember { mutableStateOf<String?>(null) }
    val selectedBinding = profile.bindings.find { it.id == selectedPinId }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Interactive HUD Layout (Free Fire)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Tap any pin to resize or delete. Drag to position directly over Free Fire controls.",
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

                // Draggable key badges with individual sizing
                profile.bindings.forEach { binding ->
                    var pinX by remember(binding.targetX) { mutableFloatStateOf(binding.targetX * widthPx) }
                    var pinY by remember(binding.targetY) { mutableFloatStateOf(binding.targetY * heightPx) }
                    val isSelected = (binding.id == selectedPinId)

                    val pinColor = when {
                        isSelected -> CyberAmber
                        binding.id == "fire" -> NeonRed
                        binding.id == "aim" -> CyberAmber
                        binding.id == "gloo_wall" -> NeonGreen
                        binding.id == "skill" -> NeonPurple
                        else -> CyberCyan
                    }

                    val pinSizeDp = (44 * binding.size).dp.coerceIn(24.dp, 88.dp)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((pinX - pinSizeDp.value / 2).roundToInt(), (pinY - pinSizeDp.value / 2).roundToInt()) }
                            .size(pinSizeDp)
                            .background(if (isSelected) DarkSurfaceHighlight else DarkSurfaceElevated.copy(alpha = 0.85f), CircleShape)
                            .border(BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, pinColor), CircleShape)
                            .clickable { selectedPinId = binding.id }
                            .pointerInput(binding.id) {
                                detectDragGestures(
                                    onDragStart = { selectedPinId = binding.id },
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
                            fontSize = (11f * binding.size).coerceIn(8f, 16f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Pin Toolbar (Individual Resizing & Actions)
        if (selectedBinding != null) {
            GamingCard(borderColor = CyberAmber.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedBinding.name} (${selectedBinding.keyLabel})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Size: ${(selectedBinding.size * 100).toInt()}% • Pos: (${(selectedBinding.targetX * 100).toInt()}%, ${(selectedBinding.targetY * 100).toInt()}%)",
                            fontSize = 11.sp,
                            color = CyberAmber
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Smaller
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateBindingSize(selectedBinding.id, -0.15f) },
                            color = DarkSurfaceHighlight
                        ) {
                            Text("➖", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp)
                        }

                        // Bigger
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateBindingSize(selectedBinding.id, 0.15f) },
                            color = DarkSurfaceHighlight
                        ) {
                            Text("➕", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp)
                        }

                        // Edit
                        IconButton(
                            onClick = { onSelectBinding(selectedBinding) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }

                        // Delete
                        IconButton(
                            onClick = {
                                onDeleteBinding(selectedBinding.id)
                                selectedPinId = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        }
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
    onDelete: () -> Unit,
    onSizeDelta: (Float) -> Unit
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
                .padding(12.dp),
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

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = binding.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Individual Size Controls directly on the item
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Size: ${(binding.size * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = CyberAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Quick decrement button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSizeDelta(-0.15f) },
                            color = DarkSurfaceHighlight
                        ) {
                            Text("➖", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Quick increment button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSizeDelta(0.15f) },
                            color = DarkSurfaceHighlight
                        ) {
                            Text("➕", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = "Pos: (${(binding.targetX * 100).toInt()}%, ${(binding.targetY * 100).toInt()}%) • ${binding.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
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

data class KeyPreset(
    val name: String,
    val keyLabel: String,
    val keyCode: Int,
    val category: BindingCategory
)

private fun formatKeyLabel(keyCode: Int): String {
    return when (keyCode) {
        KeyEvent.KEYCODE_SPACE -> "Space"
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift"
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl"
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "Alt"
        KeyEvent.KEYCODE_TAB -> "Tab"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        KeyEvent.KEYCODE_ESCAPE -> "Esc"
        KeyEvent.KEYCODE_DEL -> "Backspace"
        KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
        KeyEvent.KEYCODE_GRAVE -> "~"
        ProfileRepository.KEYCODE_MOUSE_LEFT -> "L-Click"
        ProfileRepository.KEYCODE_MOUSE_RIGHT -> "R-Click"
        ProfileRepository.KEYCODE_MOUSE_MIDDLE -> "M-Click"
        ProfileRepository.KEYCODE_MOUSE_BACK -> "Mouse Back"
        ProfileRepository.KEYCODE_MOUSE_FORWARD -> "Mouse Fwd"
        else -> {
            val str = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            if (str.length == 1) str else str.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

private fun suggestActionName(keyCode: Int, keyLabel: String): Pair<String, BindingCategory> {
    return when (keyCode) {
        KeyEvent.KEYCODE_SPACE -> "Jump" to BindingCategory.MOVEMENT
        KeyEvent.KEYCODE_C -> "Crouch" to BindingCategory.MOVEMENT
        KeyEvent.KEYCODE_Z -> "Prone" to BindingCategory.MOVEMENT
        KeyEvent.KEYCODE_R -> "Reload" to BindingCategory.COMBAT
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "Sprint / Rush" to BindingCategory.MOVEMENT
        KeyEvent.KEYCODE_E -> "Active Skill" to BindingCategory.TACTICAL
        KeyEvent.KEYCODE_G -> "Gloo Wall" to BindingCategory.TACTICAL
        KeyEvent.KEYCODE_F -> "Quick Loot / Revive" to BindingCategory.TACTICAL
        KeyEvent.KEYCODE_TAB -> "Open Backpack" to BindingCategory.INVENTORY
        KeyEvent.KEYCODE_1 -> "Primary Weapon" to BindingCategory.COMBAT
        KeyEvent.KEYCODE_2 -> "Secondary Weapon" to BindingCategory.COMBAT
        KeyEvent.KEYCODE_3 -> "Melee / Pistol" to BindingCategory.COMBAT
        KeyEvent.KEYCODE_4 -> "Medkit" to BindingCategory.INVENTORY
        KeyEvent.KEYCODE_V -> "Enter Vehicle / Drive" to BindingCategory.MOVEMENT
        KeyEvent.KEYCODE_B -> "Shop / Buy Gun" to BindingCategory.TACTICAL
        KeyEvent.KEYCODE_M -> "Tactical Map" to BindingCategory.TACTICAL
        ProfileRepository.KEYCODE_MOUSE_LEFT -> "Shoot / Fire" to BindingCategory.COMBAT
        ProfileRepository.KEYCODE_MOUSE_RIGHT -> "Aim / ADS" to BindingCategory.COMBAT
        ProfileRepository.KEYCODE_MOUSE_MIDDLE -> "Ping / Mark" to BindingCategory.TACTICAL
        else -> "Key $keyLabel" to BindingCategory.TACTICAL
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
    var isListeningForKey by remember { mutableStateOf(isNew) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isListeningForKey) {
        if (isListeningForKey) {
            focusRequester.requestFocus()
        }
    }

    val quickPresets = listOf(
        KeyPreset("Jump", "Space", KeyEvent.KEYCODE_SPACE, BindingCategory.MOVEMENT),
        KeyPreset("Fire", "L-Click", ProfileRepository.KEYCODE_MOUSE_LEFT, BindingCategory.COMBAT),
        KeyPreset("Aim", "R-Click", ProfileRepository.KEYCODE_MOUSE_RIGHT, BindingCategory.COMBAT),
        KeyPreset("Gloo Wall", "G", KeyEvent.KEYCODE_G, BindingCategory.TACTICAL),
        KeyPreset("Skill", "E", KeyEvent.KEYCODE_E, BindingCategory.TACTICAL),
        KeyPreset("Crouch", "C", KeyEvent.KEYCODE_C, BindingCategory.MOVEMENT),
        KeyPreset("Prone", "Z", KeyEvent.KEYCODE_Z, BindingCategory.MOVEMENT),
        KeyPreset("Reload", "R", KeyEvent.KEYCODE_R, BindingCategory.COMBAT),
        KeyPreset("Medkit", "4", KeyEvent.KEYCODE_4, BindingCategory.INVENTORY),
        KeyPreset("Gun 1", "1", KeyEvent.KEYCODE_1, BindingCategory.COMBAT),
        KeyPreset("Gun 2", "2", KeyEvent.KEYCODE_2, BindingCategory.COMBAT),
        KeyPreset("Backpack", "Tab", KeyEvent.KEYCODE_TAB, BindingCategory.INVENTORY)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text(
                text = if (isNew) "Add Custom Key (+ Button)" else "Edit Key: ${binding.name}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Hardware Key Listener Box
                if (isListeningForKey) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(BorderStroke(2.dp, CyberCyan), RoundedCornerShape(12.dp))
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    val code = event.nativeKeyEvent.keyCode
                                    keyCode = code
                                    keyLabel = formatKeyLabel(code)
                                    val suggestion = suggestActionName(code, keyLabel)
                                    name = suggestion.first
                                    category = suggestion.second
                                    isListeningForKey = false
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable { focusRequester.requestFocus() }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "⌨️ PRESS ANY KEY ON KEYBOARD NOW...",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "कीबोर्ड का कोई भी बटन दबाएं (Space, Shift, E, R, 1, 2, C, आदि)",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Listening for physical keypress...",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Captured Key Success Card with re-capture button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonGreen.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, NeonGreen), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Key Set:", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            KeyBadge(keyLabel = keyLabel, color = NeonGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("($name)", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isListeningForKey = true },
                            color = DarkSurfaceHighlight
                        ) {
                            Text(
                                "Press Key ⌨️",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Mouse Click Selectors
                Column {
                    Text("Or Bind Mouse Click (माउस बटन):", fontSize = 11.sp, color = TextTertiary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    keyCode = ProfileRepository.KEYCODE_MOUSE_LEFT
                                    keyLabel = "L-Click"
                                    name = "Shoot / Fire"
                                    category = BindingCategory.COMBAT
                                    isListeningForKey = false
                                },
                            color = if (keyLabel == "L-Click") NeonRed.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = BorderStroke(1.dp, if (keyLabel == "L-Click") NeonRed else DarkBorder)
                        ) {
                            Text("🖱️ Left Click", color = if (keyLabel == "L-Click") NeonRed else TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    keyCode = ProfileRepository.KEYCODE_MOUSE_RIGHT
                                    keyLabel = "R-Click"
                                    name = "Aim / ADS"
                                    category = BindingCategory.COMBAT
                                    isListeningForKey = false
                                },
                            color = if (keyLabel == "R-Click") CyberAmber.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = BorderStroke(1.dp, if (keyLabel == "R-Click") CyberAmber else DarkBorder)
                        ) {
                            Text("🖱️ Right Click", color = if (keyLabel == "R-Click") CyberAmber else TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    keyCode = ProfileRepository.KEYCODE_MOUSE_MIDDLE
                                    keyLabel = "M-Click"
                                    name = "Enemy Mark / Ping"
                                    category = BindingCategory.TACTICAL
                                    isListeningForKey = false
                                },
                            color = if (keyLabel == "M-Click") CyberCyan.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = BorderStroke(1.dp, if (keyLabel == "M-Click") CyberCyan else DarkBorder)
                        ) {
                            Text("🖱️ Middle", color = if (keyLabel == "M-Click") CyberCyan else TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // Action Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Action Name (Action ka naam)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Individual Button Size Scale Slider (सिर्फ इस बटन का साइज)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Button Size (बटन साइज)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Changes only this button", fontSize = 10.sp, color = TextSecondary)
                    }
                    Text("${(size * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                }
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                )

                // Quick Size Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("Compact 75%", 0.75f),
                        Pair("Normal 100%", 1.0f),
                        Pair("Large 130%", 1.30f),
                        Pair("Huge 160%", 1.60f)
                    ).forEach { (label, presetSize) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { size = presetSize },
                            color = if ((size * 100).roundToInt() == (presetSize * 100).roundToInt()) CyberAmber.copy(alpha = 0.2f) else DarkSurfaceHighlight,
                            border = BorderStroke(1.dp, if ((size * 100).roundToInt() == (presetSize * 100).roundToInt()) CyberAmber else DarkBorder)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if ((size * 100).roundToInt() == (presetSize * 100).roundToInt()) CyberAmber else TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Coordinate Sliders (Target X and Y)
                Text("Screen Position X: ${(targetX * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                Slider(
                    value = targetX,
                    onValueChange = { targetX = it },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Text("Screen Position Y: ${(targetY * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
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
                text = "Save Key",
                onClick = {
                    onSave(
                        binding.copy(
                            name = name.ifBlank { "Custom Key" },
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
