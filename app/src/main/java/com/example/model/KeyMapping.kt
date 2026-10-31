package com.example.model

import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject

enum class ActionType {
    TAP,
    HOLD,
    CONTINUOUS
}

enum class BindingCategory {
    MOVEMENT,
    COMBAT,
    TACTICAL,
    INVENTORY
}

data class KeyBinding(
    val id: String,
    val name: String,
    val keyCode: Int,
    val keyLabel: String,
    val targetX: Float, // Normalized 0.0f..1.0f (Screen width ratio)
    val targetY: Float, // Normalized 0.0f..1.0f (Screen height ratio)
    val actionType: ActionType = ActionType.TAP,
    val category: BindingCategory = BindingCategory.COMBAT,
    val size: Float = 1.0f // Size scale multiplier (0.6f to 2.5f)
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("keyCode", keyCode)
            put("keyLabel", keyLabel)
            put("targetX", targetX.toDouble())
            put("targetY", targetY.toDouble())
            put("actionType", actionType.name)
            put("category", category.name)
            put("size", size.toDouble())
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): KeyBinding {
            return KeyBinding(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", "Custom Binding"),
                keyCode = json.optInt("keyCode", KeyEvent.KEYCODE_UNKNOWN),
                keyLabel = json.optString("keyLabel", "?"),
                targetX = json.optDouble("targetX", 0.5).toFloat(),
                targetY = json.optDouble("targetY", 0.5).toFloat(),
                actionType = try {
                    ActionType.valueOf(json.optString("actionType", ActionType.TAP.name))
                } catch (e: Exception) {
                    ActionType.TAP
                },
                category = try {
                    BindingCategory.valueOf(json.optString("category", BindingCategory.COMBAT.name))
                } catch (e: Exception) {
                    BindingCategory.COMBAT
                },
                size = json.optDouble("size", 1.0).toFloat().coerceIn(0.5f, 3.0f)
            )
        }
    }
}

data class JoystickConfig(
    val enabled: Boolean = true,
    val centerX: Float = 0.18f,
    val centerY: Float = 0.72f,
    val radius: Float = 0.08f,
    val dragDistance: Float = 0.14f, // Travel distance for active drag (ensures Free Fire walk/sprint)
    val upKey: Int = KeyEvent.KEYCODE_W,
    val downKey: Int = KeyEvent.KEYCODE_S,
    val leftKey: Int = KeyEvent.KEYCODE_A,
    val rightKey: Int = KeyEvent.KEYCODE_D,
    val sprintKey: Int = KeyEvent.KEYCODE_SHIFT_LEFT
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("enabled", enabled)
            put("centerX", centerX.toDouble())
            put("centerY", centerY.toDouble())
            put("radius", radius.toDouble())
            put("dragDistance", dragDistance.toDouble())
            put("upKey", upKey)
            put("downKey", downKey)
            put("leftKey", leftKey)
            put("rightKey", rightKey)
            put("sprintKey", sprintKey)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): JoystickConfig {
            return JoystickConfig(
                enabled = json.optBoolean("enabled", true),
                centerX = json.optDouble("centerX", 0.18).toFloat(),
                centerY = json.optDouble("centerY", 0.72).toFloat(),
                radius = json.optDouble("radius", 0.08).toFloat(),
                dragDistance = json.optDouble("dragDistance", 0.14).toFloat(),
                upKey = json.optInt("upKey", KeyEvent.KEYCODE_W),
                downKey = json.optInt("downKey", KeyEvent.KEYCODE_S),
                leftKey = json.optInt("leftKey", KeyEvent.KEYCODE_A),
                rightKey = json.optInt("rightKey", KeyEvent.KEYCODE_D),
                sprintKey = json.optInt("sprintKey", KeyEvent.KEYCODE_SHIFT_LEFT)
            )
        }
    }
}

