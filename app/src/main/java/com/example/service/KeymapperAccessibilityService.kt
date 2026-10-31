package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.example.data.ProfileRepository
import com.example.engine.GestureInjector
import com.example.model.JoystickConfig
import com.example.model.MappingProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

        var currentServiceInstance: KeymapperAccessibilityService? = null
            private set

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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pressedMovementKeys = mutableSetOf<Int>()
    private var joystickLoopJob: Job? = null
    private var repository: ProfileRepository? = null

    private var currentDirX = 0f
    private var currentDirY = 0f

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentServiceInstance = this
        _isServiceConnected.value = true
        repository = ProfileRepository.getInstance(applicationContext)

        // Determine real screen dimensions
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
        return processKeyEvent(event)
    }

    /**
     * Centralized key event processor called either by AccessibilityService
     * or by the focused pointer capture overlay window.
     */
    fun processKeyEvent(event: KeyEvent): Boolean {
        val profile = repository?.activeProfile?.value ?: return false
        val keyCode = event.keyCode
        val action = event.action

        lastKeyEventFlow.value = "Key: ${KeyEvent.keyCodeToString(keyCode)} (${if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"})"

        // EMERGENCY UNLOCK: Volume Up Key immediately frees the display and mouse lock
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (_isMouseLocked.value) {
                if (action == KeyEvent.ACTION_DOWN) {
                    setMouseLocked(false)
                    com.example.overlay.FloatingOverlayManager.emergencyUnlock(this)
                    android.widget.Toast.makeText(
                        this,
                        "🔓 Emergency Unlock: Display and Mouse Unlocked!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        }

        // Check for Mouse Lock Toggle key (e.g. `~` / Grave key)
        if (keyCode == profile.camera.lockToggleKeyCode) {
            if (action == KeyEvent.ACTION_DOWN) {
                toggleMouseLock()
                Log.d(TAG, "Mouse lock toggled: ${_isMouseLocked.value}")
            }
            return true
        }

        // Check for Movement (WASD) keys: continuous active drag in Free Fire
        val joystick = profile.joystick
        if (joystick.enabled && (keyCode == joystick.upKey || keyCode == joystick.downKey ||
                    keyCode == joystick.leftKey || keyCode == joystick.rightKey)
        ) {
            synchronized(pressedMovementKeys) {
                if (action == KeyEvent.ACTION_DOWN) {
                    pressedMovementKeys.add(keyCode)
                } else if (action == KeyEvent.ACTION_UP) {
                    pressedMovementKeys.remove(keyCode)
                }

                var dx = 0f
                var dy = 0f
                if (joystick.rightKey in pressedMovementKeys) dx += 1f
                if (joystick.leftKey in pressedMovementKeys) dx -= 1f
                if (joystick.downKey in pressedMovementKeys) dy += 1f
                if (joystick.upKey in pressedMovementKeys) dy -= 1f

                currentDirX = dx
                currentDirY = dy

                if (dx != 0f || dy != 0f) {
                    startJoystickDragLoop(joystick)
                } else {
                    stopJoystickDragLoop()
                }
            }
            return true
        }

        // Check for mapped action keys (Space, R, C, Z, 1, 2, 3, etc.)
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

        return false
    }

    private fun startJoystickDragLoop(joystick: JoystickConfig) {
        if (joystickLoopJob?.isActive == true) return
        joystickLoopJob = serviceScope.launch {
            while (isActive) {
                val dx = currentDirX
                val dy = currentDirY
                if (dx != 0f || dy != 0f) {
                    GestureInjector.instance.injectJoystickVector(joystick, dx, dy)
                } else {
                    break
                }
                // Pulse interval to sustain continuous joystick movement while leaving time for camera swipes
                delay(75L)
            }
        }
    }

    private fun stopJoystickDragLoop() {
        joystickLoopJob?.cancel()
        joystickLoopJob = null
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        stopJoystickDragLoop()
        pressedMovementKeys.clear()
        GestureInjector.instance.resetVirtualPointer()
        Log.d(TAG, "Keymapper Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        currentServiceInstance = null
        _isServiceConnected.value = false
        stopJoystickDragLoop()
        serviceScope.cancel()
        GestureInjector.instance.detachService()
        Log.d(TAG, "Keymapper Accessibility Service destroyed")
    }
}
