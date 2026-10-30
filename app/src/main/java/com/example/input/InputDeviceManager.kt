package com.example.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectedDevice(
    val id: Int,
    val name: String,
    val isKeyboard: Boolean,
    val isMouse: Boolean,
    val isGamepad: Boolean,
    val descriptor: String
)

data class InputDeviceState(
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val hasKeyboard: Boolean = false,
    val hasMouse: Boolean = false,
    val keyboardName: String? = null,
    val mouseName: String? = null
)

class InputDeviceManager(private val context: Context) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager

    private val _deviceState = MutableStateFlow(InputDeviceState())
    val deviceState: StateFlow<InputDeviceState> = _deviceState.asStateFlow()

    init {
        refreshDevices()
        inputManager?.registerInputDeviceListener(this, null)
    }

    fun refreshDevices() {
        val deviceIds = InputDevice.getDeviceIds()
        val devices = mutableListOf<ConnectedDevice>()

        var foundKeyboard = false
        var foundMouse = false
        var kbName: String? = null
        var mName: String? = null

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            // Skip virtual/software devices
            if (device.isVirtual) continue

            val sources = device.sources
            val isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD &&
                    device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            val isMouse = (sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
                    (sources and InputDevice.SOURCE_TRACKBALL) == InputDevice.SOURCE_TRACKBALL
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isKeyboard || isMouse || isGamepad) {
                devices.add(
                    ConnectedDevice(
                        id = device.id,
                        name = device.name,
                        isKeyboard = isKeyboard,
                        isMouse = isMouse,
                        isGamepad = isGamepad,
                        descriptor = device.descriptor
                    )
                )
                if (isKeyboard && !foundKeyboard) {
                    foundKeyboard = true
                    kbName = device.name
                }
                if (isMouse && !foundMouse) {
                    foundMouse = true
                    mName = device.name
                }
            }
        }

        _deviceState.value = InputDeviceState(
            connectedDevices = devices,
            hasKeyboard = foundKeyboard,
            hasMouse = foundMouse,
            keyboardName = kbName,
            mouseName = mName
        )
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshDevices()
    }

    fun release() {
        inputManager?.unregisterInputDeviceListener(this)
    }
}
