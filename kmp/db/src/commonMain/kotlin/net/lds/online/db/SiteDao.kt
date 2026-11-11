package net.lds.online.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("DELETE FROM sites")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SiteEntity>)

    /** Replace entire `/sw` menu snapshot. */
    @Transaction
    suspend fun replaceAll(rows: List<SiteEntity>) {
        deleteAll()
        if (rows.isNotEmpty()) insertAll(rows)
    }
}
