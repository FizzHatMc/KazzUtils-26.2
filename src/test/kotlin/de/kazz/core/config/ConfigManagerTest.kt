package de.kazz.core.config

import com.mojang.serialization.JsonOps
import net.minecraft.util.GsonHelper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Unit tests for config serialization and the [ConfigManager].
 */
class ConfigManagerTest {

    // ── ModColor tests ────────────────────────────────────────────

    @Test
    fun `test ModColor from ARGB`() {
        val c = ModColor.fromARGB(-0x10000) // 0xFFFF0000
        assertEquals(255, c.alpha)
        assertEquals(255, c.red)
        assertEquals(0, c.green)
        assertEquals(0, c.blue)
    }

    @Test
    fun `test ModColor from channels`() {
        val c = ModColor.fromChannels(128, 100, 150, 200)
        assertEquals(128, c.alpha)
        assertEquals(100, c.red)
        assertEquals(150, c.green)
        assertEquals(200, c.blue)
    }

    @Test
    fun `test ModColor from HSBA`() {
        val c = ModColor.fromHSBA(0f, 1f, 1f, 1f) // pure red
        assertEquals(255, c.red)
        assertEquals(0, c.green)
        assertEquals(0, c.blue)
        assertEquals(255, c.alpha)
    }

    @Test
    fun `test ModColor HSBA roundtrip`() {
        val original = ModColor.fromHSBA(120f, 0.8f, 0.6f, 0.7f)
        val map = original.toHSBAMap()
        val restored = ModColor.fromHSBA(
            (map["hue"] as Number).toFloat(),
            (map["saturation"] as Number).toFloat(),
            (map["brightness"] as Number).toFloat(),
            (map["alpha"] as Number).toFloat()
        )
        assertEquals(original, restored)
    }

    @Test
    fun `test ModColor predefined constants`() {
        assertEquals(-0x10000, ModColor.RED.argb)          // 0xFFFF0000
        assertEquals(-0xFF0100, ModColor.GREEN.argb)       // 0xFF00FF00
        assertEquals(-0xFFFF01, ModColor.BLUE.argb)        // 0xFF0000FF
        assertEquals(-0x1, ModColor.WHITE.argb)            // 0xFFFFFFFF
        assertEquals(-0x1000000, ModColor.BLACK.argb)      // 0xFF000000
        assertEquals(-0x100, ModColor.YELLOW.argb)         // 0xFFFFFF00
        assertEquals(0x00000000, ModColor.TRANSPARENT.argb)
    }

    @Test
    fun `test ModColor withAlpha creates new color`() {
        val c = ModColor.RED.withAlpha(64)
        assertEquals(64, c.alpha)
        assertEquals(255, c.red)
        assertEquals(0, c.green)
        assertEquals(0, c.blue)
    }

    @Test
    fun `test ModColor withHue changes hue`() {
        val c = ModColor.RED.withHue(240f) // rotate to blue
        assertEquals(240f, c.hue, 1f)
    }

    @Test
    fun `test ModColor withSaturation`() {
        val c = ModColor.RED.withSaturation(0.5f)
        assertEquals(0.5f, c.saturation, 0.01f)
    }

    @Test
    fun `test ModColor string representation`() {
        val c = ModColor.fromRGBA(255, 0, 0)
        assertTrue(c.toString().contains("FF"))
        assertTrue(c.toString().contains("00"))
    }

    @Test
    fun `test ModColor channel clamping`() {
        val c = ModColor.fromChannels(300, -50, 100, 200)
        assertEquals(255, c.alpha)
        assertEquals(0, c.red)
        assertEquals(100, c.green)
        assertEquals(200, c.blue)
    }

    // ── ConfigValue tests ────────────────────────────────────────

    @Test
    fun `test ConfigValue bool get and set`() {
        val v = ConfigValue("test_bool", true)
        assertEquals(true, v.get())
        v.set(false)
        assertEquals(false, v.get())
    }

    @Test
    fun `test ConfigValue int get and set`() {
        val v = ConfigValue("test_int", 42)
        assertEquals(42, v.get())
        v.set(100)
        assertEquals(100, v.get())
    }

