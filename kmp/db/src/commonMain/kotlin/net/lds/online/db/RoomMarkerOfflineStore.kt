package net.lds.online.db

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.lds.online.network.nos.LeanMapMarker
import net.lds.online.network.nos.MarkerOfflineStore

/**
 * Real offline store — lives in `:kmp:db` **commonMain** and ships to all Room
 * targets (Android / iOS / Desktop JVM). JS/Wasm use [net.lds.online.network.nos.NoOpMarkerOfflineStore].
 *
 * All writes go through one [writeMutex] + [NonCancellable] so progressive markgen
 * `replaceType`, maintenance `upsert`, and job cancel cannot interleave mid-transaction
 * (BundledSQLite → `Error code: 5, database is locked`).
 */
class RoomMarkerOfflineStore(
    private val dao: MarkerDao,
) : MarkerOfflineStore {

    private val writeMutex = Mutex()

    override fun observeAll(): Flow<List<LeanMapMarker>> =
        dao.observeAll().map { rows -> rows.map { it.toLean() } }

    override suspend fun replaceType(type: String, markers: List<LeanMapMarker>) {
        val rows = markers.map { it.toEntity() }
        writeMutex.withLock {
            withContext(NonCancellable) {
                withSqliteBusyRetry {
                    dao.replaceType(type, rows)
                }
            }
        }
    }

    override suspend fun upsert(marker: LeanMapMarker) {
        val row = marker.toEntity()
        writeMutex.withLock {
            withContext(NonCancellable) {
                withSqliteBusyRetry {
                    dao.upsert(row)
                }
            }
        }
    }
}

/** Retry SQLITE_BUSY / "database is locked" a few times (WAL + mutex cover most cases). */
private suspend fun withSqliteBusyRetry(block: suspend () -> Unit) {
    var last: Throwable? = null
    repeat(WRITE_RETRY_ATTEMPTS) { attempt ->
        try {
            block()
            return
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            if (!t.isSqliteBusy()) throw t
            last = t
            delay(WRITE_RETRY_BASE_MS * (attempt + 1))
        }
    }
    throw checkNotNull(last)
}

private fun Throwable.isSqliteBusy(): Boolean {
    var cur: Throwable? = this
    while (cur != null) {
        val msg = cur.message.orEmpty()
        if (
            msg.contains("database is locked", ignoreCase = true) ||
            msg.contains("SQLITE_BUSY", ignoreCase = true) ||
            msg.contains("Error code: 5")
        ) {
            return true
        }
        cur = cur.cause
    }
    return false
}

private const val WRITE_RETRY_ATTEMPTS = 5
private const val WRITE_RETRY_BASE_MS = 40L

private fun LeanMapMarker.toEntity(): MarkerEntity {
    val id = MarkerEntity.stableId(
        type = type,
        entityId = entityId,
        ip = ip,
        lat = lat,
        lng = lng,
        name = name,
    )
    return MarkerEntity(
        stableId = id,
        type = type,
        name = name,
        lat = lat,
        lng = lng,
        state = state,
        layer = layer,
        icon = icon,
        ip = ip,
        pingType = pingType,
        siteId = siteId,
        siteLabel = siteLabel,
        place = place,
        deviceModel = deviceModel,
        serial = serial,
        mac = mac,
        maintenanceId = maintenanceId,
        maintenanceActive = maintenanceActive,
        maintenanceText = maintenanceText,
        entityId = entityId,
    )
}

private fun MarkerEntity.toLean(): LeanMapMarker =
    LeanMapMarker(
        name = name,
        lat = lat,
        lng = lng,
        state = state,
        layer = layer,
        icon = icon,
        type = type,
        ip = ip,
        pingType = pingType,
        siteId = siteId,
        siteLabel = siteLabel,
        place = place,
        deviceModel = deviceModel,
        serial = serial,
        mac = mac,
        maintenanceId = maintenanceId,
        maintenanceActive = maintenanceActive,
        maintenanceText = maintenanceText,
        entityId = entityId,
    )
