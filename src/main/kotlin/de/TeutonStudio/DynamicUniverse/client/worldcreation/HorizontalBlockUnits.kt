package de.TeutonStudio.DynamicUniverse.client.worldcreation

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Formats a count of horizontal block positions, not a physical square-metre conversion. */
object HorizontalBlockUnits {
    private val units = listOf(
        30 to "Quinto",
        27 to "Quadria",
        24 to "Quadro",
        21 to "Tria",
        18 to "Trio",
        15 to "Bia",
        12 to "Bio",
        9 to "Mia",
        6 to "Mio",
        3 to "Tsd.",
    )

    fun format(value: BigInteger): String {
        require(value.signum() >= 0) { "A horizontal block count cannot be negative." }
        val unit = units.firstOrNull { (exponent, _) -> value >= BigInteger.TEN.pow(exponent) }
            ?: return "$value horizontale Blöcke"
        val scaled = BigDecimal(value).divide(BigDecimal.TEN.pow(unit.first), 2, RoundingMode.HALF_UP)
        return "${scaled.toPlainString().replace('.', ',')} ${unit.second} horizontale Blöcke"
    }
}
