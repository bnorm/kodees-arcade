package dev.bnorm.arcade.service.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route

@ContributesIntoSet(AppScope::class)
class WebAppRouter : Router {
    context(route: Route)
    override fun route() {
        route.staticResources("/", basePackage = "webapp")
    }
}
