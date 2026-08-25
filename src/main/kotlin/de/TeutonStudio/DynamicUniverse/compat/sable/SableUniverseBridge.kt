package de.TeutonStudio.DynamicUniverse.compat.sable

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.runtime.SablePlotyardReservation
import de.TeutonStudio.DynamicUniverse.runtime.UniverseHostLayout

/**
 * Optional Sable 2.0.3 seam. This deliberately references no Sable class, so saves load when
 * Sable is absent; a Sable-side adapter supplies poses and sublevel ids only when installed.
 */
class SableUniverseBridge(private val bindings: SableSublevelBindings = SableSublevelBindings()) {
    fun bind(binding: SableSublevelBinding) = bindings.bind(binding)
    fun unbind(id: SableSublevelId) = bindings.unbind(id)
    fun isAvailable() = runCatching { Class.forName("dev.ryanhcode.sable.api.Sable") }.isSuccess

    fun universePosition(id: SableSublevelId, local: Vector3, pose: SableSublevelPose, object_: CelestialSpatialObject): Vector3? =
        bindings.bindingFor(id)?.universePosition(local, pose, object_)

    /** Fails early if an active host bubble would consume Sable's dedicated Plotyard region. */
    fun reservePlotyard(layout: UniverseHostLayout, plotyard: SablePlotyardReservation): UniverseHostLayout =
        layout.copy(sablePlotyard = plotyard).also { require(it.sablePlotyard == plotyard) }
}
