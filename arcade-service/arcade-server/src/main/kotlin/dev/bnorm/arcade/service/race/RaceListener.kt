package dev.bnorm.arcade.service.race

interface RaceListener {
    suspend fun onRaceCreated(entity: RaceEntity) {}
    suspend fun onRaceComplete(entity: RaceEntity) {}
}
