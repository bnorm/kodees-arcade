package dev.bnorm.arcade.service.repo

interface Repository {
    suspend fun migrate()
}
