package dev.bnorm.arcade.geometry

data class Rectangle(
    val center: Point,
    val width: Double,
    val height: Double,
) {
    init {
        require(width > 0.0 && height > 0.0) { "width=$width height=$height" }
    }

    val xRange = (center.x - width / 2)..(center.x + width / 2)
    val yRange = (center.y - height / 2)..(center.y + height / 2)
}

operator fun Rectangle.contains(p: Point): Boolean {
    return p.x in xRange && p.y in yRange
}
