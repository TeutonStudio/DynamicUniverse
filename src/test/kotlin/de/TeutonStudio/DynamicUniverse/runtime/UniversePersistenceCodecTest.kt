package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.client.worldcreation.CelestialBodyKind
import de.TeutonStudio.DynamicUniverse.client.worldcreation.EditableGalaxy
import de.TeutonStudio.DynamicUniverse.client.worldcreation.EditablePlanet
import de.TeutonStudio.DynamicUniverse.client.worldcreation.EditableSolarSystem
import de.TeutonStudio.DynamicUniverse.client.worldcreation.EditableStar
import de.TeutonStudio.DynamicUniverse.client.worldcreation.EditableUniverse
import de.TeutonStudio.DynamicUniverse.client.worldcreation.UniverseWorldCreationDraft
import de.TeutonStudio.DynamicUniverse.client.worldcreation.toWorldType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UniversePersistenceDefinitionTest {
    @Test
    fun `persisted definition retains moon topology`() {
        val luna = EditablePlanet.default().withBodyKind(CelestialBodyKind.MOON).copy(name = "Luna")
        val worldType = UniverseWorldCreationDraft(
            EditableUniverse(listOf(EditableGalaxy("Test", listOf(
                EditableSolarSystem("Sol", EditableStar("Sol"), listOf(EditablePlanet.default().copy(moons = listOf(luna)))),
            )))),
        ).toWorldType("dynamicuniverse:test")
        val definition = PersistedUniverseDefinition(worldType = worldType)

        assertEquals(2, definition.worldType.galaxies.single().groups.single().allPlanets().size)
        assertEquals("dynamicuniverse:test", definition.worldType.id)
    }

    @Test
    fun `unknown save versions are rejected instead of silently changing topology`() {
        assertFailsWith<IllegalArgumentException> {
            PersistedUniverseDefinition(
                formatVersion = PersistedUniverseDefinition.CURRENT_FORMAT_VERSION + 1,
                worldType = UniverseWorldCreationDraft().toWorldType(),
            )
        }
    }
}
