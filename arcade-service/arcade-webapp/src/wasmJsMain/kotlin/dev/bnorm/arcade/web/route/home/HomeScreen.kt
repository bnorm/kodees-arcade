package dev.bnorm.arcade.web.route.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_webapp.generated.resources.Res
import dev.bnorm.arcade.arcade_webapp.generated.resources.icon
import dev.bnorm.arcade.web.route.HomeKey
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.RouteScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass
import org.jetbrains.compose.resources.painterResource

@ContributesIntoSet(AppScope::class, binding<RouteScreen<RouteKey>>())
class HomeScreen : RouteScreen<HomeKey> {
    override val key: KClass<out HomeKey>
        get() = HomeKey::class

    @Composable
    override fun Content(key: HomeKey) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Image(painterResource(Res.drawable.icon), contentDescription = "Kodee's Arcade icon")
            Text(text = "(Coming Soon)", style = MaterialTheme.typography.titleMedium)
        }
    }
}
