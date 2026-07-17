package dev.bnorm.arcade.web.route

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.terrakok.navigation3.browser.buildBrowserHistoryFragment
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentName
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentParameters
import dev.bnorm.arcade.service.api.SeasonId
import kotlin.uuid.Uuid
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface RouteKey : NavKey {
    fun buildFragment(): String
}

@Serializable
data object HomeKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("")
    }
}

@Serializable
data object DriversKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("drivers")
    }
}

@Serializable
data object RacesKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("races")
    }
}

@Serializable
data object TracksKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("tracks")
    }
}

@Serializable
data object SeasonsKey : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("seasons")
    }
}

@Serializable
data class SeasonKey(val id: SeasonId) : RouteKey {
    override fun buildFragment(): String {
        return buildBrowserHistoryFragment("seasons", mapOf("id" to id.toString()))
    }
}

fun restoreKey(fragment: String): RouteKey {
    return when (getBrowserHistoryFragmentName(fragment)) {
        "drivers" -> DriversKey

        "races" -> RacesKey

        "seasons" -> {
            val parameters = getBrowserHistoryFragmentParameters(fragment)
            val seasonId = parameters["id"]?.let { Uuid.parseOrNull(it) }?.let { SeasonId(it) }
            if (seasonId != null) {
                SeasonKey(seasonId)
            } else {
                SeasonsKey
            }
        }

        "tracks" -> TracksKey

        else -> HomeKey
    }
}
