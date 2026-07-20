package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable

@Serializable
data class Arc(
    val pivot: Point,
    val radius: Double,
    val start: Angle,
    val sweep: Angle,
) : Boundable {
    init {
        // TODO allow negative sweep?
        require(sweep >= Angle.ZERO)
    }

    operator fun contains(angle: Angle): Boolean {
        return angle - start < sweep
    }

    override val bounds: Rectangle
        get() {
            var minX = pivot.x - radius
            var maxX = pivot.x + radius
            var minY = pivot.y - radius
            var maxY = pivot.y + radius

            // If the arc is not a full circle...
            if (sweep < Angle.FULL_CIRCLE) {
                fun contains(angle: Angle): Boolean {
                    return (angle - start).toAbsolute() < sweep
                }

                val end = start + sweep
                fun closestTo(angle: Angle): Angle {
                    return if (abs((start - angle).toRelative()) < abs((end - angle).toRelative())) start else end
                }

                if (!contains(Angle.ZERO)) {
                    maxX = pivot.x + radius * cos(closestTo(Angle.ZERO))
                }
                if (!contains(Angle.QUARTER_CIRCLE)) {
                    maxY = pivot.y + radius * sin(closestTo(Angle.QUARTER_CIRCLE))
                }
                if (!contains(Angle.HALF_CIRCLE)) {
                    minX = pivot.x + radius * cos(closestTo(Angle.HALF_CIRCLE))
                }
                val threeQuarter = 3.0 * Angle.QUARTER_CIRCLE
                if (!contains(threeQuarter)) {
                    minY = pivot.y + radius * sin(closestTo(threeQuarter))
                }
            }

            return Rectangle(minX, minY, maxX, maxY)
        }

}
