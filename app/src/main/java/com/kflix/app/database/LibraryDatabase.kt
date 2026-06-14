package com.kflix.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kflix.app.database.dao.LibraryItemDao
import com.kflix.app.database.dao.PlaylistDao
import com.kflix.app.database.dao.PlaylistItemDao
import com.kflix.app.database.dao.ProfileDao
import com.kflix.app.database.dao.WatchRecordDao
import com.kflix.app.models.LibraryItem
import com.kflix.app.models.Playlist
import com.kflix.app.models.PlaylistItem
import com.kflix.app.models.Profile
import com.kflix.app.models.WatchRecord

@Database(
    entities = [LibraryItem::class, Playlist::class, PlaylistItem::class, Profile::class, WatchRecord::class],
    version = 6,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun profileDao(): ProfileDao
    abstract fun watchRecordDao(): WatchRecordDao

    companion object {

        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getInstance(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = LibraryDatabase::class.java,
                    name = "kflix_library.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
