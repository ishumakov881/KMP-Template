package net.lds.online.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun nosDatabaseBuilder(
    directory: File = File(System.getProperty("user.home"), ".lds-nos"),
): RoomDatabase.Builder<NosDatabase> {
    if (!directory.exists()) directory.mkdirs()
    val dbFile = File(directory, NOS_DATABASE_FILE)
    return Room.databaseBuilder<NosDatabase>(
        name = dbFile.absolutePath,
    )
}

fun createNosDatabase(directory: File = File(System.getProperty("user.home"), ".lds-nos")): NosDatabase =
    buildNosDatabase(nosDatabaseBuilder(directory))
