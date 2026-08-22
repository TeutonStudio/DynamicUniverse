package de.TeutonStudio.DynamicUniverse.compat.sable

import de.TeutonStudio.DynamicUniverse.cosmos.CosmicSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.SpatialRotation
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3

/**
 * Stable identifier supplied by a Sable-facing adapter. This module does not load Sable classes,
 * so a normal DynamicUniverse server remains independent of the optional mod.
 */
@JvmInline
value class SableSublevelId(val value: String) {
    init { require(value.isNotBlank()) { "A Sable sublevel id must not be blank." } }
}

/** A local Sable pose supplied by an optional direct API adapter on the correct side of the mod boundary. */
data class SableSublevelPose(
    val positionInObject: Vector3,
    val orientationInObject: SpatialRotation = SpatialRotation.IDENTITY,
)

/**
 * Binds one moving Sable sublevel to an authoritative CosmicSpatialObject.
 *
 * Sable plot coordinates are never treated as UniverseSpace coordinates directly: callers must
 * project through this binding before doing gravity, collision, distance, or portal work.
 */
data class SableSublevelBinding(
    val sublevelId: SableSublevelId,
    val cosmicObjectId: String,
) {
    init { require(cosmicObjectId.isNotBlank()) { "A Sable binding needs a cosmic object id." } }

    fun universePosition(
        sublevelLocalPosition: Vector3,
        sublevelPose: SableSublevelPose,
        cosmicObject: CosmicSpatialObject,
    ): Vector3 {
        require(cosmicObject.id == cosmicObjectId) { "Sable binding was projected with the wrong cosmic object." }
        val objectLocalPosition = sublevelPose.positionInObject + sublevelPose.orientationInObject.rotate(sublevelLocalPosition)
        return cosmicObject.kinematics.position + cosmicObject.kinematics.orientation.rotate(objectLocalPosition)
    }
}

/** Registry seam for optional Sable adapters. It intentionally has no dependency on the Sable API. */
class SableSublevelBindings {
    private val bySublevel = linkedMapOf<SableSublevelId, SableSublevelBinding>()

    fun bind(binding: SableSublevelBinding) {
        require(binding.sublevelId !in bySublevel) { "Duplicate Sable sublevel binding: ${binding.sublevelId.value}" }
        bySublevel[binding.sublevelId] = binding
    }

    fun bindingFor(sublevelId: SableSublevelId): SableSublevelBinding? = bySublevel[sublevelId]

    fun unbind(sublevelId: SableSublevelId): SableSublevelBinding? = bySublevel.remove(sublevelId)
}
