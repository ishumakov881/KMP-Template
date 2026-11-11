package net.lds.online.db

import net.lds.online.network.nos.MarkerOfflineStore

/** Desktop JVM: Room commonMain store. */
fun createMarkerOfflineStore(): MarkerOfflineStore =
    RoomMarkerOfflineStore(createNosDatabase().markerDao())
