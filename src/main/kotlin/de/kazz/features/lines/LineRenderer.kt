package de.kazz.features.lines

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
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
import org.joml.Vector3d
import org.joml.Vector4f
import java.util.Optional
import java.util.OptionalDouble

/**
 * Custom render pipeline for in-world lines.
 *
 * Provides two rendering modes:
 * - **See-through**: Uses a custom pipeline without depth/stencil testing so the
 *   line is visible through walls.
 * - **Normal**: Uses the standard [RenderPipelines.DEBUG_FILLED_BOX] pipeline so the
 *   line is occluded by solid blocks.
 *
 * Each line renders as a rectangular prism oriented along the start→end vector,
 * either as a solid fill or as wireframe edges.
 */
object LineRenderer {

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
    private val MODEL_OFFSET = org.joml.Vector3f()
    private val TEXTURE_MATRIX = Matrix4f()

    /** Shared buffer for all line geometry. */
    private val stagedBuffer = StagedVertexBuffer(
        { "Line Buffer" },
        RenderType.SMALL_BUFFER_SIZE
    )

    // ── Extraction state ─────────────────────────────────────────────────

    /** Snapshot of lines to render this frame (set during extraction). */
    private var currentLines: List<Line> = emptyList()

    // ── Initialisation ───────────────────────────────────────────────────

