package dev.bnorm.arcade.web.route.seasons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.SeasonKey
import dev.bnorm.arcade.web.route.SeasonsKey
import dev.bnorm.arcade.web.components.MaxWidthContent
import dev.bnorm.arcade.web.route.RouteScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass

@ContributesIntoSet(AppScope::class, binding<RouteScreen<RouteKey>>())
class SeasonsScreen(
    private val client: ArcadeClient,
    private val backStack: NavBackStack<RouteKey>,
) : RouteScreen<SeasonsKey> {
    private val seasons = mutableStateListOf<SeasonResponse>()

    override val key: KClass<out SeasonsKey>
        get() = SeasonsKey::class

    @Composable
    override fun Content(key: SeasonsKey) {
        Content()
    }

    @Composable
    fun Content() {
        LaunchedEffect(Unit) {
            val apiSeasons = client.getSeasons()
            seasons.clear()
            seasons.addAll(apiSeasons)
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {

                    },
                    title = {
                        Text("Seasons")
                    },
                    actions = {

                    }
                )
            }
        ) { innerPadding ->
            MaxWidthContent(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    for (season in seasons) {
                        SeasonCard(
                            client = client,
                            season = season,
                            onClick = {
                                backStack.add(SeasonKey(season.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonCard(
    client: ArcadeClient,
    season: SeasonResponse,
    onClick: (SeasonResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick(season) }
                .padding(16.dp)
        ) {
            Text(season.name)
        }
    }
}
