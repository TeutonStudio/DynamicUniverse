package de.TeutonStudio.GalactiCraft.planet

/** Data-pack-backed templates will extend this catalog; built-ins keep the first server runnable. */
object PlanetTemplateCatalog {
    private val builtIns = listOf(StandardPlanetTemplates.earth).associateBy { it.id }

    fun resolve(id: String): PlanetTemplate? = builtIns[id]
}