    /**
     * Register the extraction and drawing event listeners.
     * Call this once during [net.fabricmc.api.ClientModInitializer.onInitializeClient].
     */
    fun initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(this::extractLines)
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::renderAndDrawLines)
    }

    // ── Extraction phase ─────────────────────────────────────────────────

    private fun extractLines(context: LevelExtractionContext) {
        // Snapshot the current lines (expired ones are pruned by the manager)
        currentLines = LineManager.getActiveLines()
    }

    // ── Drawing phase ────────────────────────────────────────────────────

    private fun renderAndDrawLines(context: LevelRenderContext) {
        if (currentLines.isEmpty()) return

        // Split into see-through and normal groups so we can batch by pipeline
        val seeThrough = currentLines.filter { it.seeThrough }
        val normal = currentLines.filter { !it.seeThrough }

        if (seeThrough.isNotEmpty()) {
            renderGroup(context, seeThrough, FILLED_THROUGH_WALLS)
        }
        if (normal.isNotEmpty()) {
            renderGroup(context, normal, RenderPipelines.DEBUG_FILLED_BOX)
        }
    }

    /**
     * Render a group of lines using the given [pipeline], then draw.
     */
    private fun renderGroup(context: LevelRenderContext, lines: List<Line>, pipeline: RenderPipeline) {
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

        for (line in lines) {
            renderLine(matrices, builder, line)
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
     * Render a single line into the vertex builder.
     */
    private fun renderLine(matrices: PoseStack, builder: VertexConsumer, line: Line) {
        val positionMatrix = matrices.last().pose()
        val r = line.color.red / 255f
        val g = line.color.green / 255f
        val b = line.color.blue / 255f
        val a = line.color.alpha / 255f

        // Compute the 8 corners of the oriented prism
        val corners = computePrismCorners(
            line.startX, line.startY, line.startZ,
            line.endX, line.endY, line.endZ,
            line.thickness
        )

        when (line.style) {
            LineStyle.SOLID -> renderSolidPrism(positionMatrix, builder, corners, r, g, b, a)
            LineStyle.WIREFRAME -> renderWireframePrism(positionMatrix, builder, corners, r, g, b, a)
        }
    }

    // ── Geometry computation ─────────────────────────────────────────────

    /**
     * Compute the 8 corners of a rectangular prism oriented along the
     * start→end vector with the given [thickness] as the cross-section size.
     *
     * Returns an array of 8 [Vector3d] corners in the following order:
     *   0-3: start face (bottom-left, bottom-right, top-right, top-left)
     *   4-7: end face   (bottom-left, bottom-right, top-right, top-left)
     */
    private fun computePrismCorners(
        startX: Double, startY: Double, startZ: Double,
        endX: Double, endY: Double, endZ: Double,
        thickness: Double,
    ): Array<Vector3d> {
        val dir = Vector3d(endX - startX, endY - startY, endZ - startZ)
        val length = dir.length()
        if (length < 1e-10) {
            // Degenerate: zero-length line, render a small box at the start point
            val h = thickness / 2.0
            return arrayOf(
                Vector3d(startX - h, startY - h, startZ - h),
                Vector3d(startX + h, startY - h, startZ - h),
                Vector3d(startX + h, startY + h, startZ - h),
                Vector3d(startX - h, startY + h, startZ - h),
                Vector3d(startX - h, startY - h, startZ + h),
                Vector3d(startX + h, startY - h, startZ + h),
                Vector3d(startX + h, startY + h, startZ + h),
                Vector3d(startX - h, startY + h, startZ + h),
            )
        }

        dir.div(length) // normalize

        // Find an up vector that is not parallel to dir
        val up = Vector3d(0.0, 1.0, 0.0)
        if (kotlin.math.abs(dir.dot(up)) > 0.99) {
            up.set(0.0, 0.0, 1.0) // use Z as up if dir is nearly vertical
        }

        // Compute right = dir × up, then actual up = right × dir
        val right = Vector3d()
        dir.cross(up, right)
        right.normalize()
        up.set(right).cross(dir, up)
        up.normalize()

        val half = thickness / 2.0
        right.mul(half)
        up.mul(half)

        // Start face center
        val sc = Vector3d(startX, startY, startZ)
        // End face center
        val ec = Vector3d(endX, endY, endZ)

        // 8 corners: start face then end face
        // Order: -right-up, +right-up, +right+up, -right+up
        // Note: Vector3d.add/sub mutate in-place, so we create fresh copies
        val nr = Vector3d(right).negate()
        val nu = Vector3d(up).negate()
        return arrayOf(
            // Start face (0-3)
            Vector3d(sc).add(nr).add(nu),
            Vector3d(sc).add(right).add(nu),
            Vector3d(sc).add(right).add(up),
            Vector3d(sc).add(nr).add(up),
            // End face (4-7)
            Vector3d(ec).add(nr).add(nu),
            Vector3d(ec).add(right).add(nu),
            Vector3d(ec).add(right).add(up),
            Vector3d(ec).add(nr).add(up),
        )
    }

    // ── SOLID rendering ──────────────────────────────────────────────────

    /**
     * Render a filled rectangular prism using 6 quads (24 vertices).
     */
    private fun renderSolidPrism(
        positionMatrix: Matrix4fc,
        buffer: VertexConsumer,
        corners: Array<Vector3d>,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        // Indices for the 6 faces (each as a quad of 4 corners)
        // Face order: front, back, left, right, top, bottom
        val faces = arrayOf(
            intArrayOf(0, 1, 2, 3), // start face
            intArrayOf(4, 5, 6, 7), // end face
            intArrayOf(0, 4, 7, 3), // left face
            intArrayOf(1, 5, 6, 2), // right face
            intArrayOf(3, 2, 6, 7), // top face
            intArrayOf(0, 1, 5, 4), // bottom face
        )

        for (face in faces) {
            val c0 = corners[face[0]]
            val c1 = corners[face[1]]
            val c2 = corners[face[2]]
            val c3 = corners[face[3]]

            buffer.addVertex(positionMatrix, c0.x.toFloat(), c0.y.toFloat(), c0.z.toFloat()).setColor(red, green, blue, alpha)
            buffer.addVertex(positionMatrix, c1.x.toFloat(), c1.y.toFloat(), c1.z.toFloat()).setColor(red, green, blue, alpha)
            buffer.addVertex(positionMatrix, c2.x.toFloat(), c2.y.toFloat(), c2.z.toFloat()).setColor(red, green, blue, alpha)
            buffer.addVertex(positionMatrix, c3.x.toFloat(), c3.y.toFloat(), c3.z.toFloat()).setColor(red, green, blue, alpha)
        }
    }

    // ── WIREFRAME rendering ──────────────────────────────────────────────

    /**
     * Render the 12 edges of the prism as thin boxes (each edge is a small
     * rectangular prism of [thickness] × [thickness] × edgeLength).
     *
     * We reuse [renderSolidPrism] for each edge, computing the 8 corners of
     * a thin box between two corner points.
     */
    private fun renderWireframePrism(
        positionMatrix: Matrix4fc,
        buffer: VertexConsumer,
        corners: Array<Vector3d>,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        // The 12 edges of a rectangular prism (pairs of corner indices)
        val edges = arrayOf(
            // Start face edges
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
            // End face edges
            intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
            // Connecting edges
            intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7),
        )

        // Use a small fraction of thickness for the edge box cross-section
        // so wireframe edges are visible but thin relative to the line thickness
        val edgeThickness = lineThicknessForWireframe(corners)

        for (edge in edges) {
            val p1 = corners[edge[0]]
            val p2 = corners[edge[1]]

            val edgeCorners = computePrismCorners(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                edgeThickness
            )
            renderSolidPrism(positionMatrix, buffer, edgeCorners, red, green, blue, alpha)
        }
    }

    /**
     * Determine the edge thickness for wireframe rendering.
     * We use a fixed small value (0.02 blocks) so wireframe edges are visible
     * but don't overwhelm the visual. The user's `thickness` parameter controls
     * the overall prism size; wireframe edges are a visual representation of
     * that prism's boundaries.
     */
    private fun lineThicknessForWireframe(corners: Array<Vector3d>): Double {
        // Estimate the prism size from the first edge
        val dx = corners[1].x - corners[0].x
        val dy = corners[1].y - corners[0].y
        val dz = corners[1].z - corners[0].z
        val faceWidth = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        // Edge thickness is a fraction of the face width, clamped to a reasonable range
        return (faceWidth * 0.15).coerceIn(0.01, 0.1)
    }

    // ── GPU draw call ────────────────────────────────────────────────────

    private fun drawToScreen(client: Minecraft, info: StagedVertexBuffer.ExecuteInfo, pipeline: RenderPipeline) {
        val dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX)

        val mainTarget = client.gameRenderer.mainRenderTarget()
        val colorTexture = mainTarget.getColorTextureView()

        if (colorTexture == null) return

        val renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "kazzutils line rendering" },
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