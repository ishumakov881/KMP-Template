package net.lds.online.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached entry from web `/sw` left menu `#s_sw`. */
@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
