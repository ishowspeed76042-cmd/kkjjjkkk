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
import kotlin.math.abs
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

    // Virtual touch cursor for camera rotation
    private var virtualCameraPoint: PointF? = null
    private var lastMouseTime: Long = 0L

    // Live touch event stream for in-app visualizer & sandbox
    private val _visualTouchFlow = MutableSharedFlow<VisualTouchEvent>(extraBufferCapacity = 64)
    val visualTouchFlow: SharedFlow<VisualTouchEvent> = _visualTouchFlow.asSharedFlow()

    fun attachService(service: AccessibilityService) {
        accessibilityService = service
        Log.d(TAG, "AccessibilityService attached to GestureInjector")
    }

    fun detachService() {
        accessibilityService = null
        virtualCameraPoint = null
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
            lineTo(px + 1f, py + 1f)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(10L, 100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Injects a continuous directional movement for WASD joystick.
     */
    fun injectJoystickVector(
        config: JoystickConfig,
        dirX: Float, // -1.0f (left) to 1.0f (right)
        dirY: Float  // -1.0f (up) to 1.0f (down)
    ): Boolean {
        val centerX = config.centerX * screenWidth
        val centerY = config.centerY * screenHeight
        val radiusPx = config.radius * screenWidth

        val targetX = centerX + dirX * radiusPx
        val targetY = centerY + dirY * radiusPx

        _visualTouchFlow.tryEmit(
            VisualTouchEvent(targetX, targetY, isMouseSwipe = false, label = "WASD")
        )

        val service = accessibilityService ?: return false
        val path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(targetX, targetY)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 40L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * The Core Mouse-to-Touch Camera Rotation Swipe:
     * Converts relative mouse deltas (dx, dy) into seamless continuous swipe gestures
     * in the camera region. Automatically repositions virtual finger when reaching zone borders.
     */
    fun injectMouseDelta(
        deltaX: Float,
        deltaY: Float,
        config: CameraSwipeConfig
    ): Boolean {
        // Deadzone filter to eliminate micro-jitter
        val magnitude = hypot(deltaX, deltaY)
        if (magnitude < config.deadzone) {
            return false
        }

        // Camera zone bounding box in pixels
        val zoneLeft = config.zoneLeft * screenWidth
        val zoneRight = config.zoneRight * screenWidth
        val zoneTop = config.zoneTop * screenHeight
        val zoneBottom = config.zoneBottom * screenHeight
        val zoneCenterX = (zoneLeft + zoneRight) / 2f
        val zoneCenterY = (zoneTop + zoneBottom) / 2f

        // Initialize virtual camera point if idle or expired
        val now = System.currentTimeMillis()
        var currentPoint = virtualCameraPoint
        if (currentPoint == null || (now - lastMouseTime) > 300L) {
            currentPoint = PointF(zoneCenterX, zoneCenterY)
        }
        lastMouseTime = now

        // Calculate scaled deltas
        val effectiveDy = if (config.invertY) -deltaY else deltaY
        val moveDx = deltaX * config.sensitivityX
        val moveDy = effectiveDy * config.sensitivityY

        var nextX = currentPoint.x + moveDx
        var nextY = currentPoint.y + moveDy

        // Margin inside swipe zone before resetting to center
        val marginX = (zoneRight - zoneLeft) * 0.10f
        val marginY = (zoneBottom - zoneTop) * 0.10f

        // Check if virtual finger reached border of camera zone:
        // Seamlessly reset to center like PC emulators do (lift touch and reposition)
        if (nextX < (zoneLeft + marginX) || nextX > (zoneRight - marginX) ||
            nextY < (zoneTop + marginY) || nextY > (zoneBottom - marginY)
        ) {
            currentPoint = PointF(zoneCenterX, zoneCenterY)
            nextX = currentPoint.x + moveDx
            nextY = currentPoint.y + moveDy
        }

        // Clamp inside camera bounds
        val clampedNextX = nextX.coerceIn(zoneLeft, zoneRight)
        val clampedNextY = nextY.coerceIn(zoneTop, zoneBottom)

        // Notify visualizer
        _visualTouchFlow.tryEmit(
            VisualTouchEvent(clampedNextX, clampedNextY, isMouseSwipe = true, label = "Look")
        )

        virtualCameraPoint = PointF(clampedNextX, clampedNextY)

        val service = accessibilityService ?: return false

        // Build swipe gesture
        val path = Path().apply {
            moveTo(currentPoint.x, currentPoint.y)
            lineTo(clampedNextX, clampedNextY)
        }

        // Micro-swipe duration (16ms = ~60fps smooth gesture delivery)
        val stroke = GestureDescription.StrokeDescription(path, 0, 16L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, null, null)
    }

    fun resetVirtualPointer() {
        virtualCameraPoint = null
    }
}
