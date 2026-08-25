package de.TeutonStudio.DynamicUniverse.compat.sable

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.SpatialRotation
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3

@JvmInline value class SableSublevelId(val value: String) { init { require(value.isNotBlank()) } }

/** Pose supplied by an optional Sable adapter; common/runtime code stays Sable-class-free. */
data class SableSublevelPose(val positionInObject: Vector3, val orientationInObject: SpatialRotation = SpatialRotation.IDENTITY)

data class SableSublevelBinding(val sublevelId: SableSublevelId, val cosmicObjectId: String) {
    init { require(cosmicObjectId.isNotBlank()) }
    fun universePosition(local: Vector3, pose: SableSublevelPose, object_: CelestialSpatialObject): Vector3 {
        require(object_.id == cosmicObjectId) { "Sable binding was projected with the wrong spatial object." }
        val objectLocal = pose.positionInObject + pose.orientationInObject.rotate(local)
        return object_.kinematics.position + object_.kinematics.orientation.rotate(objectLocal)
    }
}

class SableSublevelBindings {
    private val bySublevel = linkedMapOf<SableSublevelId, SableSublevelBinding>()
    fun bind(binding: SableSublevelBinding) { require(binding.sublevelId !in bySublevel) { "Duplicate Sable sublevel binding." }; bySublevel[binding.sublevelId] = binding }
    fun bindingFor(id: SableSublevelId): SableSublevelBinding? = bySublevel[id]
    fun unbind(id: SableSublevelId): SableSublevelBinding? = bySublevel.remove(id)
}
