package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import com.example.model.ActionType
import com.example.model.BindingCategory
import com.example.model.CameraSwipeConfig
import com.example.model.JoystickConfig
import com.example.model.KeyBinding
import com.example.model.MappingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class ProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("apex_mapper_profiles", Context.MODE_PRIVATE)

    private val _profiles: MutableStateFlow<List<MappingProfile>>
    val profiles: StateFlow<List<MappingProfile>>

    private val _activeProfile: MutableStateFlow<MappingProfile>
    val activeProfile: StateFlow<MappingProfile>

    init {
        val initialProfiles = loadInitialProfiles()
        _profiles = MutableStateFlow(initialProfiles)
        profiles = _profiles.asStateFlow()

        val activeId = prefs.getString(PREF_ACTIVE_PROFILE_ID, null)
        val initialActive = initialProfiles.find { it.id == activeId }
            ?: initialProfiles.firstOrNull()
            ?: getFreeFireDefaultProfile()
        _activeProfile = MutableStateFlow(initialActive)
        activeProfile = _activeProfile.asStateFlow()
    }

    companion object {
        const val PREF_ACTIVE_PROFILE_ID = "active_profile_id"
        const val PREF_PROFILES_JSON = "profiles_json"

        // Mouse Button virtual keycodes
        const val KEYCODE_MOUSE_LEFT = 1001
        const val KEYCODE_MOUSE_RIGHT = 1002
        const val KEYCODE_MOUSE_MIDDLE = 1003
        const val KEYCODE_MOUSE_BACK = 1004
        const val KEYCODE_MOUSE_FORWARD = 1005

        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(context: Context): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getFreeFireDefaultProfile(): MappingProfile {
        val bindings = listOf(
            KeyBinding("fire", "Shoot / Fire", KEYCODE_MOUSE_LEFT, "L-Click", 0.88f, 0.58f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("aim", "Aim Down Sights (ADS)", KEYCODE_MOUSE_RIGHT, "R-Click", 0.82f, 0.40f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("reload", "Reload", KeyEvent.KEYCODE_R, "R", 0.82f, 0.26f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("jump", "Jump", KeyEvent.KEYCODE_SPACE, "Space", 0.92f, 0.72f, ActionType.TAP, BindingCategory.MOVEMENT),
            KeyBinding("crouch", "Crouch", KeyEvent.KEYCODE_C, "C", 0.82f, 0.78f, ActionType.TAP, BindingCategory.MOVEMENT),
            KeyBinding("prone", "Prone", KeyEvent.KEYCODE_Z, "Z", 0.72f, 0.88f, ActionType.TAP, BindingCategory.MOVEMENT),
            KeyBinding("sprint", "Sprint / Rush", KeyEvent.KEYCODE_SHIFT_LEFT, "Shift", 0.22f, 0.48f, ActionType.TAP, BindingCategory.MOVEMENT),
            KeyBinding("weapon_1", "Primary Weapon", KeyEvent.KEYCODE_1, "1", 0.58f, 0.10f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("weapon_2", "Secondary Weapon", KeyEvent.KEYCODE_2, "2", 0.72f, 0.10f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("weapon_3", "Melee / Pistol", KeyEvent.KEYCODE_3, "3", 0.84f, 0.10f, ActionType.TAP, BindingCategory.COMBAT),
            KeyBinding("medkit", "Medkit / Heal", KeyEvent.KEYCODE_4, "4", 0.10f, 0.58f, ActionType.TAP, BindingCategory.INVENTORY),
            KeyBinding("gloo_wall", "Gloo Wall / Grenade", KeyEvent.KEYCODE_G, "G", 0.12f, 0.72f, ActionType.TAP, BindingCategory.TACTICAL),
            KeyBinding("skill", "Active Character Skill", KeyEvent.KEYCODE_E, "E", 0.62f, 0.68f, ActionType.TAP, BindingCategory.TACTICAL),
            KeyBinding("backpack", "Open Backpack", KeyEvent.KEYCODE_TAB, "Tab", 0.08f, 0.85f, ActionType.TAP, BindingCategory.INVENTORY),
            KeyBinding("loot", "Quick Loot / Revive", KeyEvent.KEYCODE_F, "F", 0.68f, 0.48f, ActionType.TAP, BindingCategory.TACTICAL),
            KeyBinding("map", "Tactical Map", KeyEvent.KEYCODE_M, "M", 0.10f, 0.14f, ActionType.TAP, BindingCategory.TACTICAL),
            KeyBinding("drive", "Enter Vehicle / Drive", KeyEvent.KEYCODE_V, "V", 0.68f, 0.36f, ActionType.TAP, BindingCategory.MOVEMENT)
        )

        val joystick = JoystickConfig(
            enabled = true,
            centerX = 0.18f,
            centerY = 0.72f,
            radius = 0.08f,
            upKey = KeyEvent.KEYCODE_W,
            downKey = KeyEvent.KEYCODE_S,
            leftKey = KeyEvent.KEYCODE_A,
            rightKey = KeyEvent.KEYCODE_D,
            sprintKey = KeyEvent.KEYCODE_SHIFT_LEFT
        )

        val camera = CameraSwipeConfig(
            sensitivityX = 1.0f,
            sensitivityY = 0.85f,
            invertY = false,
            deadzone = 1.2f,
            smoothingFactor = 0.85f,
            zoneLeft = 0.50f,
            zoneTop = 0.15f,
            zoneRight = 0.95f,
            zoneBottom = 0.85f,
            lockToggleKeyCode = KeyEvent.KEYCODE_GRAVE,
            lockToggleKeyLabel = "~ (Tilde)"
        )

        return MappingProfile(
            id = "freefire_br_default",
            name = "Free Fire - Battle Royale",
            description = "Optimized HUD layout for Free Fire Battle Royale with instant Gloo Wall and mouse look.",
            isDefault = true,
            bindings = bindings,
            joystick = joystick,
            camera = camera,
            floatingOverlayEnabled = false,
            overlayOpacity = 0.70f
        )
    }

    fun getClashSquadProfile(): MappingProfile {
        val br = getFreeFireDefaultProfile()
        val clashBindings = br.bindings.toMutableList().apply {
            add(KeyBinding("shop", "Shop / Buy Gun", KeyEvent.KEYCODE_B, "B", 0.50f, 0.85f, ActionType.TAP, BindingCategory.TACTICAL))
            add(KeyBinding("ping", "Enemy Ping", KeyEvent.KEYCODE_X, "X", 0.50f, 0.22f, ActionType.TAP, BindingCategory.TACTICAL))
        }
        return br.copy(
            id = "freefire_cs_default",
            name = "Free Fire - Clash Squad",
            description = "Preset for Clash Squad mode with instant buy menu and tactical pings.",
            isDefault = false,
            bindings = clashBindings
        )
    }

    private fun loadInitialProfiles(): List<MappingProfile> {
        val jsonStr = prefs.getString(PREF_PROFILES_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaults = listOf(getFreeFireDefaultProfile(), getClashSquadProfile())
            try {
                val array = JSONArray()
                defaults.forEach { array.put(it.toJsonString(0)) }
                prefs.edit().putString(PREF_PROFILES_JSON, array.toString()).apply()
            } catch (e: Exception) {
                // Ignore persistence error
            }
            return defaults
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MappingProfile>()
            for (i in 0 until array.length()) {
                list.add(MappingProfile.fromJsonString(array.getString(i)))
            }
            if (list.isEmpty()) {
                listOf(getFreeFireDefaultProfile())
            } else {
                list
            }
        } catch (e: Exception) {
            listOf(getFreeFireDefaultProfile())
        }
    }

    fun setActiveProfile(profileId: String) {
        val found = _profiles.value.find { it.id == profileId }
        if (found != null) {
            prefs.edit().putString(PREF_ACTIVE_PROFILE_ID, profileId).apply()
            _activeProfile.value = found
        }
    }

    fun saveActiveProfile(profile: MappingProfile) {
        val list = _profiles.value.toMutableList()
        val index = list.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            list[index] = profile
        } else {
            list.add(profile)
        }
        saveProfiles(list)
        _activeProfile.value = profile
        prefs.edit().putString(PREF_ACTIVE_PROFILE_ID, profile.id).apply()
    }

    fun updateBinding(binding: KeyBinding) {
        val current = _activeProfile.value
        val updatedBindings = current.bindings.toMutableList()
        val index = updatedBindings.indexOfFirst { it.id == binding.id }
        if (index != -1) {
            updatedBindings[index] = binding
        } else {
            updatedBindings.add(binding)
        }
        saveActiveProfile(current.copy(bindings = updatedBindings))
    }

    fun deleteBinding(bindingId: String) {
        val current = _activeProfile.value
        val updatedBindings = current.bindings.filter { it.id != bindingId }
        saveActiveProfile(current.copy(bindings = updatedBindings))
    }

    fun updateCameraConfig(cameraConfig: CameraSwipeConfig) {
        val current = _activeProfile.value
        saveActiveProfile(current.copy(camera = cameraConfig))
    }

    fun updateJoystickConfig(joystickConfig: JoystickConfig) {
        val current = _activeProfile.value
        saveActiveProfile(current.copy(joystick = joystickConfig))
    }

    fun updateGlobalButtonScale(scale: Float) {
        val current = _activeProfile.value
        saveActiveProfile(current.copy(globalButtonScale = scale))
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        val current = _activeProfile.value
        saveActiveProfile(current.copy(floatingOverlayEnabled = enabled))
    }

    fun resetToDefaults() {
        val defaults = listOf(getFreeFireDefaultProfile(), getClashSquadProfile())
        saveProfiles(defaults)
        _activeProfile.value = defaults.first()
        prefs.edit().putString(PREF_ACTIVE_PROFILE_ID, defaults.first().id).apply()
    }

    fun importProfile(jsonString: String): Result<MappingProfile> {
        return try {
            val imported = MappingProfile.fromJsonString(jsonString)
            val updated = imported.copy(id = "profile_${System.currentTimeMillis()}", isDefault = false)
            val list = _profiles.value.toMutableList()
            list.add(updated)
            saveProfiles(list)
            _activeProfile.value = updated
            prefs.edit().putString(PREF_ACTIVE_PROFILE_ID, updated.id).apply()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveProfiles(list: List<MappingProfile>) {
        try {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonString(0)) }
            prefs.edit().putString(PREF_PROFILES_JSON, array.toString()).apply()
        } catch (e: Exception) {
            // Ignore persistence error
        }
        _profiles.value = list
    }
}
