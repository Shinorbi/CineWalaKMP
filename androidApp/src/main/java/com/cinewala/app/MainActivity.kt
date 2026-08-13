package com.cinewala.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cinewala.shared.app.App
import com.cinewala.shared.data.db.DatabaseDriverFactory
import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.ui.theme.CineWalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatabaseProvider.init(DatabaseDriverFactory(applicationContext))
        setContent {
            CineWalaTheme {
                App()
            }
        }
    }
}
