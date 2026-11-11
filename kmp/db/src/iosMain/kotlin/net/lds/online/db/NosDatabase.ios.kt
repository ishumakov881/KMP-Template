package net.lds.online.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun nosDatabaseBuilder(): RoomDatabase.Builder<NosDatabase> {
    val dbFilePath = documentDirectory() + "/$NOS_DATABASE_FILE"
    return Room.databaseBuilder<NosDatabase>(
        name = dbFilePath,
    )
}

fun createNosDatabase(): NosDatabase =
    buildNosDatabase(nosDatabaseBuilder())

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
