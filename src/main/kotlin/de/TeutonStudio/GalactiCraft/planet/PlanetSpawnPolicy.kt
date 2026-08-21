package de.TeutonStudio.GalactiCraft.planet

enum class PlanetSpawnPolicy {
    ADMINISTRATIVE_ONLY,
    ;

    fun permitsSurvivalCreation(): Boolean = false
}
