package com.lezzwatch.app.ui.navigation

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Channels : Destination("channels")
    data object Coffee : Destination("coffee")
    data object Settings : Destination("settings")
    data object About : Destination("about")
}
