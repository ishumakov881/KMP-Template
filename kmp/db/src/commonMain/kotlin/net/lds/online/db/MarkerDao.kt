package net.lds.online.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers")
    fun observeAll(): Flow<List<MarkerEntity>>

    @Query("SELECT * FROM markers WHERE type = :type")
    fun observeByType(type: String): Flow<List<MarkerEntity>>

    @Query("DELETE FROM markers WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<MarkerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: MarkerEntity)

    /**
     * Full markgen snapshot for one ajax `type`: wipe that type only, then insert batch.
     * Other types and single-row upserts stay intact.
     */
    @Transaction
    suspend fun replaceType(type: String, rows: List<MarkerEntity>) {
        deleteByType(type)
        if (rows.isNotEmpty()) insertAll(rows)
    }
}
