package com.ultrastream.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Search : Screen("search")
    object Addons : Screen("addons")
    object Profile : Screen("profile")
    object Details : Screen("details/{id}/{type}") {
        fun pass(id: String, type: String) = "details/$id/$type"
    }
    object Player : Screen("player/{url}") {
        fun pass(url: String) = "player/$url"
    }
}
