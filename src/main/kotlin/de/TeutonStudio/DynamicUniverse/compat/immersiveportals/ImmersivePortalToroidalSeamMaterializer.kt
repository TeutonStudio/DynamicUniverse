package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.topology.ToroidalSeamSpec
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal

/** Optional, server-side materialization of the four portals described by a [ToroidalSeamSpec]. */
class ImmersivePortalToroidalSeamMaterializer {
    fun materialize(level: ServerLevel, seam: ToroidalSeamSpec): Int {
        val existing = WorldWrappingPortal.getWrappingZones(level).firstOrNull { zone ->
            val area = zone.area
            area.minX == seam.minX.toDouble() && area.minZ == seam.minZ.toDouble() &&
                area.maxX == seam.maxX.toDouble() && area.maxZ == seam.maxZ.toDouble()
        }
        if (existing != null) return existing.id

        val id = WorldWrappingPortal.getAvailableId(WorldWrappingPortal.getWrappingZones(level))
        WorldWrappingPortal.invokeAddWrappingZone(
            level,
            seam.minX,
            seam.minZ,
            seam.maxX,
            seam.maxZ,
            false,
        ) { message: Component -> level.server.sendSystemMessage(message) }
        return id
    }

    fun remove(level: ServerLevel, zoneId: Int) {
        WorldWrappingPortal.invokeRemoveWrappingZone(level, zoneId) { message: Component -> level.server.sendSystemMessage(message) }
    }
}
