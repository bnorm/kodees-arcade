package dev.bnorm.arcade.web.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import dev.bnorm.arcade.web.route.DriversKey
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.SeasonsKey
import dev.bnorm.arcade.web.route.TracksKey
import kotlinx.coroutines.launch

@Composable
fun ArcadeScaffold(
    backStack: NavBackStack<RouteKey>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Button(onClick = {
                        scope.launch {
                            when (drawerState.currentValue) {
                                DrawerValue.Closed -> drawerState.open()
                                DrawerValue.Open -> drawerState.close()
                            }
                        }
                    }) {
                        Text("NAV") // TODO icon
                    }
                },
                title = {
                    Text("Kodee's Arcade")
                },
                actions = {

                }
            )
        }
    ) { contentPadding ->
        DismissibleNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier
                .padding(contentPadding),
            drawerContent = {
                Column {
                    Button(onClick = { backStack.add(DriversKey) }) {
                        Text("Drivers")
                    }
                    Button(onClick = { backStack.add(SeasonsKey) }) {
                        Text("Seasons")
                    }
                    Button(onClick = { backStack.add(TracksKey) }) {
                        Text("Tracks")
                    }
                }
            },
            content = {
                MaxWidthContent {
                    content()
                }
            },
        )
    }
}
