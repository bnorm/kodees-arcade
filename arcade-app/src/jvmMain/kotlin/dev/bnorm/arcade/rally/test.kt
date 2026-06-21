@file:OptIn(ExperimentalComposeUiApi::class)

package dev.bnorm.arcade.rally

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// !!!
// This whole file is me just messing around to get something working that I can test.
// !!!

// TODO support team racing?
//  could be cool for racers to try and assist each other

// TODO support heats and seasons?
//  same Wasm instance for racers through the heats or entire season
//  tracks change over the course of the season or repeat for heats

// TODO full F1 style season?
//  teams
//  qualifying
//  some sort of endurance aspect for the whole season

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp)
        ) {
            SampleRally()
        }
    }
}