data class CameraSwipeConfig(
    val sensitivityX: Float = 1.0f,
    val sensitivityY: Float = 0.9f,
    val speedMultiplier: Float = 6.0f, // Amplifies mouse delta for responsive 360 camera turns
    val invertY: Boolean = false,
    val invertX: Boolean = false,
    val deadzone: Float = 1.0f, // Pixel delta deadzone
    val smoothingFactor: Float = 0.85f, // 0.0 = direct, 1.0 = heavy smoothing
    val zoneLeft: Float = 0.50f, // Normalized screen boundary for swipe injection
    val zoneTop: Float = 0.15f,
    val zoneRight: Float = 0.95f,
    val zoneBottom: Float = 0.85f,
    val lockToggleKeyCode: Int = KeyEvent.KEYCODE_GRAVE, // `~` key to toggle mouse lock
    val lockToggleKeyLabel: String = "~ (Tilde)"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("sensitivityX", sensitivityX.toDouble())
            put("sensitivityY", sensitivityY.toDouble())
            put("speedMultiplier", speedMultiplier.toDouble())
            put("invertY", invertY)
            put("invertX", invertX)
            put("deadzone", deadzone.toDouble())
            put("smoothingFactor", smoothingFactor.toDouble())
            put("zoneLeft", zoneLeft.toDouble())
            put("zoneTop", zoneTop.toDouble())
            put("zoneRight", zoneRight.toDouble())
            put("zoneBottom", zoneBottom.toDouble())
            put("lockToggleKeyCode", lockToggleKeyCode)
            put("lockToggleKeyLabel", lockToggleKeyLabel)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CameraSwipeConfig {
            return CameraSwipeConfig(
                sensitivityX = json.optDouble("sensitivityX", 1.0).toFloat(),
                sensitivityY = json.optDouble("sensitivityY", 0.9).toFloat(),
                speedMultiplier = json.optDouble("speedMultiplier", 6.0).toFloat(),
                invertY = json.optBoolean("invertY", false),
                invertX = json.optBoolean("invertX", false),
                deadzone = json.optDouble("deadzone", 1.0).toFloat(),
                smoothingFactor = json.optDouble("smoothingFactor", 0.85).toFloat(),
                zoneLeft = json.optDouble("zoneLeft", 0.50).toFloat(),
                zoneTop = json.optDouble("zoneTop", 0.15).toFloat(),
                zoneRight = json.optDouble("zoneRight", 0.95).toFloat(),
                zoneBottom = json.optDouble("zoneBottom", 0.85).toFloat(),
                lockToggleKeyCode = json.optInt("lockToggleKeyCode", KeyEvent.KEYCODE_GRAVE),
                lockToggleKeyLabel = json.optString("lockToggleKeyLabel", "~ (Tilde)")
            )
        }
    }
}

data class MappingProfile(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val bindings: List<KeyBinding>,
    val joystick: JoystickConfig = JoystickConfig(),
    val camera: CameraSwipeConfig = CameraSwipeConfig(),
    val floatingOverlayEnabled: Boolean = false,
    val overlayOpacity: Float = 0.70f,
    val globalButtonScale: Float = 1.0f // Global scale for all HUD pins (0.6f .. 2.0f)
) {
    fun toJsonString(indent: Int = 2): String {
        val root = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("isDefault", isDefault)
            put("floatingOverlayEnabled", floatingOverlayEnabled)
            put("overlayOpacity", overlayOpacity.toDouble())
            put("globalButtonScale", globalButtonScale.toDouble())
            put("joystick", joystick.toJsonObject())
            put("camera", camera.toJsonObject())
            val bindingsArray = JSONArray()
            bindings.forEach { bindingsArray.put(it.toJsonObject()) }
            put("bindings", bindingsArray)
        }
        return root.toString(indent)
    }

    companion object {
        fun fromJsonString(jsonStr: String): MappingProfile {
            val root = JSONObject(jsonStr)
            val bindingsList = mutableListOf<KeyBinding>()
            val bindingsArray = root.optJSONArray("bindings")
            if (bindingsArray != null) {
                for (i in 0 until bindingsArray.length()) {
                    val item = bindingsArray.getJSONObject(i)
                    bindingsList.add(KeyBinding.fromJsonObject(item))
                }
            }
            return MappingProfile(
                id = root.optString("id", System.currentTimeMillis().toString()),
                name = root.optString("name", "Imported Profile"),
                description = root.optString("description", "Custom keymapping configuration"),
                isDefault = root.optBoolean("isDefault", false),
                floatingOverlayEnabled = root.optBoolean("floatingOverlayEnabled", false),
                overlayOpacity = root.optDouble("overlayOpacity", 0.70).toFloat(),
                globalButtonScale = root.optDouble("globalButtonScale", 1.0).toFloat().coerceIn(0.5f, 2.5f),
                joystick = root.optJSONObject("joystick")?.let { JoystickConfig.fromJsonObject(it) } ?: JoystickConfig(),
                camera = root.optJSONObject("camera")?.let { CameraSwipeConfig.fromJsonObject(it) } ?: CameraSwipeConfig(),
                bindings = bindingsList
            )
        }
    }
}
