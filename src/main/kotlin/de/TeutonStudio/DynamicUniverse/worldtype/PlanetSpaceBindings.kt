package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace

/**
 * Creates the runtime bridge for a configured planet layer without making the world-creation
 * model the owner of the Universe simulation.
 */
fun Planet.spaceBinding(
    localSpaceId: String,
    universeSpace: UniverseSpace,
    kinematics: UniverseKinematicState,
    localUnitsPerUniverseUnit: Double = 1.0,
): PlanetSpaceBinding = PlanetSpaceBinding(
    planetId = id,
    localSpaceId = localSpaceId,
    universeSpace = universeSpace,
    kinematics = kinematics,
    localUnitsPerUniverseUnit = localUnitsPerUniverseUnit,
)
