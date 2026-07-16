package dev.bnorm.arcade.service.season

import dev.bnorm.arcade.service.race.RaceEntity
import dev.bnorm.arcade.service.race.RaceListener
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlin.math.pow

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class SeasonRaceProcessor(
    private val seasons: SeasonRepository,
) : RaceListener {
    override suspend fun onRaceComplete(entity: RaceEntity) {
        val participants = seasons.getParticipants(entity.id)
        val newScores = elo(
            current = participants.associate { it.driverVersionId to it.score },
            results = entity.drivers.associate { it.driverVersionId to it.result!! },
        )

        val participantByDriverVersionId = participants.associateBy { it.driverVersionId }
        for ((driverVersionId, score) in newScores) {
            val participant = participantByDriverVersionId[driverVersionId] ?: continue // TODO error?
            seasons.updateParticipant(participant.seasonId, participant.id, score)
        }
    }
}

// https://github.com/Kunal-Attri/ELO-Rating/blob/main/README.md
// https://gamedev.stackexchange.com/questions/55441/player-ranking-using-elo-with-more-than-two-players
private fun <K> elo(
    current: Map<K, Double>,
    results: Map<K, Double>,
    startingElo: Double = 1200.0,
    minimumElo: Double = 100.0,
): Map<K, Double> {
    val K = 32.0
    val c = 400

    return buildMap {
        for (name1 in results.keys) {
            var adjustment = 0.0
            val elo1 = current[name1] ?: startingElo
            val result1 = results.getValue(name1)

            for (name2 in results.keys) {
                if (name1 == name2) continue

                val elo2 = current[name2] ?: startingElo
                val result2 = results.getValue(name2)

                val E = 1.0 / (1.0 + 10.0.pow((elo2 - elo1) / c))
                val S = (1.0 - result1 / (result1 + result2)) // low score is better
                val R = elo1 + K * (S - E)

                adjustment += R - elo1
            }

            put(name1, (elo1 + adjustment).coerceAtLeast(minimumElo))
        }
    }
}
