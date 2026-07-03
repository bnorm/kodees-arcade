package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.Version
import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

fun Table.nioPath(name: String): Column<Path> {
    return text(name).transform(Paths::get, Any::toString)
}

fun Table.version(name: String): Column<Version> {
    return text(name).transform(Version::parse, Any::toString)
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

fun Table.trackId(name: String): Column<TrackId> {
    return uuid(name).transform(::TrackId, TrackId::uuid)
}

fun Table.nonce(name: String): Column<Nonce> {
    return uuid(name).transform(::Nonce, Nonce::uuid)
}
