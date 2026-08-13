package com.cinewala.shared.data.db

import com.cinewala.shared.db.CineWalaDatabase

/**
 * Holds the app-wide SQLDelight database and repository.
 * Must be initialized once at app startup from each platform.
 */
object DatabaseProvider {
    private var database: CineWalaDatabase? = null
    private var repository: WatchProgressRepository? = null

    fun init(driverFactory: DatabaseDriverFactory) {
        if (database == null) {
            val db = CineWalaDatabase(driverFactory.createDriver())
            database = db
            repository = WatchProgressRepository(db)
        }
    }

    fun getDatabase(): CineWalaDatabase {
        return database ?: error("DatabaseProvider not initialized. Call init() first.")
    }

    fun getRepository(): WatchProgressRepository {
        return repository ?: error("DatabaseProvider not initialized. Call init() first.")
    }
}