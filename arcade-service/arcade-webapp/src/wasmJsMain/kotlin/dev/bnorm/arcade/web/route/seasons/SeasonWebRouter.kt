package dev.bnorm.arcade.web.route.seasons

import androidx.compose.runtime.Composable
import app.softwork.routingcompose.RouteBuilder
import app.softwork.routingcompose.Router
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.web.route.WebRouter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@Composable
fun RouteBuilder.seasonId(nestedRoute: @Composable RouteBuilder.(SeasonId) -> Unit) {
    uuid { nestedRoute(SeasonId(it)) }
}

@ContributesIntoSet(AppScope::class)
class SeasonWebRouter(
    private val seasonsScreen: SeasonsScreen,
    private val seasonScreen: SeasonScreen,
) : WebRouter {
    @Composable
    override fun RouteBuilder.Route() {
        route("/seasons") {
            seasonId { seasonId ->
                seasonScreen.Content(seasonId)
            }
            route("/") {
                parameters
                seasonsScreen.Content()
            }
            noMatch {
                Router.current.navigate(to = "/", replace = true)
            }
        }
    }
}
