package net.lds.online.db

import net.lds.online.network.nos.MarkerOfflineStore

/** iOS: Room commonMain store. */
fun createMarkerOfflineStore(): MarkerOfflineStore =
    RoomMarkerOfflineStore(createNosDatabase().markerDao())
