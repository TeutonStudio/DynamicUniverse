package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.cosmos.Vector3

/**
 * Converts synchronized UniverseSpace body state into a renderer-neutral draw plan.
 *
 * The caller supplies bubble-local camera coordinates, so a floating-origin rebase changes only
 * this plan's relative positions and never the persistent celestial coordinates.
 */
object GlobeRenderPlanner {
    fun plan(
        bodies: Collection<GlobeRenderBody>,
        cameraUniversePosition: Vector3,
        viewportHeightPixels: Int,
        verticalFovRadians: Double,
        maxLod: Int,
    ): List<GlobeRenderPass> {
        require(viewportHeightPixels > 0 && verticalFovRadians.isFinite() && verticalFovRadians > 0.0)
        return bodies.mapNotNull { body ->
            val relative = body.universePosition - cameraUniversePosition
            val distance = kotlin.math.sqrt(relative.lengthSquared())
            if (distance <= 0.0) return@mapNotNull null
            val projectedDiameter = (2.0 * body.visual.visualRadius / distance) *
                (viewportHeightPixels / (2.0 * kotlin.math.tan(verticalFovRadians / 2.0)))
            GlobeRenderPass(
                body = body,
                relativePosition = relative,
                projectedDiameterPixels = projectedDiameter.coerceAtLeast(0.0),
                lod = GlobeLodPolicy.lodFor(projectedDiameter.coerceAtLeast(0.0), maxLod),
            )
        }.sortedByDescending(GlobeRenderPass::projectedDiameterPixels)
    }
}

data class GlobeRenderPass(
    val body: GlobeRenderBody,
    val relativePosition: Vector3,
    val projectedDiameterPixels: Double,
    val lod: Int,
)
