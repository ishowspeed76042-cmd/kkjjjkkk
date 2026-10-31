package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.example.model.CameraSwipeConfig
import com.example.model.JoystickConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.hypot

data class VisualTouchEvent(
    val x: Float,
    val y: Float,
    val isMouseSwipe: Boolean = false,
    val label: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class GestureInjector {

    companion object {
        private const val TAG = "GestureInjector"
        val instance = GestureInjector()
    }

    private var accessibilityService: AccessibilityService? = null

    // Screen dimensions in pixels
    var screenWidth: Int = 1080
    var screenHeight: Int = 2400

    // Accumulated mouse deltas for smooth paced gesture dispatching
    private var pendingDeltaX: Float = 0f
    private var pendingDeltaY: Float = 0f
    private var isSwipeBusy: Boolean = false
    private val swipeLock = Any()

    // Live touch event stream for in-app visualizer & sandbox
    private val _visualTouchFlow = MutableSharedFlow<VisualTouchEvent>(extraBufferCapacity = 64)
    val visualTouchFlow: SharedFlow<VisualTouchEvent> = _visualTouchFlow.asSharedFlow()

    fun attachService(service: AccessibilityService) {
        accessibilityService = service
        Log.d(TAG, "AccessibilityService attached to GestureInjector")
    }

    fun detachService() {
        accessibilityService = null
        synchronized(swipeLock) {
            isSwipeBusy = false
            pendingDeltaX = 0f
            pendingDeltaY = 0f
        }
        Log.d(TAG, "AccessibilityService detached from GestureInjector")
    }

    fun isServiceReady(): Boolean = accessibilityService != null

    /**
     * Injects a quick tap gesture at normalized coordinates (0.0f..1.0f).
     */
    fun injectTap(normX: Float, normY: Float, label: String? = null, durationMs: Long = 25L): Boolean {
        val px = normX * screenWidth
        val py = normY * screenHeight

        _visualTouchFlow.tryEmit(VisualTouchEvent(px, py, isMouseSwipe = false, label = label))

        val service = accessibilityService ?: return false
        val path = Path().apply {
            moveTo(px, py)
            lineTo(px + 2f, py + 2f)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(15L, 60L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Injects an active drag stroke for the virtual joystick (WASD in Free Fire).
     * Starts at joystick center and drags outward with sufficient distance
     * to trigger full walking / sprinting in Free Fire.
     */
    fun injectJoystickVector(
        config: JoystickConfig,
        dirX: Float, // -1.0f (left) to 1.0f (right)
        dirY: Float  // -1.0f (up) to 1.0f (down)
    ): Boolean {
        val centerX = config.centerX * screenWidth
        val centerY = config.centerY * screenHeight
        // Use configurable dragDistance (e.g. 14% of screen) to ensure Free Fire sprint activation
        val dragDistPx = config.dragDistance * screenWidth

        val targetX = (centerX + dirX * dragDistPx).coerceIn(0f, screenWidth.toFloat())
        val targetY = (centerY + dirY * dragDistPx).coerceIn(0f, screenHeight.toFloat())

        _visualTouchFlow.tryEmit(
            VisualTouchEvent(targetX, targetY, isMouseSwipe = false, label = "WASD")
        )

        val service = accessibilityService ?: return false
        val path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(targetX, targetY)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 25L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Core FPS Mouse Look:
     * Converts physical mouse movement into smooth, continuous camera swipes.
     * Accumulates deltas and dispatches them sequentially through GestureResultCallback
     * so no mouse movements are dropped by the OS gesture throttler.
     */
    fun injectMouseDelta(
        deltaX: Float,
        deltaY: Float,
        config: CameraSwipeConfig
    ) {
        val mag = hypot(deltaX, deltaY)
        if (mag < config.deadzone) return

        synchronized(swipeLock) {
            pendingDeltaX += deltaX
            pendingDeltaY += deltaY
            if (!isSwipeBusy) {
                dispatchPendingMouseSwipe(config)
            }
        }
    }

    private fun dispatchPendingMouseSwipe(config: CameraSwipeConfig) {
        val service = accessibilityService ?: return

        var curDx: Float
        var curDy: Float
        synchronized(swipeLock) {
            curDx = pendingDeltaX
            curDy = pendingDeltaY
            pendingDeltaX = 0f
            pendingDeltaY = 0f
            if (hypot(curDx, curDy) < 0.5f) {
                isSwipeBusy = false
                return
            }
            isSwipeBusy = true
        }

        // Camera zone bounding box (right half of screen in Free Fire)
        val zoneLeft = config.zoneLeft * screenWidth
        val zoneRight = config.zoneRight * screenWidth
        val zoneTop = config.zoneTop * screenHeight
        val zoneBottom = config.zoneBottom * screenHeight
        val startX = (zoneLeft + zoneRight) / 2f
        val startY = (zoneTop + zoneBottom) / 2f

        // Apply sensitivity, invert Y if needed, and speed multiplier with crisp gaming amplification (8.5x)
        // Moving mouse left (curDx < 0) produces a swipe to the left (endX < startX), rotating camera left.
        // Moving mouse right (curDx > 0) produces a swipe to the right (endX > startX), rotating camera right.
        val effectiveDy = if (config.invertY) -curDy else curDy
        val amplification = 8.5f
        val swipeLengthX = curDx * config.sensitivityX * config.speedMultiplier * amplification
        val swipeLengthY = effectiveDy * config.sensitivityY * config.speedMultiplier * amplification

        // End point clamped inside swipe zone
        val endX = (startX + swipeLengthX).coerceIn(zoneLeft + 20f, zoneRight - 20f)
        val endY = (startY + swipeLengthY).coerceIn(zoneTop + 20f, zoneBottom - 20f)

        _visualTouchFlow.tryEmit(
            VisualTouchEvent(endX, endY, isMouseSwipe = true, label = "Aim")
        )

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        // Fast 20ms stroke allows quick touch-swipe execution and rapid re-dispatch
        val stroke = GestureDescription.StrokeDescription(path, 0, 20L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    synchronized(swipeLock) {
                        isSwipeBusy = false
                    }
                    dispatchPendingMouseSwipe(config)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    synchronized(swipeLock) {
                        isSwipeBusy = false
                    }
                    dispatchPendingMouseSwipe(config)
                }
            },
            null
        )

        if (!dispatched) {
            synchronized(swipeLock) {
                isSwipeBusy = false
            }
        }
    }

    fun resetVirtualPointer() {
        synchronized(swipeLock) {
            pendingDeltaX = 0f
            pendingDeltaY = 0f
            isSwipeBusy = false
        }
    }
}
