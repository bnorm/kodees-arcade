package dev.bnorm.arcade.rally

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import dev.bnorm.arcade.app.AppGraph
import dev.zacsweers.metro.createGraphFactory

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("composeApp") {
        val scope = rememberCoroutineScope()
        val graph = remember {
            createGraphFactory<AppGraph.Factory>()
                .create(scope)
        }

        Column {
            graph.webMenu.Content()
            Spacer(Modifier.height(2.dp).fillMaxWidth().background(Color.Black))
            graph.gameScreen.Content()
        }
    }
}
