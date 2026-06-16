package com.streamflixreborn.streamflix.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.streamflixreborn.streamflix.database.dao.LibraryItemDao
import com.streamflixreborn.streamflix.database.dao.PlaylistDao
import com.streamflixreborn.streamflix.database.dao.PlaylistItemDao
import com.streamflixreborn.streamflix.database.dao.WatchRecordDao
import com.streamflixreborn.streamflix.models.LibraryItem
import com.streamflixreborn.streamflix.models.Playlist
import com.streamflixreborn.streamflix.models.PlaylistItem
import com.streamflixreborn.streamflix.models.WatchRecord

@Database(
    entities = [LibraryItem::class, Playlist::class, PlaylistItem::class, WatchRecord::class],
    version = 5,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun watchRecordDao(): WatchRecordDao

    companion object {

        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getInstance(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = LibraryDatabase::class.java,
                    name = "streamflix_library.db"
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
