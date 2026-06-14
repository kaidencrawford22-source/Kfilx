package com.kflix.app.backup

import android.content.Context
import android.util.Log
import androidx.room.Transaction
import com.kflix.app.database.AppDatabase
import com.kflix.app.database.LibraryDatabase
import com.kflix.app.database.dao.EpisodeDao
import com.kflix.app.database.dao.MovieDao
import com.kflix.app.database.dao.TvShowDao
import com.kflix.app.database.dao.SeasonDao
import com.kflix.app.models.Episode
import com.kflix.app.models.LibraryItem
import com.kflix.app.models.Movie
import com.kflix.app.models.Playlist
import com.kflix.app.models.PlaylistItem
import com.kflix.app.models.Profile
import com.kflix.app.models.Season
import com.kflix.app.models.TvShow
import com.kflix.app.models.WatchItem
import com.kflix.app.models.WatchRecord
import com.kflix.app.providers.Provider
import com.kflix.app.utils.UserDataCache
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProviderBackupContext(
    val name: String,
    val movieDao: MovieDao,
    val tvShowDao: TvShowDao,
    val episodeDao: EpisodeDao,
    val seasonDao: SeasonDao,
    val provider: Provider
)

class BackupRestoreManager(
    private val context: Context,
    private val providers: List<ProviderBackupContext>
) {
    private val TAG = "BackupVerify"

    suspend fun refreshCachesFromDatabase(): Boolean {
        return try {
            providers.forEach { buildCacheForProvider(it) }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Error refreshing caches from database", t)
            false
        }
    }

    fun exportDatabaseZip(): ByteArray? {
        return try {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                providers.forEach { providerCtx ->
                    addDatabaseFilesToZip(zip, providerCtx.name)
                }
                addLibraryDatabaseToZip(zip)
            }
            output.toByteArray()
        } catch (t: Throwable) {
            Log.e(TAG, "Error exporting database zip", t)
            null
        }
    }

    suspend fun importDatabaseZip(zipBytes: ByteArray): Boolean {
        return try {
            AppDatabase.resetInstance()
            LibraryDatabase.resetInstance()
            restoreDatabaseZip(ByteArrayInputStream(zipBytes))
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Error importing database zip", t)
            false
        }
    }

    fun exportUserData(): String? {
        return try {
            val root = JSONObject()
            root.put("version", 6)
            root.put("exportedAt", System.currentTimeMillis())

            val providersArray = JSONArray()
            for (p in providers) {
                val moviesToExport = p.movieDao.getAll()
                    .filter { it.isWatched || it.watchedDate != null || it.watchHistory != null || it.isFavorite }
                val tvShowsToExport = p.tvShowDao.getAllForBackup()
                    .filter { it.isWatching || it.isFavorite }
                val episodesToExport = p.episodeDao.getAllForBackup()
                    .filter { it.isWatched || it.watchedDate != null || it.watchHistory != null }

                if (moviesToExport.isEmpty() && tvShowsToExport.isEmpty() && episodesToExport.isEmpty()) {
                    continue
                }

                val providerObj = JSONObject()
                providerObj.put("name", p.name)

                val moviesArray = JSONArray()
                moviesToExport.forEach { movie ->
                    val obj = JSONObject().apply {
                        put("id", movie.id)
                        put("title", movie.title)
                        put("poster", movie.poster)
                        put("banner", movie.banner)
                        put("isFavorite", movie.isFavorite)
                        put("favoritedAtMillis", movie.favoritedAtMillis ?: JSONObject.NULL)
                        put("isWatched", movie.isWatched)
                        put("watchedDate", movie.watchedDate?.timeInMillis ?: JSONObject.NULL)
                        put("watchHistory", movie.watchHistory?.toJson() ?: JSONObject.NULL)
                    }
                    moviesArray.put(obj)
                    Log.d(TAG, "EXPORT: [${p.name}] Movie: ${movie.title} (Fav: ${movie.isFavorite})")
                }
                providerObj.put("movies", moviesArray)

                val tvShowsArray = JSONArray()
                tvShowsToExport.forEach { show ->
                    val obj = JSONObject().apply {
                        put("id", show.id)
                        put("title", show.title)
                        put("poster", show.poster)
                        put("banner", show.banner)
                        put("isFavorite", show.isFavorite)
                        put("favoritedAtMillis", show.favoritedAtMillis ?: JSONObject.NULL)
                        put("isWatching", show.isWatching)
                    }
                    tvShowsArray.put(obj)
                    Log.d(TAG, "EXPORT: [${p.name}] TV Show: ${show.title} (Fav: ${show.isFavorite})")
                }
                providerObj.put("tvShows", tvShowsArray)

                val seasonsArray = JSONArray()
                p.seasonDao.getAllForBackup()
                    .forEach { season ->
                        val obj = JSONObject().apply {
                            put("id", season.id)
                            put("number", season.number)
                            put("title", season.title)
                            put("poster", season.poster)
                            put("tvShowId", season.tvShow?.id)
                        }
                        seasonsArray.put(obj)
                    }
                providerObj.put("seasons", seasonsArray)

                val episodesArray = JSONArray()
                episodesToExport.forEach { ep ->
                    val obj = JSONObject().apply {
                        put("id", ep.id)
                        put("number", ep.number)
                        put("title", ep.title)
                        put("poster", ep.poster)
                        put("tvShowId", ep.tvShow?.id)
                        put("seasonId", ep.season?.id)
                        put("isWatched", ep.isWatched)
                        put("watchedDate", ep.watchedDate?.timeInMillis ?: JSONObject.NULL)
                        put("watchHistory", ep.watchHistory?.toJson() ?: JSONObject.NULL)
                    }
                    episodesArray.put(obj)
                    Log.d(TAG, "EXPORT: [${p.name}] Episode: ${ep.title} (Watched: ${ep.isWatched})")
                }
                providerObj.put("episodes", episodesArray)

                providersArray.put(providerObj)
            }

            root.put("providers", providersArray)

            val libraryDb = LibraryDatabase.getInstance(context)
            root.put("library", exportLibraryData(libraryDb))

            Log.d(TAG, "Export successful for version 5. Total providers exported: ${providersArray.length()}")
            root.toString()
        } catch (t: Throwable) {
            Log.e(TAG, "Error during exportUserData", t)
            null
        }
    }

    private fun exportLibraryData(libraryDb: LibraryDatabase): JSONObject {
        val lib = JSONObject()

        val profilesArray = JSONArray()
        libraryDb.profileDao().getAllSync().forEach { profile ->
            val obj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("iconRes", profile.iconRes)
                put("age", profile.age ?: JSONObject.NULL)
                put("isDefault", profile.isDefault)
                put("createdAt", profile.createdAt)
            }
            profilesArray.put(obj)
        }
        lib.put("profiles", profilesArray)

        val watchRecordsArray = JSONArray()
        libraryDb.watchRecordDao().getAllRecordsSync().forEach { r ->
            val obj = JSONObject().apply {
                put("contentId", r.contentId)
                put("title", r.title)
                put("poster", r.poster ?: JSONObject.NULL)
                put("type", r.type)
                put("providerName", r.providerName)
                put("profileId", r.profileId)
                put("watchedDateMillis", r.watchedDateMillis)
                put("durationMillis", r.durationMillis)
                put("showTitle", r.showTitle ?: JSONObject.NULL)
                put("seasonNumber", r.seasonNumber ?: JSONObject.NULL)
                put("episodeNumber", r.episodeNumber ?: JSONObject.NULL)
            }
            watchRecordsArray.put(obj)
        }
        lib.put("watchRecords", watchRecordsArray)

        val libraryItemsArray = JSONArray()
        libraryDb.libraryItemDao().getAllSync().forEach { item ->
            val obj = JSONObject().apply {
                put("contentId", item.contentId)
                put("title", item.title)
                put("type", item.type)
                put("poster", item.poster ?: JSONObject.NULL)
                put("banner", item.banner ?: JSONObject.NULL)
                put("overview", item.overview ?: JSONObject.NULL)
                put("rating", item.rating ?: JSONObject.NULL)
                put("year", item.year ?: JSONObject.NULL)
                put("profileId", item.profileId ?: JSONObject.NULL)
                put("addedAtMillis", item.addedAtMillis)
            }
            libraryItemsArray.put(obj)
        }
        lib.put("libraryItems", libraryItemsArray)

        val playlistsArray = JSONArray()
        libraryDb.playlistDao().getAllSync().forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("createdAt", p.createdAt)
            }
            playlistsArray.put(obj)
        }
        lib.put("playlists", playlistsArray)

        val playlistItemsArray = JSONArray()
        libraryDb.playlistItemDao().getAllSync().forEach { pi ->
            val obj = JSONObject().apply {
                put("id", pi.id)
                put("playlistId", pi.playlistId)
                put("contentId", pi.contentId)
                put("title", pi.title)
                put("poster", pi.poster ?: JSONObject.NULL)
                put("type", pi.type)
                put("position", pi.position)
                put("addedAt", pi.addedAt)
            }
            playlistItemsArray.put(obj)
        }
        lib.put("playlistItems", playlistItemsArray)

        return lib
    }

    @Transaction
    suspend fun importUserData(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val providersArray = obj.optJSONArray("providers") ?: return false
            val backupVersion = obj.optInt("version", 1)

            Log.d(TAG, "Starting import from version $backupVersion for ${providersArray.length()} providers")

            for (i in 0 until providersArray.length()) {
                val providerObj = providersArray.optJSONObject(i) ?: continue
                val providerName = providerObj.optString("name") ?: continue
                val providerCtx = providers.find { it.name == providerName }
                if (providerCtx == null) {
                    Log.w(TAG, "Provider '$providerName' not found in current providers. Skipping...")
                    continue
                }

                providerObj.optJSONArray("seasons")?.let { arr ->
                    val seasonsToSave = mutableListOf<Season>()
                    for (j in 0 until arr.length()) {
                        val s = arr.optJSONObject(j) ?: continue
                        val season = Season(
                            id = s.optString("id", ""),
                            number = s.optInt("number", 0)
                        ).apply {
                            title = s.optStringOrNull("title")
                            poster = s.optStringOrNull("poster")
                            s.optStringOrNull("tvShowId")?.let { tvId -> tvShow = TvShow(tvId, "") }
                        }
                        seasonsToSave.add(season)
                    }
                    if (seasonsToSave.isNotEmpty()) {
                        providerCtx.seasonDao.saveAll(seasonsToSave)
                        Log.d(TAG, "IMPORT: Imported ${seasonsToSave.size} seasons for provider $providerName")
                    }
                }

                providerObj.optJSONArray("tvShows")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        val s = arr.optJSONObject(j) ?: continue
                        val isFavorite = s.optBoolean("isFavorite", false)
                        val favoritedAtMillis = s.optLongOrNull("favoritedAtMillis")
                        val isWatching = s.optBoolean("isWatching", false)

                        val tvShow = TvShow(
                            id = s.optString("id", ""),
                            title = s.optString("title", "")
                        ).apply {
                            poster = s.optStringOrNull("poster")
                            banner = s.optStringOrNull("banner")
                            this.isFavorite = isFavorite
                            this.favoritedAtMillis = favoritedAtMillis
                            this.isWatching = isWatching
                        }
                        providerCtx.tvShowDao.save(tvShow)
                        Log.d(TAG, "IMPORT: [${providerName}] TV Show: ${tvShow.title}. Favorites: $isFavorite, Watching: $isWatching")
                    }
                }

                providerObj.optJSONArray("movies")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        val m = arr.optJSONObject(j) ?: continue
                        val isFavorite = m.optBoolean("isFavorite", false)
                        val favoritedAtMillis = m.optLongOrNull("favoritedAtMillis")
                        val isWatched = m.optBoolean("isWatched", false)
                        val watchedDate = m.optLongOrNull("watchedDate")?.toCalendar()
                        val watchHistory = m.optJSONObject("watchHistory")?.toWatchHistory()

                        val movie = Movie(
                            id = m.optString("id", ""),
                            title = m.optString("title", "")
                        ).apply {
                            poster = m.optStringOrNull("poster")
                            banner = m.optStringOrNull("banner")
                            this.isFavorite = isFavorite
                            this.favoritedAtMillis = favoritedAtMillis
                            this.isWatched = isWatched
                            this.watchedDate = watchedDate
                            this.watchHistory = watchHistory
                        }
                        providerCtx.movieDao.save(movie)
                        Log.d(TAG, "IMPORT: [${providerName}] Movie: ${movie.title}. Favorites: $isFavorite, Watched: $isWatched, History: ${watchHistory != null}")
                    }
                }

                providerObj.optJSONArray("episodes")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        val e = arr.optJSONObject(j) ?: continue
                        val isWatched = e.optBoolean("isWatched", false)
                        val watchedDate = e.optLongOrNull("watchedDate")?.toCalendar()
                        val watchHistory = e.optJSONObject("watchHistory")?.toWatchHistory()

                        val ep = Episode(id = e.optString("id", "")).apply {
                            number = e.optInt("number", 0)
                            title = e.optStringOrNull("title")
                            poster = e.optStringOrNull("poster")
                            e.optStringOrNull("tvShowId")?.let { tvId -> tvShow = TvShow(tvId, "") }
                            e.optStringOrNull("seasonId")?.let { sId -> season = Season(sId, 0) }
                            this.isWatched = isWatched
                            this.watchedDate = watchedDate
                            this.watchHistory = watchHistory
                        }
                        providerCtx.episodeDao.save(ep)
                        Log.d(TAG, "IMPORT: [${providerName}] Episode: ${ep.title}. Watched: $isWatched, History: ${watchHistory != null}")
                    }
                }

                buildCacheForProvider(providerCtx)
            }

            if (backupVersion >= 5) {
                obj.optJSONObject("library")?.let { lib ->
                    importLibraryData(lib, backupVersion)
                }
            }

            Log.d(TAG, "Import completed successfully")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Error during importUserData", t)
            false
        }
    }

    private suspend fun importLibraryData(lib: JSONObject, backupVersion: Int = 5) {
        val libraryDb = LibraryDatabase.getInstance(context)

        if (backupVersion >= 6) {
            lib.optJSONArray("profiles")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val p = arr.optJSONObject(j) ?: continue
                    val profile = Profile(
                        id = p.optString("id", ""),
                        name = p.optString("name", ""),
                        iconRes = p.optString("iconRes", "ic_profile_blue"),
                        age = p.optIntOrNull("age"),
                        isDefault = p.optBoolean("isDefault", false),
                        createdAt = p.optLong("createdAt", System.currentTimeMillis())
                    )
                    libraryDb.profileDao().insert(profile)
                }
                Log.d(TAG, "IMPORT: Imported ${arr.length()} profiles")
            }
        }

        lib.optJSONArray("watchRecords")?.let { arr ->
            for (j in 0 until arr.length()) {
                val r = arr.optJSONObject(j) ?: continue
                val record = WatchRecord(
                    contentId = r.optString("contentId", ""),
                    title = r.optString("title", ""),
                    poster = r.optStringOrNull("poster"),
                    type = r.optString("type", ""),
                    providerName = r.optString("providerName", ""),
                    profileId = r.optString("profileId", ""),
                    watchedDateMillis = r.optLong("watchedDateMillis", 0L),
                    durationMillis = r.optLong("durationMillis", 0L),
                    showTitle = r.optStringOrNull("showTitle"),
                    seasonNumber = r.optIntOrNull("seasonNumber"),
                    episodeNumber = r.optIntOrNull("episodeNumber")
                )
                libraryDb.watchRecordDao().insert(record)
            }
            Log.d(TAG, "IMPORT: Imported ${arr.length()} watch records")
        }

        lib.optJSONArray("libraryItems")?.let { arr ->
            for (j in 0 until arr.length()) {
                val item = arr.optJSONObject(j) ?: continue
                val libraryItem = LibraryItem(
                    contentId = item.optString("contentId", ""),
                    title = item.optString("title", ""),
                    type = item.optString("type", ""),
                    poster = item.optStringOrNull("poster"),
                    banner = item.optStringOrNull("banner"),
                    overview = item.optStringOrNull("overview"),
                    rating = item.optDoubleOrNull("rating"),
                    year = item.optStringOrNull("year"),
                    profileId = item.optStringOrNull("profileId"),
                    addedAtMillis = item.optLong("addedAtMillis", System.currentTimeMillis())
                )
                libraryDb.libraryItemDao().insert(libraryItem)
            }
            Log.d(TAG, "IMPORT: Imported ${arr.length()} library items")
        }

        lib.optJSONArray("playlists")?.let { arr ->
            for (j in 0 until arr.length()) {
                val p = arr.optJSONObject(j) ?: continue
                val playlist = Playlist(
                    name = p.optString("name", ""),
                    createdAt = p.optLong("createdAt", System.currentTimeMillis())
                )
                libraryDb.playlistDao().insert(playlist)
            }
            Log.d(TAG, "IMPORT: Imported ${arr.length()} playlists")
        }

        lib.optJSONArray("playlistItems")?.let { arr ->
            for (j in 0 until arr.length()) {
                val pi = arr.optJSONObject(j) ?: continue
                val playlistItem = PlaylistItem(
                    playlistId = pi.optLong("playlistId", 0),
                    contentId = pi.optString("contentId", ""),
                    title = pi.optString("title", ""),
                    poster = pi.optStringOrNull("poster"),
                    type = pi.optString("type", ""),
                    position = pi.optInt("position", 0),
                    addedAt = pi.optLong("addedAt", System.currentTimeMillis())
                )
                libraryDb.playlistItemDao().insert(playlistItem)
            }
            Log.d(TAG, "IMPORT: Imported ${arr.length()} playlist items")
        }
    }

    private suspend fun buildCacheForProvider(providerCtx: ProviderBackupContext) {
        try {
            val movies = providerCtx.movieDao.getFavorites().first()
            val tvShows = providerCtx.tvShowDao.getFavorites().first()
            val watchingMovies = providerCtx.movieDao.getWatchingMovies().first()
            val watchingEpisodes = providerCtx.episodeDao.getWatchingEpisodes().first()

            UserDataCache.writeMovies(context, providerCtx.provider, movies + watchingMovies)
            UserDataCache.writeTvShows(context, providerCtx.provider, tvShows)
            UserDataCache.writeEpisodes(context, providerCtx.provider, watchingEpisodes)
            Log.d(TAG, "CACHE: Built cache for provider ${providerCtx.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error building cache for provider ${providerCtx.name}", e)
        }
    }

    private fun addDatabaseFilesToZip(zip: ZipOutputStream, providerName: String) {
        val dbName = sanitizedDbName(providerName)
        listOf("", "-wal", "-shm").forEach { suffix ->
            val file = context.getDatabasePath("$dbName.db$suffix")
            if (!file.exists()) return@forEach
            zip.putNextEntry(ZipEntry("databases/${file.name}"))
            file.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun addLibraryDatabaseToZip(zip: ZipOutputStream) {
        listOf("", "-wal", "-shm").forEach { suffix ->
            val file = context.getDatabasePath("streamflix_library.db$suffix")
            if (!file.exists()) return@forEach
            zip.putNextEntry(ZipEntry("databases/${file.name}"))
            file.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun restoreDatabaseZip(input: InputStream) {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith("databases/")) {
                    val fileName = entry.name.removePrefix("databases/")
                    val target = context.getDatabasePath(fileName)
                    target.parentFile?.mkdirs()
                    if (target.exists()) target.delete()
                    target.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun sanitizedDbName(providerName: String): String {
        return providerName.lowercase(Locale.getDefault())
            .replace("[^a-z0-9]".toRegex(), "_")
            .replace("__+".toRegex(), "_")
            .trim('_')
    }

}

private fun Long.toCalendar(): Calendar = Calendar.getInstance().apply { timeInMillis = this@toCalendar }

private fun WatchItem.WatchHistory.toJson(): JSONObject =
    JSONObject().apply {
        put("lastEngagementTimeUtcMillis", lastEngagementTimeUtcMillis)
        put("lastPlaybackPositionMillis", lastPlaybackPositionMillis)
        put("durationMillis", durationMillis)
    }

private fun JSONObject.toWatchHistory(): WatchItem.WatchHistory? {
    val duration = optLong("durationMillis", 0L)
    if (duration <= 0) return null
    return WatchItem.WatchHistory(
        lastEngagementTimeUtcMillis = optLong("lastEngagementTimeUtcMillis", 0L),
        lastPlaybackPositionMillis = optLong("lastPlaybackPositionMillis", 0L),
        durationMillis = duration
    )
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    return if (has(name) && !isNull(name)) optLong(name) else null
}

private fun JSONObject.optStringOrNull(name: String): String? {
    return if (has(name) && !isNull(name)) optString(name) else null
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    return if (has(name) && !isNull(name)) optInt(name) else null
}

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    return if (has(name) && !isNull(name)) optDouble(name) else null
}
