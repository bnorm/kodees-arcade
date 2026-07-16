package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding<RaceListener>())
@ContributesIntoSet(AppScope::class, binding<Service>())
class RaceWorkerQueue(
    private val races: RaceRepository,
) : RaceListener, Service {
    companion object {
        private val log = logger<RaceWorkerQueue>()
    }

    // TODO umm... this needs to be better
    private val channel = Channel<RaceProcessEvent>(1_000)

    override suspend fun initialize() {
        for (entity in races.getIncompleteRaces()) {
            if (entity.endTime == null) {
                resetRace(entity.id)
            }
        }
    }

    fun listen(): Flow<RaceProcessEvent> = flow {
        // TODO require workerId
        //  - each worker will get it's own Channel with set capacity (10?)
        //  - the event is first added to the worker channel before emitted
        //  - events are removed from the channel when the race is completed
        //  - when this flow is cancelled, the worker channel is closed
        //  - unprocessed events in the worker channel are sent back to the main channel
        //  - this should provide durable race processing which survives worker disconnects
        // TODO should there be a worker count limit?
        for (event in channel) {
            emit(event)
        }
    }

    suspend fun resetRace(id: RaceId): RaceResponse? {
        val entity = races.resetRace(id) ?: return null
        submitRaceForProcessing(entity.id, entity.nonce)
        return entity.toResponse()
    }

    override suspend fun onRaceCreated(entity: RaceEntity) {
        submitRaceForProcessing(entity.id, entity.nonce)
    }

    private fun submitRaceForProcessing(id: RaceId, nonce: Nonce) {
        channel.trySend(RaceProcessEvent(id, nonce))
            .onClosed { TODO("should be impossible") }
            .onFailure { log.warn("large event processing backlog, could not reprocess race", it) }
    }
}
