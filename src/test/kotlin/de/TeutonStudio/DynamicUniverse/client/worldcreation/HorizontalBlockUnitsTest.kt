package de.TeutonStudio.DynamicUniverse.client.worldcreation

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class HorizontalBlockUnitsTest {
    @Test
    fun `formats counts as horizontal blocks with decimal large-number units`() {
        assertEquals("999 horizontale Blöcke", HorizontalBlockUnits.format(BigInteger.valueOf(999)))
        assertEquals("1,50 Tsd. horizontale Blöcke", HorizontalBlockUnits.format(BigInteger.valueOf(1_500)))
        assertEquals("68,72 Mia horizontale Blöcke", HorizontalBlockUnits.format(BigInteger.valueOf(68_719_476_736L)))
        assertEquals("1,00 Tria horizontale Blöcke", HorizontalBlockUnits.format(BigInteger.TEN.pow(21)))
    }
}
