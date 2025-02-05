package io.eigr.synapsys.extensions.store.backends.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "actor_states")
data class ActorStateEntity(
    @PrimaryKey val id: String,
    val data: ByteArray
)

@Dao
interface ActorStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ActorStateEntity)

    @Query("SELECT * FROM actor_states WHERE id = :id")
    suspend fun load(id: String): ActorStateEntity?
}

@Database(entities = [ActorStateEntity::class], version = 1)
abstract class ActorStateDatabase : RoomDatabase() {
    abstract fun dao(): ActorStateDao
}
