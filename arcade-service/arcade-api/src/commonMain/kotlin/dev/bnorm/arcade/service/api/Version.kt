package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class Version private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val classifier: String? = null,
) : Comparable<Version> {
    companion object {
        fun parse(version: String): Version {
            val classifierSplit = version.split('-')
            require(classifierSplit.size in 1..2)
            val versionSplit = classifierSplit[0].split('.')
            require(versionSplit.size in 1..3)

            val majorStr = versionSplit[0]
            val minorStr = versionSplit.getOrNull(1)
            val patchStr = versionSplit.getOrNull(2)

            val major = majorStr.toIntOrNull()
            val minor = minorStr?.toIntOrNull()
            val patch = patchStr?.toIntOrNull()

            require(major == null || major >= 0)
            require(minorStr == null || minor != null && minor >= 0)
            require(patchStr == null || patch != null && patch >= 0)

            val classifier = when {
                major == null -> version
                classifierSplit.size == 1 -> null
                /* classifierSplit.size == 2 */ else -> classifierSplit[1]
            }
            require(
                classifier?.any { it == '.' || it == '-' } != true &&
                    classifier?.firstOrNull()?.isDigit() != true
            )

            if (patch != null) {
                require(minor != null && major != null)
            } else if (minor != null) {
                require(major != null)
            } else if (major == null) {
                require(classifier != null)
            }

            return Version(
                major = major ?: -1,
                minor = minor ?: -1,
                patch = patch ?: -1,
                classifier = classifier,
            )
        }
    }

    override fun compareTo(other: Version): Int {
        var compare = compareValues(major, other.major)
        if (compare != 0) return compare
        compare = compareValues(minor, other.minor)
        if (compare != 0) return compare
        compare = compareValues(patch, other.patch)
        if (compare != 0) return compare
        return compareValues(classifier, other.classifier)
    }

    override fun toString(): String {
        return buildString {
            if (major >= 0) append(major)
            if (minor >= 0) append('.').append(minor)
            if (patch >= 0) append('.').append(patch)
            if (classifier != null) {
                if (isNotEmpty()) append('-')
                append(classifier)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
            other is Version &&
            major == other.major &&
            minor == other.minor &&
            patch == other.patch &&
            classifier == other.classifier
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + classifier.hashCode()
        return result
    }
}
