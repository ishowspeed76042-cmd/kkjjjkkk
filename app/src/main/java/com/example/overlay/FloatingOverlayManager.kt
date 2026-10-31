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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.data.ProfileRepository
import com.example.engine.GestureInjector
import com.example.model.ActionType
import com.example.model.BindingCategory
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

        @Volatile
        private var instance: FloatingOverlayManager? = null

        fun emergencyUnlock(ctx: Context? = null) {
            instance?.performEmergencyUnlock()
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val repository = ProfileRepository.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var floatingPillView: View? = null
    private var keyPinsOverlayView: FrameLayout? = null
    private var mouseCaptureView: View? = null
    private var emergencyUnlockBannerView: View? = null

    private var isOverlayVisible = false
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    private var hasPrevMousePos = false

    // Track currently selected binding for individual resizing in HUD
    private var selectedBindingId: String? = null

    init {
        instance = this
    }

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
                    showEmergencyUnlockBanner()
                } else {
                    disableMouseCapture()
                    hideEmergencyUnlockBanner()
                }
            }
        }
    }

    fun stop() {
        hideFloatingPill()
        hideKeyPinsOverlay()
        disableMouseCapture()
        hideEmergencyUnlockBanner()
        scope.cancel()
    }

    fun toggleOverlay(forceShow: Boolean? = null) {
        val shouldShow = forceShow ?: !isOverlayVisible
        repository.toggleFloatingOverlay(shouldShow)
    }

    /**
     * Emergency Unlock: Immediately frees the phone display, dismisses pointer capture,
     * and sets mouse locked to false.
     */
    fun performEmergencyUnlock() {
        Log.d(TAG, "performEmergencyUnlock triggered")
        KeymapperAccessibilityService.setMouseLocked(false)
        disableMouseCapture()
        hideEmergencyUnlockBanner()
        try {
            Toast.makeText(
                context,
                "🔓 Display & Mouse Unlocked! (Press Vol Up anytime)",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
    }

    /**
     * Floating Emergency Unlock Banner & Mouse Capture:
     * High-visibility, compact floating banner at top center when mouse is locked.
     * Takes only a small top strip (~50dp high), keeping the entire rest of the phone display
     * 100% touchable and unblocked.
     * Captures relative mouse movements for FPS look without covering the screen with an invisible blocker.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showEmergencyUnlockBanner() {
        if (!Settings.canDrawOverlays(context) || windowManager == null) return
        if (emergencyUnlockBannerView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_NOT_TOUCH_MODAL ensures all touches outside this top banner reach Free Fire directly
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 20
        }

        val banner = object : LinearLayout(context) {
            override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
                handleCapturedPointer(event)
                return true
            }

            override fun onPointerCaptureChange(hasCapture: Boolean) {
                super.onPointerCaptureChange(hasCapture)
                if (!hasCapture && KeymapperAccessibilityService.isMouseLocked.value) {
                    post { requestPointerCapture() }
                }
            }

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // Hardware Volume Up Key Emergency Unlock
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    performEmergencyUnlock()
                    return true
                }
                val handled = KeymapperAccessibilityService.currentServiceInstance?.processKeyEvent(event) ?: false
                return handled || super.dispatchKeyEvent(event)
            }
        }.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 16, 32, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E53935")) // Red safety alert
                cornerRadius = 40f
                setStroke(3, Color.WHITE)
            }
            elevation = 20f
            isFocusable = true
            isFocusableInTouchMode = true

            val iconText = TextView(context).apply {
                text = "🔓"
                textSize = 16f
                setPadding(0, 0, 10, 0)
            }
            addView(iconText)

            val textMsg = TextView(context).apply {
                text = "MOUSE LOCKED • TAP HERE TO UNLOCK (or Vol Up)"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(textMsg)

            setOnClickListener {
                performEmergencyUnlock()
            }
        }

        try {
            windowManager.addView(banner, params)
            emergencyUnlockBannerView = banner
            hasPrevMousePos = false
            banner.post {
                banner.requestFocus()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    banner.requestPointerCapture()
                }
            }
            Log.d(TAG, "Top Emergency Unlock banner active with pointer capture")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add emergency unlock banner", e)
        }
    }

    private fun hideEmergencyUnlockBanner() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                emergencyUnlockBannerView?.releasePointerCapture()
            }
            emergencyUnlockBannerView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing emergency unlock banner", e)
        } finally {
            emergencyUnlockBannerView = null
            hasPrevMousePos = false
        }
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

        val titleText = TextView(context).apply {
            text = "⚡ Apex"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 14, 0)
        }
        pillLayout.addView(titleText)

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

        val editBtn = TextView(context).apply {
            text = "✏️ HUD Pins"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                toggleKeyPinsOverlay()
            }
        }
        pillLayout.addView(editBtn)

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
     * Toggles visual on-screen HUD drag-editor with individual button resizing and pin management
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
            setBackgroundColor(Color.argb(85, 0, 0, 0))
        }

        // Top Controls Bar
        val topContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
        }

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 14, 24, 14)
            setBackgroundColor(Color.parseColor("#EE131B2E"))

            val infoText = TextView(context).apply {
                text = "🎯 Tap any pin to resize individually. Drag to position."
                setTextColor(Color.WHITE)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(infoText)

            // + Add Button
            val addBtn = TextView(context).apply {
                text = "+ Add Key"
                setTextColor(Color.parseColor("#00E5FF"))
                textSize = 12f
                setPadding(16, 10, 16, 10)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#233152"))
                    cornerRadius = 14f
                    setStroke(2, Color.parseColor("#00E5FF"))
                }
                setOnClickListener {
                    val newId = "custom_${System.currentTimeMillis()}"
                    val newBinding = KeyBinding(
                        id = newId,
                        name = "Custom Key",
                        keyCode = KeyEvent.KEYCODE_T,
                        keyLabel = "T",
                        targetX = 0.50f,
                        targetY = 0.50f,
                        size = 1.0f,
                        actionType = ActionType.TAP,
                        category = BindingCategory.TACTICAL
                    )
                    repository.updateBinding(newBinding)
                    selectedBindingId = newId
                    hideKeyPinsOverlay()
                    showKeyPinsOverlay()
                }
            }
            addView(addBtn)

            val spacer1 = View(context).apply { layoutParams = LinearLayout.LayoutParams(10, 1) }
            addView(spacer1)

            // Clear All Buttons
            val clearBtn = TextView(context).apply {
                text = "🗑️ Clear"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 12f
                setPadding(14, 10, 14, 10)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#233152"))
                    cornerRadius = 14f
                }
                setOnClickListener {
                    repository.clearAllBindings()
                    selectedBindingId = null
                    hideKeyPinsOverlay()
                    showKeyPinsOverlay()
                }
            }
            addView(clearBtn)

            val spacer2 = View(context).apply { layoutParams = LinearLayout.LayoutParams(12, 1) }
            addView(spacer2)

            // Save / Done
            val doneBtn = TextView(context).apply {
                text = "✓ Save HUD"
                setTextColor(Color.parseColor("#0B0F19"))
                textSize = 13f
                setPadding(22, 10, 22, 10)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#00E5FF"))
                    cornerRadius = 18f
                }
                setOnClickListener {
                    hideKeyPinsOverlay()
                }
            }
            addView(doneBtn)
        }
        topContainer.addView(topBar)

        // Selected Pin Individual Resizing / Deleting Bar
        val selectedBinding = profile.bindings.find { it.id == selectedBindingId }
        if (selectedBinding != null) {
            val selectedBar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 10, 24, 10)
                setBackgroundColor(Color.parseColor("#DD1A243B"))

                val selLabel = TextView(context).apply {
                    text = "Selected: '${selectedBinding.name}' (${selectedBinding.keyLabel}) • Size: ${(selectedBinding.size * 100).toInt()}%"
                    setTextColor(Color.parseColor("#FFB300"))
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(selLabel)

                // Individual Smaller Button
                val indSmaller = TextView(context).apply {
                    text = "➖ Smaller"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setPadding(16, 8, 16, 8)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#233152"))
                        cornerRadius = 12f
                    }
                    setOnClickListener {
                        repository.updateBindingSize(selectedBinding.id, -0.15f)
                        hideKeyPinsOverlay()
                        showKeyPinsOverlay()
                    }
                }
                addView(indSmaller)

                val spacerA = View(context).apply { layoutParams = LinearLayout.LayoutParams(10, 1) }
                addView(spacerA)

                // Individual Bigger Button
                val indBigger = TextView(context).apply {
                    text = "➕ Bigger"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setPadding(16, 8, 16, 8)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#233152"))
                        cornerRadius = 12f
                    }
                    setOnClickListener {
                        repository.updateBindingSize(selectedBinding.id, 0.15f)
                        hideKeyPinsOverlay()
                        showKeyPinsOverlay()
                    }
                }
                addView(indBigger)

                val spacerB = View(context).apply { layoutParams = LinearLayout.LayoutParams(10, 1) }
                addView(spacerB)

                // Delete this pin
                val delThis = TextView(context).apply {
                    text = "🗑️ Delete Pin"
                    setTextColor(Color.parseColor("#FF5252"))
                    textSize = 12f
                    setPadding(16, 8, 16, 8)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#3D1E28"))
                        cornerRadius = 12f
                    }
                    setOnClickListener {
                        repository.deleteBinding(selectedBinding.id)
                        selectedBindingId = null
                        hideKeyPinsOverlay()
                        showKeyPinsOverlay()
                    }
                }
                addView(delThis)
            }
            topContainer.addView(selectedBar)
        }

        rootOverlay.addView(topContainer)

        // Draw Joystick circle
        val joy = profile.joystick
        if (joy.enabled) {
            val joySize = (joy.radius * 2 * screenW * profile.globalButtonScale).toInt().coerceIn(120, 360)
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
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(joyLabel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            val joyParams = FrameLayout.LayoutParams(joySize, joySize).apply {
                leftMargin = (joy.centerX * screenW - joySize / 2f).toInt()
                topMargin = (joy.centerY * screenH - joySize / 2f).toInt()
            }
            makeDraggable(joyView, joyParams, screenW, screenH, onSelect = {
                // Joystick selected
            }) { newNormX, newNormY ->
                repository.updateJoystickConfig(joy.copy(centerX = newNormX, centerY = newNormY))
            }
            rootOverlay.addView(joyView, joyParams)
        }

        // Draw each keybinding badge with individual sizing & selection
        profile.bindings.forEach { binding ->
            val baseSize = 120
            val isSelected = (binding.id == selectedBindingId)
            val pinSize = (baseSize * binding.size * profile.globalButtonScale).toInt().coerceIn(60, 300)

            val badge = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    when {
                        isSelected -> {
                            setColor(Color.argb(230, 255, 179, 0))
                            setStroke(4, Color.WHITE)
                        }
                        binding.id == "fire" -> {
                            setColor(Color.argb(200, 255, 82, 82))
                            setStroke(3, Color.WHITE)
                        }
                        binding.id == "aim" -> {
                            setColor(Color.argb(200, 255, 179, 0))
                            setStroke(3, Color.WHITE)
                        }
                        binding.id == "gloo_wall" -> {
                            setColor(Color.argb(200, 0, 230, 118))
                            setStroke(3, Color.WHITE)
                        }
                        else -> {
                            setColor(Color.argb(200, 26, 36, 61))
                            setStroke(2, Color.parseColor("#00E5FF"))
                        }
                    }
                }

                val labelView = TextView(context).apply {
                    text = binding.keyLabel
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    textSize = (11f * binding.size * profile.globalButtonScale).coerceIn(9f, 22f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(labelView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }

            val badgeParams = FrameLayout.LayoutParams(pinSize, pinSize).apply {
                leftMargin = ((binding.targetX * screenW) - pinSize / 2f).toInt().coerceIn(0, screenW - pinSize)
                topMargin = ((binding.targetY * screenH) - pinSize / 2f).toInt().coerceIn(0, screenH - pinSize)
            }

            makeDraggable(badge, badgeParams, screenW, screenH, onSelect = {
                selectedBindingId = binding.id
                hideKeyPinsOverlay()
                showKeyPinsOverlay()
            }) { newNormX, newNormY ->
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
        onSelect: () -> Unit,
        onDrop: (normX: Float, normY: Float) -> Unit
    ) {
        var startTouchX = 0f
        var startTouchY = 0f
        var startLeftMargin = 0
        var startTopMargin = 0
        var hasMoved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    startLeftMargin = params.leftMargin
                    startTopMargin = params.topMargin
                    hasMoved = false
                    v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(80).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    if (Math.hypot(dx.toDouble(), dy.toDouble()) > 10) {
                        hasMoved = true
                    }
                    params.leftMargin = (startLeftMargin + dx).coerceIn(0, screenW - params.width)
                    params.topMargin = (startTopMargin + dy).coerceIn(0, screenH - params.height)
                    v.layoutParams = params
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                    if (!hasMoved) {
                        onSelect()
                    } else {
                        val centerX = params.leftMargin + params.width / 2f
                        val centerY = params.topMargin + params.height / 2f
                        val normX = (centerX / screenW).coerceIn(0f, 1f)
                        val normY = (centerY / screenH).coerceIn(0f, 1f)
                        onDrop(normX, normY)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Mouse Capture is handled directly by showEmergencyUnlockBanner() at the top center of the screen.
     * This guarantees that the main phone display is never blocked by a full-screen invisible window.
     */
    private fun enableMouseCapture() {
        showEmergencyUnlockBanner()
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

        // Fallback for mice sending absolute positions or generic hover events
        if (dx == 0f && dy == 0f) {
            if (hasPrevMousePos) {
                dx = event.x - lastMouseX
                dy = event.y - lastMouseY
            }
            lastMouseX = event.x
            lastMouseY = event.y
            hasPrevMousePos = true
        }

        if (dx != 0f || dy != 0f) {
            GestureInjector.instance.injectMouseDelta(dx, dy, camera)
        }

        // Left Click -> Fire button
        val buttonState = event.buttonState
        if ((buttonState and MotionEvent.BUTTON_PRIMARY) != 0) {
            profile.bindings.find { it.id == "fire" }?.let { fire ->
                GestureInjector.instance.injectTap(fire.targetX, fire.targetY, "L-Click")
            }
        }
        // Right Click -> Aim / ADS button
        if ((buttonState and MotionEvent.BUTTON_SECONDARY) != 0) {
            profile.bindings.find { it.id == "aim" }?.let { aim ->
                GestureInjector.instance.injectTap(aim.targetX, aim.targetY, "R-Click")
            }
        }
    }

    private fun disableMouseCapture() {
        hideEmergencyUnlockBanner()
    }
}
