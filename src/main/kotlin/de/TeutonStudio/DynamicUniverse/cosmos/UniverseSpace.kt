package de.TeutonStudio.DynamicUniverse.cosmos

/** The authoritative, unbounded and directionless R³ behind the technical Minecraft host. */
data class UniverseSpace(val id: String) {
    init { require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe-space id must be namespaced." } }
    fun point(x: Double, y: Double, z: Double) = Vector3(x, y, z)
}

/** Static mapping metadata. Its moving anchor lives in UniverseRuntimeState, not world geometry. */
data class PlanetSpaceBinding(
    val planetId: String,
    val localSpaceId: String,
    val universeSpace: UniverseSpace,
    val localUnitsPerUniverseUnit: Double = 1.0,
) {
    init {
        require(planetId.isNotBlank() && localSpaceId.isNotBlank()) { "A planet-space binding needs ids." }
        require(localUnitsPerUniverseUnit > 0.0 && localUnitsPerUniverseUnit.isFinite()) { "Planet scale must be finite and positive." }
    }

    fun universePosition(local: Vector3, state: UniverseKinematicState): Vector3 =
        state.position + state.orientation.rotate(local * (1.0 / localUnitsPerUniverseUnit))

    fun localPosition(universe: Vector3, state: UniverseKinematicState): Vector3 =
        state.orientation.inverse().rotate(universe - state.position) * localUnitsPerUniverseUnit
}
