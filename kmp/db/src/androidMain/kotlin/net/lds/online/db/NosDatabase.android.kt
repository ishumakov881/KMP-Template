package net.lds.online.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun nosDatabaseBuilder(context: Context): RoomDatabase.Builder<NosDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(NOS_DATABASE_FILE)
    return Room.databaseBuilder<NosDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}

/**
 * Process-wide singleton — a second [Room.databaseBuilder] on the same file
 * without closing the first opens a second connection → `database is locked`.
 */
@Volatile
private var nosDatabaseInstance: NosDatabase? = null

fun createNosDatabase(context: Context): NosDatabase {
    nosDatabaseInstance?.let { return it }
    return synchronized(NosDatabase::class.java) {
        nosDatabaseInstance?.let { return it }
        buildNosDatabase(nosDatabaseBuilder(context)).also { nosDatabaseInstance = it }
    }
}