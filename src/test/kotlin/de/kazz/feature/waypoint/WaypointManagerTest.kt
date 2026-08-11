package de.kazz.feature.waypoint

import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for waypoint serialization (codec roundtrip).
 *
 * Does NOT require a running Minecraft instance.
 */
class WaypointManagerTest {

    @Test
    fun `test Waypoint codec roundtrip`() {
        val original = Waypoint(
            name = "Spawn",
            world = "world",
            x = 0,
            y = 64,
            z = 0,
            color = 0xFFFF0000.toInt(),
            dimension = "overworld"
        )

        val json = Waypoint.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = Waypoint.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(original, decoded)
    }

    @Test
    fun `test Waypoint codec with default color and dimension`() {
        val original = Waypoint(
            name = "Home",
            world = "world",
            x = 100,
            y = 70,
            z = -200
        )

        val json = Waypoint.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = Waypoint.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(original, decoded)
        assertEquals("overworld", decoded.dimension)
    }

    @Test
    fun `test Waypoint list codec roundtrip`() {
        val waypoints = listOf(
            Waypoint("Spawn", "world", 0, 64, 0),
            Waypoint("Nether Hub", "world_nether", 10, 50, 20, dimension = "nether"),
            Waypoint("End Portal", "world", -200, 60, 300, dimension = "overworld")
        )

        val json = Waypoint.CODEC.listOf().encodeStart(JsonOps.INSTANCE, waypoints)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = Waypoint.CODEC.listOf().parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(waypoints, decoded)
    }

    @Test
    fun `test Waypoint with negative coordinates`() {
        val original = Waypoint("Deep Mine", "world", -150, -30, -300)

        val json = Waypoint.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result()
            .orElseThrow { IllegalStateException("Encode failed") }

        val decoded = Waypoint.CODEC.parse(JsonOps.INSTANCE, json)
            .result()
            .orElseThrow { IllegalStateException("Decode failed") }

        assertEquals(original, decoded)
    }
}