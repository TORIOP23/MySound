package uet.app.mysound.data.db

import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonParser
import uet.app.mysound.common.Config
import uet.app.mysound.data.db.entities.AlbumEntity
import uet.app.mysound.data.db.entities.ArtistEntity
import uet.app.mysound.data.db.entities.FormatEntity
import uet.app.mysound.data.db.entities.LocalPlaylistEntity
import uet.app.mysound.data.db.entities.LyricsEntity
import uet.app.mysound.data.db.entities.PairSongLocalPlaylist
import uet.app.mysound.data.db.entities.PlaylistEntity
import uet.app.mysound.data.db.entities.QueueEntity
import uet.app.mysound.data.db.entities.SearchHistory
import uet.app.mysound.data.db.entities.SetVideoIdEntity
import uet.app.mysound.data.db.entities.SongEntity
import uet.app.mysound.data.parser.MyDB.HttpMethod
import uet.app.mysound.data.parser.MyDB.fetchDataWithJson
import uet.app.mysound.data.parser.MyDB.postDataWithJson
import uet.app.mysound.myAPI.User.LoginResponse
import uet.app.mysound.ui.MainActivity
import java.time.LocalDateTime
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val databaseDao: DatabaseDao) {
    suspend fun getAllRecentData() = databaseDao.getAllRecentData()
    suspend fun getAllDownloadedPlaylist() = databaseDao.getAllDownloadedPlaylist()

    suspend fun getSearchHistory(): List<SearchHistory> {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token != null) {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/get_search_history"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonData).asJsonObject
            val searchHistoryListType = object : TypeToken<List<SearchHistory>>() {}.type
            gson.fromJson(jsonObject.getAsJsonArray("search_history"), searchHistoryListType)
        } else {
            // Handle the case when the token is null, e.g., return an empty list
            databaseDao.getSearchHistory();
        }
    }


    suspend fun deleteSearchHistory() {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            // Token is null, call the databaseDao.insertSearchHistory(searchHistory)
            databaseDao.deleteSearchHistory()
        } else {
            // Token is not null, log information
            Log.i("YourTag", "Token is not null: ${response.token}")
            Log.i("YourTag", "AUDIO Token is not null: ${response.audioToken}")
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/add_search_history"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.PUT)
            println("them data: $jsonData");
            databaseDao.deleteSearchHistory()
        }
    }

    suspend fun insertSearchHistory(searchHistory: SearchHistory) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            // Token is null, call the databaseDao.insertSearchHistory(searchHistory)
            databaseDao.insertSearchHistory(searchHistory)
            // Handle the result as needed, for example, print it to the screen
            Log.i("YourTag", "Inserted SearchHistory: $searchHistory")
        } else {
            // Token is not null, log information
            Log.i("YourTag", "Token is not null: ${response.token}")
            Log.i("YourTag", "AUDIO Token is not null: ${response.audioToken}")
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/add_search_history"
            val jsonBody = Gson().toJson(searchHistory)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            println("them data: $jsonData");
        }
    }

    suspend fun getAllSongs(): List<SongEntity> {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token != null) {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/songs"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonData).asJsonObject
            databaseDao.getAllSongs()
        } else {
            // Handle the case when the token is null, e.g., return an empty list
            databaseDao.getAllSongs()
        }
    }

    suspend fun getRecentSongs(limit: Int, offset: Int) = databaseDao.getRecentSongs(limit, offset)
    suspend fun getSongByListVideoId(primaryKeyList: List<String>) =
        databaseDao.getSongByListVideoId(primaryKeyList)

    suspend fun getDownloadedSongs() = databaseDao.getDownloadedSongs()
    suspend fun getDownloadingSongs() = databaseDao.getDownloadingSongs()
    suspend fun getLikedSongs() = databaseDao.getLikedSongs()
    suspend fun getLibrarySongs() = databaseDao.getLibrarySongs()
    suspend fun getSong(videoId: String) = databaseDao.getSong(videoId)
    suspend fun insertSong(song: SongEntity) {
        val response: LoginResponse? = MainActivity.loginResponse

        if (response?.token == null) {
            // Token is null, call the databaseDao.insertSong(song)
            databaseDao.insertSong(song)
            // Handle the result as needed, for example, print it to the screen
            Log.i("YourTag", "Inserted Song: $song")
        } else {
            // Token is not null, log information
            Log.i("YourTag", "Token is not null: ${response.token}")
            Log.i("YourTag", "AUDIO Token is not null: ${response.audioToken}")
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/song"
            val jsonBody = Gson().toJson(song)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            println("them data: $jsonData");
            databaseDao.insertSong(song)
        }
    }

    suspend fun updateListenCount(videoId: String) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            databaseDao.updateTotalPlayTime(videoId);
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/interaction/batch/like"
            val jsonBody = Gson().toJson(videoId)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            };
            databaseDao.updateTotalPlayTime(videoId);
        }
    }

    suspend fun updateLiked(liked: Int, videoId: String) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            databaseDao.updateLiked(liked, videoId)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/interaction/batch/unlike"
            val jsonBody = Gson().toJson(videoId)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            };
            databaseDao.updateLiked(liked, videoId)
        }
    }

    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String) =
        databaseDao.updateDurationSeconds(durationSeconds, videoId)

    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) =
        databaseDao.updateSongInLibrary(inLibrary, videoId)

    suspend fun getMostPlayedSongs() = databaseDao.getMostPlayedSongs()
    suspend fun updateDownloadState(downloadState: Int, videoId: String) =
        databaseDao.updateDownloadState(downloadState, videoId)

    suspend fun getAllArtists(): List<ArtistEntity> {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getAllArtists()
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/artists"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getAllArtists()
        }
    }

    suspend fun insertArtist(artist: ArtistEntity) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            databaseDao.insertArtist(artist)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/artists"
            val jsonBody = Gson().toJson(artist)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            };
            databaseDao.insertArtist(artist)
        }
    }

    suspend fun updateFollowed(followed: Int, channelId: String) =
        databaseDao.updateFollowed(followed, channelId)

    suspend fun getArtist(channelId: String): ArtistEntity {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getArtist(channelId)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/artist/$channelId"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getArtist(channelId)
        }
    }

    suspend fun getFollowedArtists() = databaseDao.getFollowedArtists()
    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String) =
        databaseDao.updateArtistInLibrary(inLibrary, channelId)

    suspend fun getAllAlbums(): List<AlbumEntity> {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getAllAlbums()
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/albums"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getAllAlbums()
        }
    }

    suspend fun insertAlbum(album: AlbumEntity) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            databaseDao.insertAlbum(album)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/artists"
            val jsonBody = Gson().toJson(album)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            };
            databaseDao.insertAlbum(album)
        }
    }

    suspend fun updateAlbumLiked(liked: Int, albumId: String) =
        databaseDao.updateAlbumLiked(liked, albumId)

    suspend fun getAlbum(albumId: String): AlbumEntity {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getAlbum(albumId)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/album/$albumId/info"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getAlbum(albumId)
        }
    }

    suspend fun getLikedAlbums() = databaseDao.getLikedAlbums()
    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, albumId: String) =
        databaseDao.updateAlbumInLibrary(inLibrary, albumId)

    suspend fun updateAlbumDownloadState(downloadState: Int, albumId: String) =
        databaseDao.updateAlbumDownloadState(downloadState, albumId)

    suspend fun getAllPlaylists(): List<PlaylistEntity> {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getAllPlaylists()
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/playlist"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getAllPlaylists()
        }
    }
    suspend fun insertPlaylist(playlist: PlaylistEntity) {
        val response: LoginResponse? = MainActivity.loginResponse
        if (response?.token == null) {
            databaseDao.insertPlaylist(playlist)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/artists"
            val jsonBody = Gson().toJson(playlist)
            val jsonData = postDataWithJson(url, jsonBody, response.token)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            };
            databaseDao.insertPlaylist(playlist)
        }
    }

    suspend fun insertRadioPlaylist(playlist: PlaylistEntity) =
        databaseDao.insertRadioPlaylist(playlist)

    suspend fun updatePlaylistLiked(liked: Int, playlistId: String) =
        databaseDao.updatePlaylistLiked(liked, playlistId)

    suspend fun getPlaylist(playlistId: String): PlaylistEntity? {
        val response: LoginResponse? = MainActivity.loginResponse
        return if (response?.token == null) {
            databaseDao.getPlaylist(playlistId)
        } else {
            val baseUrl = Config.local_Url
            val url = "$baseUrl/api/playlist/$playlistId"
            val jsonData = fetchDataWithJson(url, response.token, HttpMethod.GET)
            if (jsonData != null) {
                Log.i("TAG", jsonData)
            }
            databaseDao.getPlaylist(playlistId)
        }
    }

    suspend fun getLikedPlaylists() = databaseDao.getLikedPlaylists()
    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) =
        databaseDao.updatePlaylistInLibrary(inLibrary, playlistId)

    suspend fun updatePlaylistDownloadState(downloadState: Int, playlistId: String) =
        databaseDao.updatePlaylistDownloadState(downloadState, playlistId)

    suspend fun getAllLocalPlaylists() = databaseDao.getAllLocalPlaylists()
    suspend fun getLocalPlaylist(id: Long) = databaseDao.getLocalPlaylist(id)
    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity) =
        databaseDao.insertLocalPlaylist(localPlaylist)

    suspend fun deleteLocalPlaylist(id: Long) = databaseDao.deleteLocalPlaylist(id)
    suspend fun updateLocalPlaylistTitle(title: String, id: Long) =
        databaseDao.updateLocalPlaylistTitle(title, id)

    suspend fun updateLocalPlaylistThumbnail(thumbnail: String, id: Long) =
        databaseDao.updateLocalPlaylistThumbnail(thumbnail, id)

    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) =
        databaseDao.updateLocalPlaylistTracks(tracks, id)

    suspend fun updateLocalPlaylistInLibrary(inLibrary: LocalDateTime, id: Long) =
        databaseDao.updateLocalPlaylistInLibrary(inLibrary, id)

    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) =
        databaseDao.updateLocalPlaylistDownloadState(downloadState, id)

    suspend fun getDownloadedLocalPlaylists() = databaseDao.getDownloadedLocalPlaylists()
    suspend fun updateLocalPlaylistYouTubePlaylistId(id: Long, ytId: String?) =
        databaseDao.updateLocalPlaylistYouTubePlaylistId(id, ytId)

    suspend fun updateLocalPlaylistYouTubePlaylistSynced(id: Long, synced: Int) =
        databaseDao.updateLocalPlaylistYouTubePlaylistSynced(id, synced)

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) =
        databaseDao.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)

    suspend fun getSavedLyrics(videoId: String) = databaseDao.getLyrics(videoId)
    suspend fun insertLyrics(lyrics: LyricsEntity) = databaseDao.insertLyrics(lyrics)
    suspend fun getPreparingSongs() = databaseDao.getPreparingSongs()

    suspend fun insertFormat(format: FormatEntity) = databaseDao.insertFormat(format)
    suspend fun getFormat(videoId: String) = databaseDao.getFormat(videoId)

    suspend fun recoverQueue(queueEntity: QueueEntity) = databaseDao.recoverQueue(queueEntity)
    suspend fun getQueue() = databaseDao.getQueue()
    suspend fun deleteQueue() = databaseDao.deleteQueue()

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String) =
        databaseDao.getLocalPlaylistByYoutubePlaylistId(playlistId)

    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity) =
        databaseDao.insertSetVideoId(setVideoIdEntity)

    suspend fun getSetVideoId(videoId: String) = databaseDao.getSetVideoId(videoId)

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        databaseDao.insertPairSongLocalPlaylist(pairSongLocalPlaylist)

    suspend fun getPlaylistPairSong(playlistId: Long) = databaseDao.getPlaylistPairSong(playlistId)
    suspend fun deletePairSongLocalPlaylist(playlistId: Long, videoId: String) =
        databaseDao.deletePairSongLocalPlaylist(playlistId, videoId)
}