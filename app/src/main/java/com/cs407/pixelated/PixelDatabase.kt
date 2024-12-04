package com.cs407.pixelated

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import java.util.Date

// Define your own @Entity, @Dao and @Database

// Define the User entity with a unique index on userName
@Entity(
    indices = [Index(
        value = ["userName"], unique = true
    )]
)
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val userName: String = ""
)

// Converter class to handle conversions
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return Date(value)
    }
    @TypeConverter
    fun dateToTimestamp(date:Date): Long {
        return date.time
    }
}

//
@Entity
data class ScoreboardInfo(
    @PrimaryKey(autoGenerate = true) val scoreboardId: Int = 0,
    val highestPacman: Int,
    val highestGalaga: Int,
    val highestCentipede: Int,
    val recentPacman: Int,
    val recentGalaga: Int,
    val recentCentipede: Int
)

@Entity(
    primaryKeys = ["userId", "scoreboardId"],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = ScoreboardInfo::class,
        parentColumns = ["scoreboardId"],
        childColumns = ["scoreboardId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserRelations(
    val userId: Int,
    val scoreboardId: Int
)

@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE userName = :name")
    suspend fun getByName(name: String): User

    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getById(id: String): User

    @Query("SELECT scoreboardId FROM UserRelations WHERE userId = :userId LIMIT 1")
    suspend fun getScoreboardIdByUserId(userId: Int): Int

    @Insert(entity = User::class)
    suspend fun insert(user: User)
}

@Dao
interface ScoreboardDao {
    @Query("SELECT * FROM ScoreboardInfo WHERE scoreboardId = :id")
    suspend fun getById(id: String): ScoreboardInfo

    @Upsert(entity = ScoreboardInfo::class)
    suspend fun upsert(score: ScoreboardInfo): Long

    @Query("SELECT scoreboardId FROM ScoreboardInfo WHERE rowid = :rowId")
    suspend fun getByRowId(rowId: Long): Int

    @Query("UPDATE ScoreboardInfo SET recentPacman = :score WHERE scoreboardId = :id")
    suspend fun updateRecentPacman(id: String, score: String)

    @Query("UPDATE ScoreboardInfo SET highestPacman = :score WHERE scoreboardId = :id")
    suspend fun updateHighestPacman(id: String, score: String)

    @Query("SELECT recentPacman FROM ScoreboardInfo WHERE scoreboardId = :scoreboardId")
    suspend fun getRecentScoreByScoreboardId(scoreboardId: Int): Int?

    @Query("SELECT highestPacman FROM ScoreboardInfo WHERE scoreboardId = :scoreboardId")
    suspend fun getHighscoreByScoreboardId(scoreboardId: Int): Int?

    @Insert(entity = ScoreboardInfo::class)
    suspend fun insert(score: ScoreboardInfo)

    @Insert
    suspend fun insertRelation(userAndScoreboard: UserRelations)

    @Transaction
    suspend fun upsertInfo(score: ScoreboardInfo, userId: Int) {
        val rowId = upsert(score)
        if (score.scoreboardId == 0) {
            val scoreboardId = getByRowId(rowId)
            //TODO dont have api key, 0 is placeholder
            insertRelation(UserRelations(userId, scoreboardId))
        }
    }
}

@Database(entities = [User::class, ScoreboardInfo::class, UserRelations::class], version = 2)
@TypeConverters(Converters::class)
abstract class PixelDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scoreboardDao(): ScoreboardDao

    companion object {
        @Volatile
        private var INSTANCE: PixelDatabase? = null
        fun getDatabase(context: Context): PixelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelDatabase::class.java,
                    "pixelDatabase",
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}