package dev.bnorm.arcade.web.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import dev.bnorm.arcade.display.asset.icon.menu
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
    DismissibleNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            DismissibleDrawerSheet {
                Text(
                    text = "Arcade",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Drivers") },
                    selected = backStack.lastOrNull() == DriversKey,
                    onClick = { backStack.add(DriversKey) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Seasons") },
                    selected = backStack.lastOrNull() == SeasonsKey,
                    onClick = { backStack.add(SeasonsKey) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Tracks") },
                    selected = backStack.lastOrNull() == TracksKey,
                    onClick = { backStack.add(TracksKey) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                when (drawerState.currentValue) {
                                    DrawerValue.Closed -> drawerState.open()
                                    DrawerValue.Open -> drawerState.close()
                                }
                            }
                        }) {
                            Icon(
                                painter = rememberVectorPainter(menu),
                                contentDescription = null,
                                modifier = Modifier
                            )
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
            MaxWidthContent(
                modifier = Modifier
                    .padding(contentPadding),
            ) {
                content()
            }
        }
    }
}
