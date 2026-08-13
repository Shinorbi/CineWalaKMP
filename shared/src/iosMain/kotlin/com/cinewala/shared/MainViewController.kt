package com.cinewala.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.cinewala.shared.app.App
import com.cinewala.shared.data.db.DatabaseDriverFactory
import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.ui.theme.CineWalaTheme

fun MainViewController() = ComposeUIViewController {
    // Initialize the SQLite database; if it fails we still show the app
    // (Recents will simply remain empty) rather than crashing at startup.
    try {
        DatabaseProvider.init(DatabaseDriverFactory())
    } catch (e: Exception) {
        // Ignore DB init errors; the App handles a missing DB gracefully.
    }
    CineWalaTheme {
        App()
    }
}
