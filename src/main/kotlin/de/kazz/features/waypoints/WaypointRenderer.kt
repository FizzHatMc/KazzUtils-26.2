package de.kazz.features.waypoints

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.Optional
import java.util.OptionalDouble

/**
 * Custom render pipeline for in-world waypoints (block highlight + beam).
 *
 * Provides two rendering modes:
 * - **See-through**: Uses a custom pipeline without depth/stencil testing so the
 *   waypoint is visible through walls.
 * - **Normal**: Uses the standard [RenderPipelines.DEBUG_FILLED_BOX] pipeline so the
 *   waypoint is occluded by solid blocks.
 *
 * Each waypoint renders:
 * 1. A semi-transparent filled box (1×1×1) at the block position.
 * 2. A thin vertical beam (0.1×0.1 cross-section) extending upward from the block.
 */
object WaypointRenderer {

    // ── Custom render pipelines ──────────────────────────────────────────

    /** Filled box that renders through walls (no depth/stencil test). */
    private val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("kazzutils", "pipeline/debug_filled_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build()
    )

    // ── Drawing constants ────────────────────────────────────────────────

    private val COLOR_MODULATOR = Vector4f(1f, 1f, 1f, 1f)
    private val MODEL_OFFSET = Vector3f()
    private val TEXTURE_MATRIX = Matrix4f()

    /** Shared buffer for all waypoint geometry. */
    private val stagedBuffer = StagedVertexBuffer(
        { "Waypoint Buffer" },
        RenderType.SMALL_BUFFER_SIZE
    )

    /** Height of the beam in blocks above the waypoint. */
    private const val BEAM_HEIGHT = 64.0

    /** Half-width of the beam cross-section (beam is 0.2×0.2 total). */
    private const val BEAM_HALF = 0.1

    // ── Extraction state ─────────────────────────────────────────────────

    /** Snapshot of waypoints to render this frame (set during extraction). */
    private var currentWaypoints: List<Waypoint> = emptyList()

    // ── Initialisation ───────────────────────────────────────────────────

    /**
     * Register the extraction and drawing event listeners.
     * Call this once during [net.fabricmc.api.ClientModInitializer.onInitializeClient].
     */
    fun initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(this::extractWaypoints)
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::renderAndDrawWaypoints)
    }

    // ── Extraction phase ─────────────────────────────────────────────────

    private fun extractWaypoints(context: LevelExtractionContext) {
        // Snapshot the current waypoints (expired ones are pruned by the manager)
        currentWaypoints = WaypointManager.getActiveWaypoints()
    }

    // ── Drawing phase ────────────────────────────────────────────────────

    private fun renderAndDrawWaypoints(context: LevelRenderContext) {
        if (currentWaypoints.isEmpty()) return

        // Split into see-through and normal groups so we can batch by pipeline
        val seeThrough = currentWaypoints.filter { it.seeThrough }
        val normal = currentWaypoints.filter { !it.seeThrough }

        if (seeThrough.isNotEmpty()) {
            renderGroup(context, seeThrough, FILLED_THROUGH_WALLS)
        }
        if (normal.isNotEmpty()) {
            renderGroup(context, normal, RenderPipelines.DEBUG_FILLED_BOX)
        }
    }

    /**
     * Render a group of waypoints using the given [pipeline], then draw.
     */
    private fun renderGroup(context: LevelRenderContext, waypoints: List<Waypoint>, pipeline: RenderPipeline) {
        val formatBinding = pipeline.getVertexFormatBinding(0)
            ?: return
        val primitive = pipeline.getPrimitiveTopology()
        val draw = stagedBuffer.appendDraw(
            formatBinding,
            primitive,
            if (primitive == PrimitiveTopology.QUADS) RenderSystem.getProjectionType().vertexSorting() else null
        )

        val matrices = context.poseStack()
        val camera = context.levelState().cameraRenderState.pos

        matrices.pushPose()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        val builder = stagedBuffer.getVertexBuilder(draw)

        for (wp in waypoints) {
            renderWaypoint(matrices, builder, wp)
        }

        matrices.popPose()

        stagedBuffer.upload()

        val info = stagedBuffer.getExecuteInfo(draw)
        if (info != null) {
            drawToScreen(Minecraft.getInstance(), info, pipeline)
        }

        stagedBuffer.endFrame()
    }

    /**
     * Render a single waypoint (block highlight + beam) into the vertex builder.
     */
    private fun renderWaypoint(matrices: PoseStack, builder: VertexConsumer, wp: Waypoint) {
        val positionMatrix = matrices.last().pose()
        val r = wp.color.red / 255f
        val g = wp.color.green / 255f
        val b = wp.color.blue / 255f
        val a = wp.color.alpha / 255f

        // Block centre → block-aligned floor position for the fill
        val blockX = wp.x.toInt()
        val blockY = wp.y.toInt()
        val blockZ = wp.z.toInt()

        // ── 1. Block highlight: semi-transparent filled box ──────────────
        renderFilledBox(
            positionMatrix, builder,
            blockX.toDouble(), blockY.toDouble(), blockZ.toDouble(),
            blockX + 1.0, blockY + 1.0, blockZ + 1.0,
            r, g, b, a * 0.4f,  // more transparent for the box
        )

        // ── 2. Beam: thin vertical column ────────────────────────────────
        val beamTop = blockY + BEAM_HEIGHT
        renderFilledBox(
            positionMatrix, builder,
            blockX + 0.5 - BEAM_HALF, blockY.toDouble(), blockZ + 0.5 - BEAM_HALF,
            blockX + 0.5 + BEAM_HALF, beamTop, blockZ + 0.5 + BEAM_HALF,
            r, g, b, a * 0.6f,  // slightly more opaque for the beam
        )
    }

    // ── Geometry helpers ─────────────────────────────────────────────────

    /**
     * Render an axis-aligned filled box (all 6 faces as quads).
     */
    private fun renderFilledBox(
        positionMatrix: Matrix4fc,
        buffer: VertexConsumer,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        // Front face
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)

        // Back face
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)

        // Left face
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)

        // Right face
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)

        // Top face
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)

        // Bottom face
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
        buffer.addVertex(positionMatrix, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(red, green, blue, alpha)
    }

    // ── GPU draw call ────────────────────────────────────────────────────

    private fun drawToScreen(client: Minecraft, info: StagedVertexBuffer.ExecuteInfo, pipeline: RenderPipeline) {
        val dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX)

        val mainTarget = client.gameRenderer.mainRenderTarget()
        val colorTexture = mainTarget.getColorTextureView()

        if (colorTexture == null) return

        val renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "kazzutils waypoint rendering" },
            colorTexture,
            Optional.empty(),
            mainTarget.getDepthTextureView(),
            OptionalDouble.empty()
        )
        try {
            renderPass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(renderPass)
            renderPass.setUniform("DynamicTransforms", dynamicTransforms)

            renderPass.setVertexBuffer(0, info.vertexBuffer().slice())
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType())
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0)
        } finally {
            renderPass.close()
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    /**
     * Release the staged vertex buffer.
     * Called from a [GameRenderer.close] mixin.
     */
    fun close() {
        stagedBuffer.close()
    }
}