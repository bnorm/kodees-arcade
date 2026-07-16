package dev.bnorm.arcade.service.worker

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.logger
import dev.bnorm.arcade.service.race.RaceEntity
import dev.bnorm.arcade.service.race.RaceListener
import dev.bnorm.arcade.service.race.RaceRepository
import dev.bnorm.arcade.service.race.toResponse
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding<RaceListener>())
@ContributesIntoSet(AppScope::class, binding<Service>())
class RaceWorkerQueue(
    private val races: RaceRepository,
) : RaceListener, Service {
    companion object {
        private val log = logger<RaceWorkerQueue>()
    }

    private val semaphore = Semaphore(Int.MAX_VALUE, Int.MAX_VALUE)

    override suspend fun initialize() {
        for (entity in races.getIncompleteRaces()) {
            resetRace(entity.id)
        }
    }

    // TODO workers must specify processing limit?
    //  - can we use startRace as a trigger to send them another race?
    //  - then there is only ever one buffered race per worker
    fun listen(workerId: WorkerId): Flow<RaceProcessEvent> = flow {
        try {
            while (true) {
                semaphore.acquire()
                when (val race = races.acquireRace(workerId)) {
                    null -> error("able to acquire semaphore but unable to find race?")
                    else -> emit(RaceProcessEvent(race.id, race.nonce))
                }
            }
        } finally {
            log.info("worker listener closed")
            withContext(NonCancellable) {
                for (entity in races.getIncompleteRaces(workerId)) {
                    resetRace(entity.id)
                }
            }
        }
    }

    suspend fun resetRace(id: RaceId): RaceResponse? {
        log.info("resetting race $id")
        val entity = races.resetRace(id) ?: return null
        semaphore.release()
        return entity.toResponse()
    }

    override suspend fun onRaceCreated(entity: RaceEntity) {
        semaphore.release()
    }
}
