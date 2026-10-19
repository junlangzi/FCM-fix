package com.fcmguard.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val message: String,
    val type: String // SUCCESS, FAIL, INFO, SCREEN, SERVICE
)

@Entity(tableName = "disconnect_events")
data class DisconnectEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMs: Long? = null
)

@Dao
interface StatsDao {
    @Insert
    suspend fun insertLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getLogsSince(since: Long): Flow<List<ActivityLog>>

    @Query("DELETE FROM activity_logs WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)

    @Insert
    suspend fun insertDisconnect(event: DisconnectEvent)

    @Query("UPDATE disconnect_events SET endTime = :end, durationMs = :duration WHERE id = :id")
    suspend fun closeDisconnect(id: Long, end: Long, duration: Long)

    @Query("SELECT * FROM disconnect_events ORDER BY startTime DESC LIMIT 50")
    fun getDisconnectHistory(): Flow<List<DisconnectEvent>>

    @Query("SELECT COUNT(*) FROM disconnect_events WHERE startTime >= :since")
    fun getDisconnectCountSince(since: Long): Flow<Int>
}

@Database(entities = [ActivityLog::class, DisconnectEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fcm_guard.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
