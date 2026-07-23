package dev.bnorm.arcade.geometry

interface Boundable {
    val bounds: Rectangle
}

fun Collection<Boundable>.merge(): Boundable {
    val boundables = this
    return when (boundables.size) {
        0 -> throw IllegalArgumentException("must contain at least one boundable")
        1 -> boundables.first()
        else -> IterableBoundable(
            buildList(capacity = boundables.size) {
                for (boundable in boundables) {
                    if (boundable is IterableBoundable) {
                        addAll(boundable.boundables)
                    } else {
                        add(boundable)
                    }
                }
            }
        )
    }
}

private class IterableBoundable(
    val boundables: List<Boundable>
) : Boundable {
    override val bounds: Rectangle by lazy {
        val bounds = boundables.map { it.bounds }
        Rectangle(
            minX = bounds.minOf { it.minX },
            minY = bounds.minOf { it.minY },
            maxX = bounds.maxOf { it.maxX },
            maxY = bounds.maxOf { it.maxY },
        )
    }
}