    @Test
    fun `test ConfigValue double get and set`() {
        val v = ConfigValue("test_double", 3.14)
        assertEquals(3.14, v.get(), 0.001)
        v.set(2.71)
        assertEquals(2.71, v.get(), 0.001)
    }

    @Test
    fun `test ConfigValue string get and set`() {
        val v = ConfigValue("test_string", "hello")
        assertEquals("hello", v.get())
        v.set("world")
        assertEquals("world", v.get())
    }

    @Test
    fun `test ConfigValue color get and set`() {
        val v = ConfigValue("test_color", ModColor.RED)
        assertEquals(ModColor.RED, v.get())
        v.set(ModColor.GREEN)
        assertEquals(ModColor.GREEN, v.get())
    }

    @Test
    fun `test ConfigValue bool serializer roundtrip`() {
        val v = ConfigValue("test_bool", true)
        val raw = mutableMapOf<String, Any?>()
        v.saveTo(raw)
        assertEquals(true, raw["test_bool"])

        v.set(false)
        v.loadFrom(raw)
        assertEquals(true, v.get())
    }

    @Test
    fun `test ConfigValue color serializer roundtrip`() {
        val original = ModColor.fromHSBA(180f, 0.5f, 0.75f, 0.8f)
        val v = ConfigValue("test_color", original)
        val raw = mutableMapOf<String, Any?>()
        v.saveTo(raw)

        val loaded = raw["test_color"]
        assertTrue(loaded is Map<*, *>)
        val loadedMap = loaded as Map<String, Any>
        assertEquals(180.0, (loadedMap["hue"] as Number).toDouble(), 0.1)
        assertEquals(0.5, (loadedMap["saturation"] as Number).toDouble(), 0.01)
        assertEquals(0.75, (loadedMap["brightness"] as Number).toDouble(), 0.01)
        assertEquals(0.8, (loadedMap["alpha"] as Number).toDouble(), 0.01)
    }

    // ── Legacy tests (ElementPosition + ModConfig) ───────────────

    @Test
    fun `test ElementPosition codec roundtrip`() {
        val original = ElementPosition(10.0, 20.0, 100.0, 50.0, 1.5)

        val json = ElementPosition.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = ElementPosition.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(original, decoded)
    }

    @Test
    fun `test ElementPosition codec roundtrip with default scale`() {
        val original = ElementPosition(5.0, 5.0, 50.0, 20.0)

        val json = ElementPosition.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = ElementPosition.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(original, decoded)
    }

    @Test
    fun `test ModConfig codec roundtrip`() {
        val positions = mapOf(
            "player_stats" to ElementPosition(10.0, 10.0, 100.0, 20.0),
            "purse_display" to ElementPosition(200.0, 10.0, 120.0, 20.0)
        )
        val config = ModConfig(
            elementPositions = positions,
            enabledFeatures = setOf("waypoints", "hud"),
            keyBindings = mapOf("open_waypoints" to "key.keyboard.p")
        )

        val json = ModConfig.CODEC.encodeStart(JsonOps.INSTANCE, config)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = ModConfig.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(config, decoded)
    }

    @Test
    fun `test ModConfig defaults`() {
        val default = ModConfig()
        assertEquals(setOf("waypoints", "notifications", "hud"), default.enabledFeatures)
        assertTrue(default.elementPositions.isEmpty())
        assertTrue(default.keyBindings.isEmpty())
    }

    @Test
    fun `test ModConfig with empty features`() {
        val config = ModConfig(enabledFeatures = emptySet())
        assertTrue(config.enabledFeatures.isEmpty())
    }

    @Test
    fun `test serialized JSON can be parsed by GsonHelper`() {
        val config = ModConfig(
            enabledFeatures = setOf("waypoints"),
            keyBindings = mapOf("open_waypoints" to "key.keyboard.p")
        )

        val json = ModConfig.CODEC.encodeStart(JsonOps.INSTANCE, config)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val jsonString = GsonHelper.toStableString(json)
        val parsed = GsonHelper.parse(jsonString)

        val decoded = ModConfig.CODEC.parse(JsonOps.INSTANCE, parsed)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(config, decoded)
    }
}