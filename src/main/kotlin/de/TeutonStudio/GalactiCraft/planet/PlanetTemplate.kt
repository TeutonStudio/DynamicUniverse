package de.TeutonStudio.GalactiCraft.planet

import de.TeutonStudio.GalactiCraft.cosmos.CelestialBody
import de.TeutonStudio.GalactiCraft.dimension.DimensionStack
import de.TeutonStudio.GalactiCraft.worldtype.PlanetWorldType

data class PlanetTemplate(
    val id: String,
    val body: CelestialBody,
    val stacks: List<DimensionStack>,
    val worldType: PlanetWorldType = PlanetWorldType.GALACTICRAFT_PLANETARY,
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Planet id must be namespaced." }
        require(stacks.isNotEmpty()) { "A planet needs at least one dimension stack." }
        require(stacks.map { it.id }.distinct().size == stacks.size) { "Stack ids must be unique per planet." }
    }
}

object StandardPlanetTemplates {
    val earth = PlanetTemplate(
        id = "galacticraft:earth",
        body = CelestialBody.earth("galacticraft:earth"),
        stacks = listOf(
            DimensionStack(
                id = "main",
                layersInnerToOuter = listOf(
                    layer("core", LayerRole.PLANET_CORE, "minecraft:the_nether", 8),
                    layer("deep_nether", LayerRole.DEEP_NETHER, "minecraft:the_nether", 8),
                    layer("nether", LayerRole.NETHER, "minecraft:the_nether", 8),
                    layer("overworld", LayerRole.SURFACE, "minecraft:overworld", 8),
                    DimensionLayer("sky", LayerRole.SKY, "minecraft:overworld"),
                ),
            ),
        ),
    )

    private fun layer(id: String, role: LayerRole, template: String, scale: Long): DimensionLayer =
        DimensionLayer(id, role, template, ScaleRatio(scale))
}
