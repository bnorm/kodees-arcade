package dev.bnorm.arcade.service.route

import io.ktor.http.Parameters
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.routing.Route
import kotlin.uuid.Uuid

interface Router {
    context(route: Route)
    fun route()

    fun Parameters.getUuid(name: String): Uuid {
        return Uuid.parse(this[name] ?: throw MissingRequestParameterException(name))
    }
}
