package dev.bnorm.arcade.rally

interface Point {
    val x: Double
    val y: Double
}

fun Point(x: Double, y: Double): Point {
    class Impl(
        override val x: Double,
        override val y: Double,
    ) : Point {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Impl

            if (x != other.x) return false
            if (y != other.y) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            return result
        }

        override fun toString(): String {
            return "($x, $y)"
        }
    }

    return Impl(x, y)
}
