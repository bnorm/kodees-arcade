package dev.bnorm.arcade.gradle.player

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

abstract class ArcadeDriverExtension(
    objectFactory: ObjectFactory
) {
    val className: Property<String> = objectFactory.property(String::class.java)
}
