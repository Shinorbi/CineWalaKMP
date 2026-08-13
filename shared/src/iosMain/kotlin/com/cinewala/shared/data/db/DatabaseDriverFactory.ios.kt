package com.cinewala.shared.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.cinewala.shared.db.CineWalaDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = CineWalaDatabase.Schema,
            name = "cinewala.db"
        )
    }
}