package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.data.ProfileRepository
import com.example.engine.GestureInjector
import com.example.model.KeyBinding
import com.example.model.MappingProfile
import com.example.service.KeymapperAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val repository = ProfileRepository.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var floatingPillView: View? = null
    private var keyPinsOverlayView: FrameLayout? = null
    private var mouseCaptureView: View? = null

    private var isEditMode = false
    private var isOverlayVisible = false

    fun start() {
        scope.launch {
            repository.activeProfile.collectLatest { profile ->
                if (profile.floatingOverlayEnabled && Settings.canDrawOverlays(context)) {
                    showFloatingPill(profile)
                } else {
                    hideFloatingPill()
                    hideKeyPinsOverlay()
                }
            }
        }

        scope.launch {
            KeymapperAccessibilityService.isMouseLocked.collectLatest { locked ->
                if (locked && Settings.canDrawOverlays(context)) {
                    enableMouseCapture()
                } else {
                    disableMouseCapture()
                }
            }
        }
    }

    fun stop() {
        hideFloatingPill()
        hideKeyPinsOverlay()
        disableMouseCapture()
        scope.cancel()
    }

    fun toggleOverlay(forceShow: Boolean? = null) {
        val shouldShow = forceShow ?: !isOverlayVisible
        val profile = repository.activeProfile.value
        repository.toggleFloatingOverlay(shouldShow)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingPill(profile: MappingProfile) {
        if (!Settings.canDrawOverlays(context) || windowManager == null) return
        if (floatingPillView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        // Create pill UI
        val pillLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                setColor(Color.argb((profile.overlayOpacity * 255).toInt(), 19, 27, 46))
                cornerRadius = 48f
                setStroke(3, Color.parseColor("#00E5FF"))
            }
        }

        // Status text
        val titleText = TextView(context).apply {
            text = "⚡ Apex"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 16, 0)
        }
        pillLayout.addView(titleText)

        // Mouse Lock toggle button
        val lockBtn = TextView(context).apply {
            text = if (KeymapperAccessibilityService.isMouseLocked.value) "🎯 LOCKED" else "🔓 UNLOCKED"
            setTextColor(if (KeymapperAccessibilityService.isMouseLocked.value) Color.parseColor("#00E676") else Color.parseColor("#FFB300"))
            textSize = 12f
            setPadding(12, 8, 12, 8)
            background = GradientDrawable().apply {
                setColor(Color.argb(120, 35, 49, 82))
                cornerRadius = 24f
            }
            setOnClickListener {
                KeymapperAccessibilityService.toggleMouseLock()
                text = if (KeymapperAccessibilityService.isMouseLocked.value) "🎯 LOCKED" else "🔓 UNLOCKED"
                setTextColor(if (KeymapperAccessibilityService.isMouseLocked.value) Color.parseColor("#00E676") else Color.parseColor("#FFB300"))
            }
        }
        pillLayout.addView(lockBtn)

        // Edit HUD Pin mapping button
        val editBtn = TextView(context).apply {
            text = "✏️ HUD"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                toggleKeyPinsOverlay()
            }
        }
        pillLayout.addView(editBtn)

        // Drag handler for the floating pill
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        pillLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.hypot(dx.toDouble(), dy.toDouble()) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(pillLayout, params)
            floatingPillView = pillLayout
            isOverlayVisible = true
            Log.d(TAG, "Floating pill added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating pill", e)
        }
    }

    private fun hideFloatingPill() {
        try {
            floatingPillView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating pill", e)
        } finally {
            floatingPillView = null
            isOverlayVisible = false
        }
    }

    /**
     * Toggles the interactive visual on-screen HUD drag-editor:
     * Shows translucent draggable button badges directly over Free Fire
     * so user can align keys to game buttons with 100% precision!
     */
    fun toggleKeyPinsOverlay() {
        if (keyPinsOverlayView != null) {
            hideKeyPinsOverlay()
        } else {
            showKeyPinsOverlay()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showKeyPinsOverlay() {
        if (!Settings.canDrawOverlays(context) || windowManager == null) return
        if (keyPinsOverlayView != null) return

        val profile = repository.activeProfile.value
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val screenW = GestureInjector.instance.screenWidth
        val screenH = GestureInjector.instance.screenHeight

        val rootOverlay = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(80, 0, 0, 0)) // Soft dark tint
        }

        // Header instruction bar
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 20, 32, 20)
            setBackgroundColor(Color.parseColor("#CC131B2E"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }

            val infoText = TextView(context).apply {
                text = "🎯 Drag & position keys over Free Fire buttons. Tap 'Save HUD' when done."
                setTextColor(Color.WHITE)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(infoText)

            val doneBtn = TextView(context).apply {
                text = "✓ Save HUD"
                setTextColor(Color.parseColor("#0B0F19"))
                textSize = 13f
                setPadding(28, 14, 28, 14)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#00E5FF"))
                    cornerRadius = 20f
                }
                setOnClickListener {
                    hideKeyPinsOverlay()
                }
            }
            addView(doneBtn)
        }
        rootOverlay.addView(topBar)

        // Draw Joystick circle
        val joy = profile.joystick
        if (joy.enabled) {
            val joySize = (joy.radius * 2 * screenW).toInt()
            val joyView = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(90, 0, 229, 255))
                    setStroke(3, Color.parseColor("#00E5FF"))
                }
                val joyLabel = TextView(context).apply {
                    text = "WASD\nMove"
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(joyLabel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            val joyParams = FrameLayout.LayoutParams(joySize, joySize).apply {
                leftMargin = (joy.centerX * screenW - joySize / 2f).toInt()
                topMargin = (joy.centerY * screenH - joySize / 2f).toInt()
            }
            makeDraggable(joyView, joyParams, screenW, screenH) { newNormX, newNormY ->
                repository.updateJoystickConfig(joy.copy(centerX = newNormX, centerY = newNormY))
            }
            rootOverlay.addView(joyView, joyParams)
        }

        // Draw each keybinding badge as a draggable badge
        profile.bindings.forEach { binding ->
            val pinSize = 130
            val badge = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    when (binding.id) {
                        "fire" -> {
                            setColor(Color.argb(190, 255, 82, 82))
                            setStroke(3, Color.WHITE)
                        }
                        "aim" -> {
                            setColor(Color.argb(190, 255, 179, 0))
                            setStroke(3, Color.WHITE)
                        }
                        "gloo_wall" -> {
                            setColor(Color.argb(190, 0, 230, 118))
                            setStroke(3, Color.WHITE)
                        }
                        else -> {
                            setColor(Color.argb(190, 26, 36, 61))
                            setStroke(2, Color.parseColor("#00E5FF"))
                        }
                    }
                }

                val labelView = TextView(context).apply {
                    text = binding.keyLabel
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(labelView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }

            val badgeParams = FrameLayout.LayoutParams(pinSize, pinSize).apply {
                leftMargin = ((binding.targetX * screenW) - pinSize / 2f).toInt().coerceIn(0, screenW - pinSize)
                topMargin = ((binding.targetY * screenH) - pinSize / 2f).toInt().coerceIn(0, screenH - pinSize)
            }

            makeDraggable(badge, badgeParams, screenW, screenH) { newNormX, newNormY ->
                repository.updateBinding(binding.copy(targetX = newNormX, targetY = newNormY))
            }

            rootOverlay.addView(badge, badgeParams)
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(rootOverlay, overlayParams)
            keyPinsOverlayView = rootOverlay
            Log.d(TAG, "KeyPinsOverlay displayed for HUD editing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display KeyPinsOverlay", e)
        }
    }

    private fun hideKeyPinsOverlay() {
        try {
            keyPinsOverlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing KeyPinsOverlay", e)
        } finally {
            keyPinsOverlayView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDraggable(
        view: View,
        params: FrameLayout.LayoutParams,
        screenW: Int,
        screenH: Int,
        onDrop: (normX: Float, normY: Float) -> Unit
    ) {
        var startTouchX = 0f
        var startTouchY = 0f
        var startLeftMargin = 0
        var startTopMargin = 0

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    startLeftMargin = params.leftMargin
                    startTopMargin = params.topMargin
                    v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    params.leftMargin = (startLeftMargin + dx).coerceIn(0, screenW - params.width)
                    params.topMargin = (startTopMargin + dy).coerceIn(0, screenH - params.height)
                    v.layoutParams = params
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    val centerX = params.leftMargin + params.width / 2f
                    val centerY = params.topMargin + params.height / 2f
                    val normX = (centerX / screenW).coerceIn(0f, 1f)
                    val normY = (centerY / screenH).coerceIn(0f, 1f)
                    onDrop(normX, normY)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Mouse Capture Window:
     * When Mouse Lock is engaged, requests Pointer Capture to capture physical mouse
     * movements (AXIS_RELATIVE_X / AXIS_RELATIVE_Y) without showing the system cursor,
     * translating motion to camera swipe gestures in real-time.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun enableMouseCapture() {
        if (!Settings.canDrawOverlays(context) || windowManager == null) return
        if (mouseCaptureView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Invisible full-screen view capable of receiving focused pointer capture
        val params = WindowManager.LayoutParams(
            1, 1, // Minimal footprint
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        val captureView = View(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureView.setOnCapturedPointerListener { _, event ->
                handleCapturedPointer(event)
                true
            }
        }

        captureView.setOnGenericMotionListener { _, event ->
            handleCapturedPointer(event)
            true
        }

        try {
            windowManager.addView(captureView, params)
            mouseCaptureView = captureView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                captureView.requestPointerCapture()
            }
            Log.d(TAG, "Mouse pointer capture view enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add mouse capture view", e)
        }
    }

    private fun handleCapturedPointer(event: MotionEvent) {
        val profile = repository.activeProfile.value
        val camera = profile.camera

        var dx = 0f
        var dy = 0f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dx = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
            dy = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
        }

        if (dx == 0f && dy == 0f) {
            dx = event.getAxisValue(MotionEvent.AXIS_X)
            dy = event.getAxisValue(MotionEvent.AXIS_Y)
        }

        if (dx != 0f || dy != 0f) {
            GestureInjector.instance.injectMouseDelta(dx, dy, camera)
        }

        // Check for Left Click (Fire) & Right Click (Aim)
        val buttonState = event.buttonState
        if ((buttonState and MotionEvent.BUTTON_PRIMARY) != 0) {
            profile.bindings.find { it.id == "fire" }?.let { fire ->
                GestureInjector.instance.injectTap(fire.targetX, fire.targetY, "L-Click")
            }
        }
        if ((buttonState and MotionEvent.BUTTON_SECONDARY) != 0) {
            profile.bindings.find { it.id == "aim" }?.let { aim ->
                GestureInjector.instance.injectTap(aim.targetX, aim.targetY, "R-Click")
            }
        }
    }

    private fun disableMouseCapture() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mouseCaptureView?.releasePointerCapture()
            }
            mouseCaptureView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing mouse capture view", e)
        } finally {
            mouseCaptureView = null
        }
    }
}
