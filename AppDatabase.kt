package com.example.whatsappcatcher

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "captured_items")
data class CapturedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val sender: String = "",
    val text: String = "",
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CapturedDao {
    @Insert suspend fun insert(item: CapturedItem)
    @Query("SELECT * FROM captured_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CapturedItem>>
    @Query("DELETE FROM captured_items WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM captured_items")
    suspend fun deleteAll()
}

@Database(entities = [CapturedItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CapturedDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "captured_items.db"
                ).build().also { INSTANCE = it }
            }
    }
}
