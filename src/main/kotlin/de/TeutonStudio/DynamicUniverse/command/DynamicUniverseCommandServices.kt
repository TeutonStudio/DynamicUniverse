package de.TeutonStudio.DynamicUniverse.command

import de.TeutonStudio.DynamicUniverse.command.service.RegistryUniverseQueryService
import de.TeutonStudio.DynamicUniverse.command.service.StackDimensionNavigationService
import de.TeutonStudio.DynamicUniverse.command.service.UniverseQueryService
import de.TeutonStudio.DynamicUniverse.command.service.DimensionNavigationService

/** Replaceable command-facing services for tests and future server adapters. */
object DynamicUniverseCommandServices {
    var universeQuery: UniverseQueryService = RegistryUniverseQueryService()
    var dimensionNavigation: DimensionNavigationService = StackDimensionNavigationService()
}
