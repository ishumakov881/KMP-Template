package net.lds.online.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [MarkerEntity::class, SiteEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(NosDatabaseConstructor::class)
abstract class NosDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    abstract fun siteDao(): SiteDao
}

@Suppress("KotlinNoActualForExpect")
expect object NosDatabaseConstructor : RoomDatabaseConstructor<NosDatabase> {
    override fun initialize(): NosDatabase
}

/** Common finish for platform [androidx.room.RoomDatabase.Builder]s. */
fun buildNosDatabase(builder: RoomDatabase.Builder<NosDatabase>): NosDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        // WAL: readers (Flow observeAll) + writers (replaceType) can overlap without SQLITE_BUSY.
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

const val NOS_DATABASE_FILE = "nos_offline.db"
