package net.lds.online.db

import android.content.Context
import net.lds.online.network.nos.MarkerOfflineStore

/** Android: Room commonMain store for the three Room platforms. */
fun createMarkerOfflineStore(context: Context): MarkerOfflineStore =
    RoomMarkerOfflineStore(createNosDatabase(context.applicationContext).markerDao())
