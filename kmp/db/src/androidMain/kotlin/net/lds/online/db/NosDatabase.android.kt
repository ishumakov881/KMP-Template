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

fun createNosDatabase(context: Context): NosDatabase =
    buildNosDatabase(nosDatabaseBuilder(context))
