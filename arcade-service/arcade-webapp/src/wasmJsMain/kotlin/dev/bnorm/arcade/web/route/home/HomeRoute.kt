package dev.bnorm.arcade.web.route.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_webapp.generated.resources.Res
import dev.bnorm.arcade.arcade_webapp.generated.resources.icon
import dev.bnorm.arcade.web.route.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import org.jetbrains.compose.resources.painterResource

@ContributesIntoSet(AppScope::class)
class HomeRoute : Route {
    override val path: String get() = "/"

    @Composable
    override fun Content() {
        App()
    }
}

@Composable
fun App() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.widthIn(max = 1200.dp)) {
            Content()
        }
    }
}

@Composable
private fun Content() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(text = "Kodee's Arcade", style = MaterialTheme.typography.displayLarge)
        Image(painterResource(Res.drawable.icon), contentDescription = "Kodee's Arcade icon")
    }
}
