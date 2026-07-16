package dev.bnorm.arcade.web.route.seasons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.softwork.routingcompose.Router
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class SeasonsScreen(
    private val client: ArcadeClient,
) {
    private val seasons = mutableStateListOf<SeasonResponse>()

    @Composable
    fun Content() {
        val router = Router.current
        LaunchedEffect(Unit) {
            val apiSeasons = client.getSeasons()
            seasons.clear()
            seasons.addAll(apiSeasons)
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seasons", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                // TODO create season button
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(top = 16.dp)
            ) {
                for (season in seasons) {
                    SeasonCard(
                        client = client,
                        season = season,
                        onClick = {
                            router.navigate(to = "/seasons/${season.id}")
                        }
                    )
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
