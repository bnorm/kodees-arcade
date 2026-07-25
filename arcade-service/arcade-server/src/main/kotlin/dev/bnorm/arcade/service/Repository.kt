package dev.bnorm.arcade.service

interface Repository {
    suspend fun migrate()
}
