package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionRole
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType

/**
 * The world-generation part of a Universe creation request.
 *
 * Minecraft freezes the level-stem registry before it starts the server.  Keeping this
 * information separate from the runtime graph makes that boundary explicit: this plan is
 * consumed while the Create World UI still owns [net.minecraft.world.level.levelgen.WorldDimensions],
 * while [UniverseWorldCreationBridge] is called only after those levels exist.
 */
data class UniverseLevelStemPlan(
    val worldType: UniverseWorldType,
    val templates: Map<DimensionId, UniverseStemTemplate>,
    val bedrockPlanes: List<BedrockBoundaryPlane>,
) {
    init {
        require(templates.keys.size == templates.size) { "A DimensionId may only have one generator template." }
        require(bedrockPlanes.map { it.dimension to it.face }.distinct().size == bedrockPlanes.size) {
            "A dimension boundary may only declare one Bedrock plane."
        }
    }

    companion object {
        /**
         * Built-in alpha templates deliberately reuse vanilla, verified generators.
         * A dedicated custom generator can replace a template later without changing the
         * creation/runtime hand-off.
         */
        fun builtIns(worldType: UniverseWorldType): UniverseLevelStemPlan {
            val manifest = UniverseGeometryCompiler.compile(worldType)
            val templates = linkedMapOf<DimensionId, UniverseStemTemplate>()
            val planes = mutableListOf<BedrockBoundaryPlane>()

            manifest.layers.forEach { layer ->
                val template = when (layer.role) {
                    PlanetDimensionRole.PLANET_CORE -> UniverseStemTemplate.CORE
                    PlanetDimensionRole.INNER -> if (layer.dimension in EXTERNAL_DIMENSIONS) {
                        UniverseStemTemplate.EXTERNAL
                    } else {
                        UniverseStemTemplate.NETHER
                    }
                    PlanetDimensionRole.SURFACE,
                    PlanetDimensionRole.CUSTOM -> UniverseStemTemplate.OVERWORLD
                    PlanetDimensionRole.SKY -> if (layer.dimension in EXTERNAL_DIMENSIONS) {
                        UniverseStemTemplate.EXTERNAL
                    } else {
                        UniverseStemTemplate.VOID
                    }
                }
                templates.putIfAbsent(layer.dimension, template)

                when (layer.role) {
                    // A planet core uses a generated cubic Bedrock shell, not a horizontal
                    // Bedrock plane. Its aperture is handled from the deep-side boundary.
                    PlanetDimensionRole.PLANET_CORE -> Unit
                    // A shell is a Nether-generator instance with both verified planes.
                    PlanetDimensionRole.INNER -> {
                        planes += BedrockBoundaryPlane(layer.dimension, DimensionBoundaryFace.LOWER, NETHER_FLOOR_Y)
                        planes += BedrockBoundaryPlane(layer.dimension, DimensionBoundaryFace.UPPER, NETHER_CEILING_Y)
                    }
                    // The vanilla overworld noise generator has its lower Bedrock floor at -64.
                    PlanetDimensionRole.SURFACE -> planes += BedrockBoundaryPlane(
                        layer.dimension,
                        DimensionBoundaryFace.LOWER,
                        OVERWORLD_FLOOR_Y,
                    )
                    PlanetDimensionRole.SKY,
                    PlanetDimensionRole.CUSTOM -> Unit
                }
            }
            templates.putIfAbsent(worldType.universeDimension, UniverseStemTemplate.UNIVERSE_HOST)
            return UniverseLevelStemPlan(worldType, templates, planes)
        }

        const val NETHER_FLOOR_Y = 0
        const val NETHER_CEILING_Y = 127
        const val OVERWORLD_FLOOR_Y = -64

        /** Loaded mod dimensions whose level stem must never be replaced by a vanilla clone. */
        private val EXTERNAL_DIMENSIONS = setOf(
            DimensionId("undergarden:undergarden"),
            DimensionId("aether:the_aether"),
        )
    }
}

enum class UniverseStemTemplate { OVERWORLD, NETHER, CORE, VOID, EXTERNAL, UNIVERSE_HOST }
