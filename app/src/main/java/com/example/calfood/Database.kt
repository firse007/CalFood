package com.example.calfood

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val date: String,
    val totalCalories: Int
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int
)

// --- DAOs ---

@Dao
interface CalorieDao {
    @Query("SELECT * FROM daily_records ORDER BY date ASC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecordEntity)

    @Query("DELETE FROM daily_records")
    suspend fun deleteAll()
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY id DESC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int
}

// --- Database Configuration ---

@Database(entities = [DailyRecordEntity::class, FoodEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calorieDao(): CalorieDao
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calfood_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
