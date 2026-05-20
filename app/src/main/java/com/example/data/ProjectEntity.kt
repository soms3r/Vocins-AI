package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val inputText: String,
    val inputType: String, // "Script", "Story", "Timeline", "Subtitle"
    val durationMode: String, // "Auto", "Custom"
    val durationSeconds: Int,
    val intensity: String, // "Low", "Medium", "High"
    val atmosphere: String, // "Natural", "Spiritual", "Cinematic", "Dark", "Documentary"
    val voiceStyle: String, // "Male Humming", "Female Humming", "Mixed Choir", "Breath Atmosphere", "Ambient Voices"
    val natureBlend: Int, // 0 - 100
    val fxBlend: Int, // 0 - 100
    val blueprintJson: String, // JSON array of scenes
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null // Saved WAV file location
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atmosforge_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
