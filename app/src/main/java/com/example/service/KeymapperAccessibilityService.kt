package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.example.data.ProfileRepository
import com.example.engine.GestureInjector
import com.example.model.MappingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeymapperAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KeymapperAccService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _isMappingEnabled = MutableStateFlow(true)
        val isMappingEnabled: StateFlow<Boolean> = _isMappingEnabled.asStateFlow()

        private val _isMouseLocked = MutableStateFlow(false)
        val isMouseLocked: StateFlow<Boolean> = _isMouseLocked.asStateFlow()

        // Last pressed key event log for test sandbox & stats
        val lastKeyEventFlow = MutableStateFlow<String?>(null)

        fun setMappingEnabled(enabled: Boolean) {
            _isMappingEnabled.value = enabled
        }

        fun toggleMouseLock() {
            _isMouseLocked.value = !_isMouseLocked.value
        }

        fun setMouseLocked(locked: Boolean) {
            _isMouseLocked.value = locked
        }
    }

    private val pressedMovementKeys = mutableSetOf<Int>()
    private var repository: ProfileRepository? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
        repository = ProfileRepository.getInstance(applicationContext)

        // Determine screen dimensions
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(displayMetrics)
        if (displayMetrics.widthPixels > 0 && displayMetrics.heightPixels > 0) {
            GestureInjector.instance.screenWidth = displayMetrics.widthPixels
            GestureInjector.instance.screenHeight = displayMetrics.heightPixels
        } else {
            val resMetrics = resources.displayMetrics
            GestureInjector.instance.screenWidth = resMetrics.widthPixels
            GestureInjector.instance.screenHeight = resMetrics.heightPixels
        }

        GestureInjector.instance.attachService(this)
        Log.d(TAG, "Keymapper Accessibility Service connected. Screen: ${GestureInjector.instance.screenWidth}x${GestureInjector.instance.screenHeight}")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!_isMappingEnabled.value) {
            return super.onKeyEvent(event)
        }

        val profile = repository?.activeProfile?.value ?: return super.onKeyEvent(event)
        val keyCode = event.keyCode
        val action = event.action

        lastKeyEventFlow.value = "Key: ${KeyEvent.keyCodeToString(keyCode)} (${if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"})"

        // Check for Mouse Lock Toggle key (e.g. `~` / Grave key)
        if (keyCode == profile.camera.lockToggleKeyCode) {
            if (action == KeyEvent.ACTION_DOWN) {
                toggleMouseLock()
                Log.d(TAG, "Mouse lock toggled: ${_isMouseLocked.value}")
            }
            return true
        }

        // Check for Movement (WASD) keys
        val joystick = profile.joystick
        if (joystick.enabled && (keyCode == joystick.upKey || keyCode == joystick.downKey ||
                    keyCode == joystick.leftKey || keyCode == joystick.rightKey)
        ) {
            if (action == KeyEvent.ACTION_DOWN) {
                pressedMovementKeys.add(keyCode)
            } else if (action == KeyEvent.ACTION_UP) {
                pressedMovementKeys.remove(keyCode)
            }

            var dirX = 0f
            var dirY = 0f
            if (joystick.rightKey in pressedMovementKeys) dirX += 1f
            if (joystick.leftKey in pressedMovementKeys) dirX -= 1f
            if (joystick.downKey in pressedMovementKeys) dirY += 1f
            if (joystick.upKey in pressedMovementKeys) dirY -= 1f

            if (dirX != 0f || dirY != 0f) {
                GestureInjector.instance.injectJoystickVector(joystick, dirX, dirY)
            }
            return true
        }

        // Check for mapped action keys
        val matchedBinding = profile.bindings.find { it.keyCode == keyCode }
        if (matchedBinding != null) {
            if (action == KeyEvent.ACTION_DOWN) {
                GestureInjector.instance.injectTap(
                    normX = matchedBinding.targetX,
                    normY = matchedBinding.targetY,
                    label = matchedBinding.keyLabel
                )
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Not used for active gesture injection
    }

    override fun onInterrupt() {
        pressedMovementKeys.clear()
        GestureInjector.instance.resetVirtualPointer()
        Log.d(TAG, "Keymapper Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        GestureInjector.instance.detachService()
        Log.d(TAG, "Keymapper Accessibility Service destroyed")
    }
}
