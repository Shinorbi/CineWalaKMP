package com.cinewala.shared.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.cinewala.shared.db.CineWalaDatabase

actual class DatabaseDriverFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = CineWalaDatabase.Schema,
            context = context,
            name = "cinewala.db"
        )
    }
}