package dev.bnorm.arcade.web.route

import androidx.compose.runtime.Composable
import app.softwork.routingcompose.RouteBuilder

interface WebRouter {
    @Composable
    fun RouteBuilder.Route()
}
