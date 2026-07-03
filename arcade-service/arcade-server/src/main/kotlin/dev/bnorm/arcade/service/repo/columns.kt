package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.Version
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

fun Table.nioPath(name: String): Column<Path> {
    return text(name).transform(Paths::get, Any::toString)
}

fun Table.version(name: String): Column<Version> {
    return text(name).transform(Version::parse, Any::toString)
}

@Serializable
@JvmInline
value class BlobId(val uuid: Uuid) {
    companion object {
        fun generate(): BlobId = BlobId(Uuid.generateV7())
    }

    override fun toString(): String {
        return uuid.toString()
    }
}

fun Table.blobId(name: String): Column<BlobId> {
    return uuid(name).transform(::BlobId, BlobId::uuid)
}

fun Table.raceId(name: String): Column<RaceId> {
    return uuid(name).transform(::RaceId, RaceId::uuid)
}

fun Table.racerId(name: String): Column<RacerId> {
    return uuid(name).transform(::RacerId, RacerId::uuid)
}

@Serializable
@JvmInline
value class RacerVersionId(val uuid: Uuid) {
    companion object {
        fun generate(): RacerVersionId = RacerVersionId(Uuid.generateV7())
    }

    override fun toString(): String {
        return uuid.toString()
    }
}

fun Table.racerVersionId(name: String): Column<RacerVersionId> {
    return uuid(name).transform(::RacerVersionId, RacerVersionId::uuid)
}

fun Table.trackId(name: String): Column<TrackId> {
    return uuid(name).transform(::TrackId, TrackId::uuid)
}

fun Table.seasonId(name: String): Column<SeasonId> {
    return uuid(name).transform(::SeasonId, SeasonId::uuid)
}

fun Table.nonce(name: String): Column<Nonce> {
    return uuid(name).transform(::Nonce, Nonce::uuid)
}
