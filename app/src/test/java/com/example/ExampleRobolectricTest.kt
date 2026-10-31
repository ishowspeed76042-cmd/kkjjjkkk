package com.example

import com.example.model.ActionType
import com.example.model.BindingCategory
import com.example.model.KeyBinding
import com.example.model.MappingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testKeyBindingModel() {
    val binding = KeyBinding(
      id = "test_fire",
      name = "Shoot",
      keyCode = 1001,
      keyLabel = "L-Click",
      targetX = 0.8f,
      targetY = 0.6f,
      actionType = ActionType.TAP,
      category = BindingCategory.COMBAT,
      size = 1.2f
    )
    assertEquals("test_fire", binding.id)
    assertEquals("Shoot", binding.name)
    assertEquals(1.2f, binding.size, 0.01f)
    assertEquals("L-Click", binding.keyLabel)
  }

  @Test
  fun testCleanLayoutProfile() {
    val profile = MappingProfile(
      id = "clean",
      name = "Clean Layout",
      description = "0 buttons",
      bindings = emptyList()
    )
    assertEquals("clean", profile.id)
    assertTrue(profile.bindings.isEmpty())
  }
}

