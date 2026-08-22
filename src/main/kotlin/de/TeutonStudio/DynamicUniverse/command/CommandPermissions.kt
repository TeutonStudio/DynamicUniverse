package de.TeutonStudio.DynamicUniverse.command

import net.minecraft.commands.CommandSourceStack

/** Conservative permissions for server topology diagnostics and debug navigation. */
object CommandPermissions {
    const val INSPECT = 2
    const val NAVIGATE = 2

    fun mayInspect(source: CommandSourceStack): Boolean = source.hasPermission(INSPECT)
    fun mayNavigate(source: CommandSourceStack): Boolean = source.hasPermission(NAVIGATE)
}
