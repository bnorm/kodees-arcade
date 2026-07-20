package dev.bnorm.arcade.web.route

import androidx.navigation3.runtime.NavKey
import com.github.terrakok.navigation3.browser.buildBrowserHistoryFragment
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentName
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentParameters
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.SeasonId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.uuid.Uuid

interface RouteKey : NavKey {
    fun buildFragment(): String
}

interface BaseRouteKey : RouteKey {
    fun parse(fragmentName: String, parameters: Map<String, String?>): RouteKey?
}

abstract class AbstractBaseRouteKey(
    private val fragment: String
) : BaseRouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment(fragment)
    }

    override fun parse(fragmentName: String, parameters: Map<String, String?>): RouteKey? {
        return when (fragmentName) {
            this.fragment -> route(parameters)
            else -> null
        }
    }

    open fun route(parameters: Map<String, String?>): RouteKey? {
        return this
    }
}

data object HomeKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("")
    }
}

@ContributesIntoSet(AppScope::class, binding<BaseRouteKey>())
data object DriversKey : AbstractBaseRouteKey("drivers")

@ContributesIntoSet(AppScope::class, binding<BaseRouteKey>())
data object RacesKey : AbstractBaseRouteKey("racers")

@ContributesIntoSet(AppScope::class, binding<BaseRouteKey>())
data object TracksKey : AbstractBaseRouteKey("tracks")

@ContributesIntoSet(AppScope::class, binding<BaseRouteKey>())
data object SeasonsKey : AbstractBaseRouteKey("seasons") {
    override fun route(parameters: Map<String, String?>): RouteKey? {
        val seasonId = parameters.seasonId
        return if (seasonId != null) {
            SeasonKey(seasonId)
        } else {
            SeasonsKey
        }
    }
}

data class SeasonKey(val id: SeasonId) : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("seasons", mapOf("id" to id.toString()))
    }
}

fun Set<BaseRouteKey>.route(fragment: String, default: RouteKey = HomeKey): RouteKey {
    val fragmentName = getBrowserHistoryFragmentName(fragment) ?: return default
    val parameters = getBrowserHistoryFragmentParameters(fragment)
    for (key in this) {
        val parsed = key.parse(fragmentName, parameters)
        if (parsed != null) return parsed
    }
    return default
}

private val Map<String, String?>.seasonId: SeasonId?
    get() = this["id"]?.let { Uuid.parseOrNull(it) }?.let { SeasonId(it) }

private val Map<String, String?>.raceId: RaceId?
    get() = this["id"]?.let { Uuid.parseOrNull(it) }?.let { RaceId(it) }

private val Map<String, String?>.driverId: DriverId?
    get() = this["id"]?.let { Uuid.parseOrNull(it) }?.let { DriverId(it) }
