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

@Entity
data class MapInfo(
    @PrimaryKey(autoGenerate = true) val mapId: Int = 0,
    val favoriteLocation: String
)

@Entity(
    primaryKeys = ["userId", "scoreboardId", "mapId"],
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
    ), ForeignKey(
        entity = MapInfo::class,
        parentColumns = ["mapId"],
        childColumns = ["mapId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserRelations(
    val userId: Int,
    val scoreboardId: Int,
    val mapId: Int
)

@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE userName = :name")
    suspend fun getByName(name: String): User

    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getById(id: String): User

    @Insert(entity = User::class)
    suspend fun insert(user: User)
}

@Dao
interface ScoreboardDao {
    @Query("SELECT * FROM ScoreboardInfo WHERE scoreboardId = :id")
    suspend fun getById(id: String): ScoreboardInfo

    @Insert(entity = ScoreboardInfo::class)
    suspend fun insert(score: ScoreboardInfo)
}

@Database(entities = [User::class, ScoreboardInfo::class, MapInfo::class, UserRelations::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}