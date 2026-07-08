package com.lunar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "ai_analysis_cache", primaryKeys = ["recordId", "category"])
data class AiAnalysisCacheEntity(
    val recordId: Long,
    val category: String,
    val content: String,
    val updateTime: Long
)

@Dao
interface AiAnalysisCacheDao {
    @Query("SELECT * FROM ai_analysis_cache WHERE recordId = :recordId AND category = :category LIMIT 1")
    suspend fun find(recordId: Long, category: String): AiAnalysisCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiAnalysisCacheEntity)
}

@Database(entities = [AiAnalysisCacheEntity::class], version = 1, exportSchema = false)
abstract class LunarLocalDatabase : RoomDatabase() {
    abstract fun aiAnalysisCacheDao(): AiAnalysisCacheDao

    companion object {
        @Volatile
        private var instance: LunarLocalDatabase? = null

        fun getInstance(context: Context): LunarLocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LunarLocalDatabase::class.java,
                    "lunar_local.db"
                ).build().also { instance = it }
            }
        }
    }
}
