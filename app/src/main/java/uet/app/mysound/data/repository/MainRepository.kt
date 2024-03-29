package uet.app.mysound.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.utf8Size
import uet.app.mysound.R
import uet.app.mysound.common.Config
import uet.app.mysound.common.VIDEO_QUALITY
import uet.app.mysound.data.dataStore.DataStoreManager
import uet.app.mysound.data.db.LocalDataSource
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
import uet.app.mysound.data.model.browse.album.AlbumBrowse
import uet.app.mysound.data.model.browse.album.Track
import uet.app.mysound.data.model.browse.artist.ArtistBrowse
import uet.app.mysound.data.model.browse.playlist.Author
import uet.app.mysound.data.model.browse.playlist.PlaylistBrowse
import uet.app.mysound.data.model.explore.mood.Genre
import uet.app.mysound.data.model.explore.mood.Mood
import uet.app.mysound.data.model.explore.mood.MoodsMoment
import uet.app.mysound.data.model.explore.mood.genre.GenreObject
import uet.app.mysound.data.model.explore.mood.moodmoments.MoodsMomentObject
import uet.app.mysound.data.model.home.HomeItem
import uet.app.mysound.data.model.metadata.Lyrics
import uet.app.mysound.data.model.podcast.PodcastBrowse
import uet.app.mysound.data.model.searchResult.albums.AlbumsResult
import uet.app.mysound.data.model.searchResult.artists.ArtistsResult
import uet.app.mysound.data.model.searchResult.playlists.PlaylistsResult
import uet.app.mysound.data.model.searchResult.songs.Artist
import uet.app.mysound.data.model.searchResult.songs.SongsResult
import uet.app.mysound.data.model.searchResult.songs.Thumbnail
import uet.app.mysound.data.model.searchResult.videos.VideosResult
import uet.app.mysound.data.parser.MyDB.fetchDataFromUrl
import uet.app.mysound.data.parser.parseAlbumData
import uet.app.mysound.data.parser.parseAlbumDataFromMySound
//import uet.app.mysound.data.parser.parseAlbumDataFromMySound
import uet.app.mysound.data.parser.parseArtistData
import uet.app.mysound.data.parser.parseGenreObject
import uet.app.mysound.data.parser.parseLibraryPlaylist
import uet.app.mysound.data.parser.parseMixeSongContend
import uet.app.mysound.data.parser.parseMixedContent
import uet.app.mysound.data.parser.parseMixedContentMySound
import uet.app.mysound.data.parser.parseMoodsMomentObject
import uet.app.mysound.data.parser.parsePlaylistData
import uet.app.mysound.data.parser.parsePodcast
import uet.app.mysound.data.parser.parsePodcastContinueData
import uet.app.mysound.data.parser.parsePodcastData
import uet.app.mysound.data.parser.parseRelated
import uet.app.mysound.data.parser.parseSetVideoId
import uet.app.mysound.data.parser.search.parseSearchAlbum
import uet.app.mysound.data.parser.search.parseSearchArtist
import uet.app.mysound.data.parser.search.parseSearchPlaylist
import uet.app.mysound.data.parser.search.parseSearchSong
import uet.app.mysound.data.parser.search.parseSearchVideo
import uet.app.mysound.data.parser.toListThumbnail
import uet.app.mysound.extension.bestMatchingIndex
import uet.app.mysound.extension.toListTrack
import uet.app.mysound.extension.toLyrics
import uet.app.mysound.extension.toTrack
import uet.app.mysound.myAPI.User.LoginResponse
import uet.app.mysound.ui.MainActivity
import uet.app.mysound.utils.Resource
import uet.app.youtubeExtractor.YouTube
import uet.app.youtubeExtractor.models.MediaType
import uet.app.youtubeExtractor.models.MusicShelfRenderer
import uet.app.youtubeExtractor.models.SearchSuggestions
import uet.app.youtubeExtractor.models.SongItem
import uet.app.youtubeExtractor.models.WatchEndpoint
import uet.app.youtubeExtractor.models.musixmatch.MusixmatchCredential
import uet.app.youtubeExtractor.models.musixmatch.MusixmatchTranslationLyricsResponse
import uet.app.youtubeExtractor.models.musixmatch.SearchMusixmatchResponse
import uet.app.youtubeExtractor.models.response.SearchResponse
import uet.app.youtubeExtractor.models.sponsorblock.SkipSegments
import uet.app.youtubeExtractor.models.youtube.YouTubeInitialPage
import uet.app.youtubeExtractor.pages.BrowseResult
import uet.app.youtubeExtractor.pages.PlaylistPage
import uet.app.youtubeExtractor.pages.SearchPage
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

//@ActivityRetainedScoped
@Singleton
class MainRepository @Inject constructor(private val localDataSource: LocalDataSource, private val dataStoreManager: DataStoreManager, @ApplicationContext private val context: Context) {
    //Database
    suspend fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getSearchHistory")
            emit(localDataSource.getSearchHistory())

        }.flowOn(Dispatchers.IO)

    suspend fun insertSearchHistory(searchHistory: SearchHistory) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertSearchHistory")

            localDataSource.insertSearchHistory(searchHistory)
        }

    suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B deleteSearchHistory")

            localDataSource.deleteSearchHistory()
        }

    suspend fun getAllSongs(): Flow<List<SongEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getAllSongs")

            emit(localDataSource.getAllSongs())
        }.flowOn(Dispatchers.IO)

    suspend fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getSongsByListVideoId")

            emit(localDataSource.getSongByListVideoId(listVideoId))
        }.flowOn(Dispatchers.IO)

    suspend fun getDownloadedSongs(): Flow<List<SongEntity>?> =
        flow {
            Log.i("TAG", "QUERRY D*9B getDownloadedSongs")
            emit(localDataSource.getDownloadedSongs())
        }.flowOn(Dispatchers.IO)

    suspend fun getDownloadingSongs(): Flow<List<SongEntity>?> =
        flow { emit(localDataSource.getDownloadingSongs()) }.flowOn(Dispatchers.IO)
    suspend fun getPreparingSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getPreparingSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getLikedSongs(): Flow<List<SongEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getLikedSongs")
            emit(localDataSource.getLikedSongs())
        }.flowOn(Dispatchers.IO)

    suspend fun getLibrarySongs(): Flow<List<SongEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getLibrarySongs")

            emit(localDataSource.getLibrarySongs())
        }.flowOn(Dispatchers.IO)

    suspend fun getSongById(id: String): Flow<SongEntity?> =
        flow {
            emit(localDataSource.getSong(id))
            Log.i("TAG", "QUERRY D*9B getSongById")
        }.flowOn(Dispatchers.IO)

    suspend fun insertSong(songEntity: SongEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertSong")

            localDataSource.insertSong(songEntity)
        }

    suspend fun updateListenCount(videoId: String) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B updateListenCount")
            localDataSource.updateListenCount(videoId)
        }

    suspend fun updateLikeStatus(videoId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateLikeStatus")

            localDataSource.updateLiked(likeStatus, videoId)
        }

    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateSongInLibrary")

            localDataSource.updateSongInLibrary(inLibrary, videoId)
        }

    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateDurationSeconds")

            localDataSource.updateDurationSeconds(durationSeconds, videoId)
        }

    suspend fun getMostPlayedSongs(): Flow<List<SongEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getMostPlayedSongs")

            emit(
                localDataSource.getMostPlayedSongs()
            )
        }.flowOn(Dispatchers.IO)

    suspend fun updateDownloadState(videoId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateDownloadState")
            localDataSource.updateDownloadState(
                downloadState,
                videoId
            )
        }

    suspend fun getAllArtists(): Flow<List<ArtistEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getAllArtists")
            emit(localDataSource.getAllArtists())
        }.flowOn(Dispatchers.IO)

    suspend fun getArtistById(id: String): Flow<ArtistEntity> =
        flow {
            Log.i("TAG", "QUERRY D*9B getArtistById")

            emit(localDataSource.getArtist(id))
        }.flowOn(Dispatchers.IO)

    suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertArtist")
            localDataSource.insertArtist(artistEntity)
        }

    suspend fun updateFollowedStatus(channelId: String, followedStatus: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateFollowedStatus")
            localDataSource.updateFollowed(followedStatus, channelId)
        }

    suspend fun getFollowedArtists(): Flow<List<ArtistEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getFollowedArtists")

            emit(localDataSource.getFollowedArtists())
        }.flowOn(Dispatchers.IO)

    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateArtistInLibrary")

            localDataSource.updateArtistInLibrary(
                inLibrary,
                channelId
            )
        }

    suspend fun getAllAlbums(): Flow<List<AlbumEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getAllAlbums")

            emit(localDataSource.getAllAlbums())
        }.flowOn(Dispatchers.IO)

    suspend fun getAlbum(id: String): Flow<AlbumEntity> =
        flow {
            Log.i("TAG", "QUERRY D*9B getAlbum")
            emit(localDataSource.getAlbum(id))
        }.flowOn(Dispatchers.IO)

    suspend fun getLikedAlbums(): Flow<List<AlbumEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getLikedAlbums")

            emit(localDataSource.getLikedAlbums())
        }.flowOn(Dispatchers.IO)

    suspend fun insertAlbum(albumEntity: AlbumEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertAlbum")
            localDataSource.insertAlbum(albumEntity)
        }

    suspend fun updateAlbumLiked(albumId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateAlbumLiked")

            localDataSource.updateAlbumLiked(likeStatus, albumId)
        }

    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, albumId: String) =
        withContext(Dispatchers.Main) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }

    suspend fun updateAlbumDownloadState(albumId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updateAlbumDownloadState")

            localDataSource.updateAlbumDownloadState(
                downloadState,
                albumId
            )
        }

    suspend fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
        flow { emit(localDataSource.getAllPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getPlaylist(id: String): Flow<PlaylistEntity?> =
        flow {
            Log.i("TAG", "QUERRY D*9B getPlaylist")

            emit(localDataSource.getPlaylist(id))
        }.flowOn(Dispatchers.IO)

    suspend fun getLikedPlaylists(): Flow<List<PlaylistEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getLikedPlaylists")

            emit(localDataSource.getLikedPlaylists())
        }.flowOn(Dispatchers.IO)

    suspend fun insertPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertPlaylist")

            localDataSource.insertPlaylist(playlistEntity)
        }

    suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertRadioPlaylist")

            localDataSource.insertRadioPlaylist(playlistEntity)
        }

    suspend fun updatePlaylistLiked(playlistId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) {
            Log.i("TAG", "QUERRY D*9B updatePlaylistLiked")


            localDataSource.updatePlaylistLiked(
                likeStatus,
                playlistId
            )
        }

    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) =
        withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistInLibrary(
                inLibrary,
                playlistId
            )
        }

    suspend fun updatePlaylistDownloadState(playlistId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {

            Log.i("TAG", "QUERRY D*9B updatePlaylistDownloadState")

            localDataSource.updatePlaylistDownloadState(
                downloadState,
                playlistId
            )
        }

    suspend fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow {
            Log.i("TAG", "QUERRY D*9B getAllLocalPlaylists")

            emit(localDataSource.getAllLocalPlaylists())
        }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylist(id: Long): Flow<LocalPlaylistEntity> =
        flow {

            Log.i("TAG", "QUERRY D*9B getLocalPlaylist")

            emit(localDataSource.getLocalPlaylist(id))
        }.flowOn(Dispatchers.IO)

    suspend fun insertLocalPlaylist(localPlaylistEntity: LocalPlaylistEntity) =
        withContext(Dispatchers.IO) {
            Log.i("TAG", "QUERRY D*9B insertLocalPlaylist")

            localDataSource.insertLocalPlaylist(localPlaylistEntity)
        }

    suspend fun deleteLocalPlaylist(id: Long) =
        withContext(Dispatchers.IO) { localDataSource.deleteLocalPlaylist(id) }

    suspend fun updateLocalPlaylistTitle(title: String, id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistTitle(title, id)
        }

    suspend fun updateLocalPlaylistThumbnail(thumbnail: String, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistThumbnail(thumbnail, id) }

    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistDownloadState(
                downloadState,
                id
            )
        }

    suspend fun updateLocalPlaylistInLibrary(inLibrary: LocalDateTime, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistInLibrary(inLibrary, id) }

    suspend fun getDownloadedLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow { emit(localDataSource.getDownloadedLocalPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getAllRecentData(): Flow<List<Any>> =
        flow { emit(localDataSource.getAllRecentData()) }.flowOn(Dispatchers.IO)

    suspend fun getAllDownloadedPlaylist(): Flow<List<Any>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    suspend fun getRecentSong(limit: Int, offset: Int) = run {
        println("Fetching recent songs with limit: $limit, offset: $offset")
        val result = localDataSource.getRecentSongs(limit, offset)
        Log.i("TAG", "QUERRY D*9B getRecentSong")
        result  // return result
    }

    suspend fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> =
        flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) {

            Log.i("TAG", "QUERRY D*9B insertLyrics")

            localDataSource.insertLyrics(lyricsEntity)
        }

    suspend fun insertFormat(format: FormatEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertFormat(format) }

    suspend fun getFormat(videoId: String): Flow<FormatEntity?> =
        flow { emit(localDataSource.getFormat(videoId)) }.flowOn(Dispatchers.Main)


    suspend fun recoverQueue(temp: List<Track>) {
        val queueEntity = QueueEntity(listTrack = temp)
        withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
    }

    suspend fun removeQueue(){
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow { emit(localDataSource.getQueue())  }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String): Flow<LocalPlaylistEntity?> =
        flow { emit(localDataSource.getLocalPlaylistByYoutubePlaylistId(playlistId)) }.flowOn(Dispatchers.IO)

    suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    suspend fun getSetVideoId(videoId: String): Flow<SetVideoIdEntity?> =
        flow { emit(localDataSource.getSetVideoId(videoId)) }.flowOn(Dispatchers.IO)


    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) = withContext(Dispatchers.IO) {
        localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
    }

    suspend fun getPlaylistPairSong(playlistId: Long): Flow<List<PairSongLocalPlaylist>?> = flow<List<PairSongLocalPlaylist>?> {
        emit(localDataSource.getPlaylistPairSong(playlistId))
    }.flowOn(Dispatchers.IO)

    suspend fun deletePairSongLocalPlaylist(playlistId: Long, videoId: String) = withContext(Dispatchers.IO) {
        localDataSource.deletePairSongLocalPlaylist(playlistId, videoId)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistId(id: Long, ytId: String?) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistId(id, ytId)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistSynced(id: Long, synced: Int) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSynced(id, synced)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    suspend fun getHomeData(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        runCatching {
            YouTube.customQuery(browseId = "FEmusic_home").onSuccess { result ->
                val list: ArrayList<HomeItem> = arrayListOf()
                if (result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(0)?.text != null) {
                    val accountName = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(0)?.text ?: ""
                    val accountThumbUrl = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.get(0)?.url?.replace("s88", "s352") ?: ""
                    if (accountName != "" && accountThumbUrl != "") {
                        dataStoreManager.putString("AccountName", accountName)
                        dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                    }
                }
                var continueParam =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                        0
                    )?.nextContinuationData?.continuation
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents
                list.addAll(parseMixedContent(data))
                var count = 0
                while (count < 5 && continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam).onSuccess { response ->
                        continueParam =
                            response.continuationContents?.sectionListContinuation?.continuations?.get(
                                0
                            )?.nextContinuationData?.continuation
                        Log.d("Repository", "continueParam: $continueParam")
                        val dataContinue =
                            response.continuationContents?.sectionListContinuation?.contents
                        list.addAll(parseMixedContent(dataContinue))
                        count++
                        Log.d("Repository", "count: $count")
                    }.onFailure {
                        Log.e("Repository", "Error: ${it.message}")
                        count++
                    }
                }
                Log.d("Repository", "List size: ${list.size}")
                emit(Resource.Success<ArrayList<HomeItem>>(list))
            }.onFailure { error ->
                emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getHomeDataFromMySound(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        try {
            val baseUrl = Config.local_Url;
            val url = "$baseUrl/data"
            val data = fetchDataFromUrl(url)

            if (data != null) {
                val list: ArrayList<HomeItem> = arrayListOf()
                list.addAll(parseMixedContentMySound(data))
                Log.i("TAG", " LIST CUA HOME" + list[0].contents.size);
                emit(Resource.Success<ArrayList<HomeItem>>(list))
            } else {
                emit(Resource.Error("Failed to fetch data"))
            }
        } catch (error: Exception) {
            emit(Resource.Error(error.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getMoodAndMomentsData(): Flow<Resource<Mood>> = flow {
        runCatching {
            YouTube.moodAndGenres().onSuccess { result ->
                val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                val listGenre: ArrayList<Genre> = arrayListOf()
                result[0].let { moodsmoment ->
                    for (item in moodsmoment.items) {
                        listMoodMoments.add(
                            MoodsMoment(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                result[1].let { genres ->
                    for (item in genres.items) {
                        listGenre.add(
                            Genre(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))

            }.onFailure { e ->
                emit(Resource.Error<Mood>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> = flow {
        runCatching {
            YouTube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseMoodsMomentObject(result)
                    if (data != null) {
                        emit(Resource.Success<MoodsMomentObject>(data))
                    } else {
                        emit(Resource.Error<MoodsMomentObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getGenreData(params: String): Flow<Resource<GenreObject>> = flow {
        kotlin.runCatching {
            YouTube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseGenreObject(result)
                    if (data != null) {
                        emit(Resource.Success<GenreObject>(data))
                    } else {
                        emit(Resource.Error<GenreObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<GenreObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getRadio(radioId: String, originalTrack: SongEntity? = null, artist: ArtistEntity? = null): Flow<Resource<PlaylistBrowse>> = flow {
        runCatching {
            YouTube.next(endpoint = WatchEndpoint(playlistId = radioId)).onSuccess { next ->
                val data: ArrayList<SongItem> = arrayListOf()
                data.addAll(next.items)
                var continuation = next.continuation
                Log.w("Radio", "data: ${data.size}")
                var count = 0
                while (continuation != null && count < 3) {
                    YouTube.next(endpoint = WatchEndpoint(playlistId = radioId), continuation = continuation).onSuccess { nextContinue ->
                        data.addAll(nextContinue.items)
                        continuation = nextContinue.continuation
                        if (data.size >= 50) {
                            continuation = null
                        }
                        Log.w("Radio", "data: ${data.size}")
                        count++
                    }.onFailure {
                        continuation = null
                        count++
                    }
                }
                val listTrackResult = data.toListTrack()
                if (originalTrack != null) {
                    listTrackResult.add(0, originalTrack.toTrack())
                }
                Log.w("Repository", "data: ${data.size}")
                val playlistBrowse = PlaylistBrowse(
                    author = Author(id = "", name = "YouTube Music"),
                    description = context.getString(R.string.auto_created_by_youtube_music),
                    duration = "",
                    durationSeconds = 0,
                    id = radioId,
                    privacy = "PRIVATE",
                    thumbnails = listOf(
                        Thumbnail(
                            544,
                            originalTrack?.thumbnails ?: artist?.thumbnails ?: "",
                            544
                        )
                    ),
                    title = "${originalTrack?.title ?: artist?.name} ${context.getString(R.string.radio)}",
                    trackCount = listTrackResult.size,
                    tracks = listTrackResult,
                    year = LocalDateTime.now().year.toString()
                )
                Log.w("Repository", "playlistBrowse: $playlistBrowse")
                emit(Resource.Success<PlaylistBrowse>(playlistBrowse))
            }
            .onFailure {
                exception ->
                exception.printStackTrace()
                emit(Resource.Error<PlaylistBrowse>(exception.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun reloadSuggestionPlaylist(reloadParams: String): Flow<Pair<String?, ArrayList<Track>?>?> = flow {
        runCatching {
            YouTube.customQuery(browseId = "", continuation = reloadParams, setLogin = true).onSuccess { values ->
                val data = values.continuationContents?.musicShelfContinuation?.contents
                val dataResult: ArrayList<SearchResponse.ContinuationContents.MusicShelfContinuation.Content> = arrayListOf()
                if (!data.isNullOrEmpty()) {
                    dataResult.addAll(data)
                }
                val reloadParamsNew = values.continuationContents?.musicShelfContinuation?.continuations?.get(0)?.reloadContinuationData?.continuation
                if (dataResult.isNotEmpty()) {
                    val listTrack: ArrayList<Track> = arrayListOf()
                    dataResult.forEach {
                        listTrack.add(
                            (SearchPage.toYTItem(
                                it.musicResponsiveListItemRenderer
                            ) as SongItem).toTrack()
                        )
                    }
                    emit(Pair(reloadParamsNew, listTrack))
                }
                else {
                    emit(null)
                }
            }.onFailure {
                exception ->
                exception.printStackTrace()
                emit(null)
            }
        }
    }
    suspend fun getSuggestionPlaylist(ytPlaylistId: String): Flow<Pair<String?, ArrayList<Track>?>?> = flow {
        runCatching {
            var id = ""
            if (!ytPlaylistId.startsWith("VL")) {
                id += "VL$ytPlaylistId"
            }
            else {
                id += ytPlaylistId
            }
            YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                println(result)
                var continueParam = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation ?: result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                val dataResult: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                var reloadParams : String? = null
                println("continueParam: $continueParam")
                while (continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam, setLogin = true).onSuccess { values ->
                        val data = values.continuationContents?.sectionListContinuation?.contents?.get(0)?.musicShelfRenderer?.contents
                        println("data: $data")
                        if (!data.isNullOrEmpty()) {
                            dataResult.addAll(data)
                        }
                        reloadParams = values.continuationContents?.sectionListContinuation?.contents?.get(0)?.musicShelfRenderer?.continuations?.get(0)?.reloadContinuationData?.continuation
                        continueParam = values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(0)?.nextContinuationData?.continuation
                        println("reloadParams: $reloadParams")
                        println("continueParam: $continueParam")
                    }.onFailure {
                        Log.e("Repository", "Error: ${it.message}")
                        continueParam = null
                    }
                }
                println("dataResult: ${dataResult.size}")
                if (dataResult.isNotEmpty()) {
                    val listTrack: ArrayList<Track> = arrayListOf()
                    dataResult.forEach {
                        listTrack.add(
                            (PlaylistPage.fromMusicResponsiveListItemRenderer(
                                it.musicResponsiveListItemRenderer
                            ) as SongItem).toTrack()
                        )
                    }
                    println("listTrack: $listTrack")
                    emit(Pair(reloadParams, listTrack))
                }
                else {
                    emit(null)
                }
            }
                .onFailure { exception ->
                    exception.printStackTrace()
                    emit(null)
                }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getPodcastData(podcastId: String): Flow<Resource<PodcastBrowse>> = flow {
        runCatching {
            YouTube.customQuery(browseId = podcastId).onSuccess { result ->
                val listEpisode = arrayListOf<PodcastBrowse.EpisodeItem>()
                val thumbnail =
                    result.background?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                val title =
                    result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                val author =
                    result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.let {
                        Artist(
                            id = it.straplineTextOne?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId,
                            name = it.straplineTextOne?.runs?.firstOrNull()?.text ?: "",
                        )
                    }
                val authorThumbnail =
                    result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.let {
                        it.straplineThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
                    }
                val description =
                    result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.description?.musicDescriptionShelfRenderer?.description?.runs?.mapNotNull {
                        it.text
                    }?.joinToString("")
                val data =
                    result.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
                parsePodcastData(data, author).let {
                    listEpisode.addAll(it)
                }
                var continueParam =
                    result.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                while (continueParam != null) {
                    YouTube.customQuery(continuation = continueParam, browseId = "")
                        .onSuccess { continueData ->
                            parsePodcastContinueData(
                                continueData.continuationContents?.musicShelfContinuation?.contents,
                                author
                            ).let {
                                listEpisode.addAll(it)
                            }
                            continueParam =
                                continueData.continuationContents?.musicShelfContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                        }
                        .onFailure {
                            it.printStackTrace()
                            continueParam = null
                        }
                }
                if (author != null) {
                    emit(
                        Resource.Success<PodcastBrowse>(
                            PodcastBrowse(
                                title = title ?: "",
                                author = author,
                                authorThumbnail = authorThumbnail,
                                thumbnail = thumbnail ?: emptyList<Thumbnail>(),
                                description = description,
                                listEpisode = listEpisode
                            )
                        )
                    )
                } else {
                    emit(Resource.Error<PodcastBrowse>("Error"))
                }
            }.onFailure { error ->
                emit(Resource.Error<PodcastBrowse>(error.message.toString()))
            }
        }
    }

    suspend fun getPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> = flow {
        runCatching {
            var id = ""
            if (!playlistId.startsWith("VL")) {
                id += "VL$playlistId"
            } else {
                id += playlistId
            }
            Log.d("Repository", "playlist id: $id")
            YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                val data: List<MusicShelfRenderer.Content>? = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.contents
                if (data != null) {
                    Log.d("Data", "data: $data")
                    Log.d("Data", "data size: ${data.size}")
                    listContent.addAll(data)
                }
                val header = result.header?.musicDetailHeaderRenderer ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                Log.d("Header", "header: $header")
                var continueParam = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                var count = 0
                Log.d("Repository", "playlist data: ${listContent.size}")
                Log.d("Repository", "continueParam: $continueParam")
                while (continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam, setLogin = true).onSuccess { values ->
                        Log.d("Continue", "continue: $continueParam")
                        val dataMore: List<MusicShelfRenderer.Content>? = values.continuationContents?.musicPlaylistShelfContinuation?.contents
                        if (dataMore != null) {
                            listContent.addAll(dataMore)
                        }
                        continueParam = values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(0)?.nextContinuationData?.continuation
                        count++
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        continueParam = null
                        count++
                    }
                }
                Log.d("Repository", "playlist final data: ${listContent.size}")
                parsePlaylistData(header, listContent, playlistId)?.let { playlist ->
                    emit(Resource.Success<PlaylistBrowse>(playlist))
                } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
            }.onFailure { e ->
                emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>> = flow {
        runCatching {
            YouTube.album(browseId, withSongs = true).onSuccess { result ->
                emit(Resource.Success<AlbumBrowse>(parseAlbumData(result)))
                Log.i("TAG", "yhanh");
            }.onFailure { e ->
                Log.d("Album", "Error: ${e.message}")
                emit(Resource.Error<AlbumBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)


    suspend fun getAlbumDataFromMySound(browseId: String): Flow<Resource<AlbumBrowse>> = flow {
        try {
            var baseUrl = Config.local_Url;
            val url = "$baseUrl/album/$browseId"
            val data = fetchDataFromUrl(url)
            if (data != null) {
                val albumBrowse: AlbumBrowse? = parseAlbumDataFromMySound(data);
                if (albumBrowse != null) {
                    Log.i("TAG", " ALBUM BROWSE CUA HOME");
                    emit(Resource.Success(albumBrowse))
                } else {
                    emit(Resource.Error("Failed to parse data"))
                }
            } else {
                emit(Resource.Error("Failed to fetch data"))
            }
        } catch (error: Exception) {
            emit(Resource.Error(error.message.toString()))
        }
    }.flowOn(Dispatchers.IO)


    suspend fun getAlbumMore(browseId: String, params: String): Flow<BrowseResult?> = flow {
        runCatching {
            YouTube.browse(browseId = browseId, params = params).onSuccess { result ->
                Log.w("Album More", "result: $result")
                emit(result)
            }.onFailure {
                it.printStackTrace()
                emit(null)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> = flow {
        runCatching {
            YouTube.artist(channelId).onSuccess { result ->
                emit(Resource.Success<ArtistBrowse>(parseArtistData(result, context)))
            }.onFailure { e ->
                Log.d("Artist", "Error: ${e.message}")
                emit(Resource.Error<ArtistBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getMySearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> = flow {
        runCatching {
            // Gọi API hoặc lấy dữ liệu từ nguồn khác và nhận được dữ liệu dạng JSON

            val baseUrl = Config.local_Url;
            val url = "$baseUrl/search?keyword=$query";
            val data = fetchDataFromUrl(url)

            // Gọi hàm parseMixeSongContend để chuyển đổi JSON thành danh sách SongsResult
            val listSongs: ArrayList<SongsResult> = parseMixeSongContend(data)

            emit(Resource.Success(listSongs))
        }.onFailure { e ->
            Log.d("Search", "Error: ${e.message}")
            emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                val listSongs: ArrayList<SongsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchSong(result).let { list ->
                    listSongs.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
                        parseSearchSong(values).let { list ->
                            listSongs.addAll(list)
                        }
                        count++
                        countinueParam = values.continuation
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        countinueParam = null
                        count++
                    }
                }

                emit(Resource.Success<ArrayList<SongsResult>>(listSongs))
            }.onFailure { e ->
                Log.d("Search", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).onSuccess { result ->
                val listSongs: ArrayList<VideosResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchVideo(result).let { list ->
                    listSongs.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
                        parseSearchVideo(values).let { list ->
                            listSongs.addAll(list)
                        }
                        count++
                        countinueParam = values.continuation
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        countinueParam = null
                        count++
                    }
                }

                emit(Resource.Success<ArrayList<VideosResult>>(listSongs))
            }.onFailure { e ->
                Log.d("Search", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<VideosResult>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_PODCAST).onSuccess { result ->
                    println(query)
                    val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                    var countinueParam = result.continuation
                    Log.w("Podcast", "result: $result")
                    parsePodcast(result.listPodcast).let { list ->
                        listPlaylist.addAll(list)
                    }
                    var count = 0
                    while (count < 2 && countinueParam != null) {
                        YouTube.searchContinuation(countinueParam).onSuccess { values ->
                            parsePodcast(values.listPodcast).let { list ->
                                listPlaylist.addAll(list)
                            }
                            count++
                            countinueParam = values.continuation
                        }.onFailure {
                            Log.e("Continue", "Error: ${it.message}")
                            countinueParam = null
                            count++
                        }
                    }
                    emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                }.onFailure { e ->
                    Log.d("Search", "Error: ${e.message}")
                    emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchPlaylist(result).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parseSearchPlaylist(values).let { list ->
                                    listPlaylist.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                    Log.d("Search", "Error: ${e.message}")
                    emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> =
        flow {
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { result ->
                    val listArtist: ArrayList<ArtistsResult> = arrayListOf()
                    var countinueParam = result.continuation
                    parseSearchArtist(result).let { list ->
                        listArtist.addAll(list)
                    }
                    var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
                        parseSearchArtist(values).let { list ->
                            listArtist.addAll(list)
                        }
                        count++
                        countinueParam = values.continuation
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        countinueParam = null
                        count++
                    }
                }
                emit(Resource.Success<ArrayList<ArtistsResult>>(listArtist))
            }.onFailure { e ->
                Log.d("Search", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<ArtistsResult>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).onSuccess { result ->
                val listAlbum: ArrayList<AlbumsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchAlbum(result).let { list ->
                    listAlbum.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
                        parseSearchAlbum(values).let { list ->
                            listAlbum.addAll(list)
                        }
                        count++
                        countinueParam = values.continuation
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        countinueParam = null
                        count++
                    }
                }
                emit(Resource.Success<ArrayList<AlbumsResult>>(listAlbum))
            }.onFailure { e ->
                Log.d("Search", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<AlbumsResult>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).onSuccess { result ->
                val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchPlaylist(result).let { list ->
                    listPlaylist.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
                        parseSearchPlaylist(values).let { list ->
                            listPlaylist.addAll(list)
                        }
                        count++
                        countinueParam = values.continuation
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        countinueParam = null
                        count++
                    }
                }
                emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
            }.onFailure { e ->
                Log.d("Search", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> = flow {
        runCatching {
//            YouTube.getSuggestQuery(query).onSuccess {
//                emit(Resource.Success<ArrayList<String>>(it))
//            }.onFailure { e ->
//                Log.d("Suggest", "Error: ${e.message}")
//                emit(Resource.Error<ArrayList<String>>(e.message.toString()))
//            }
            YouTube.getYTMusicSearchSuggestions(query).onSuccess {
                emit(Resource.Success(it))
            }.onFailure { e ->
                Log.d("Suggest", "Error: ${e.message}")
                emit(Resource.Error<SearchSuggestions>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getRelatedData(videoId: String): Flow<Resource<ArrayList<Track>>> = flow {
        runCatching {
            YouTube.nextCustom(videoId).onSuccess { result ->
                val listSongs: ArrayList<Track> = arrayListOf()
                val data = result.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents
                parseRelated(data)?.let { list ->
                    listSongs.addAll(list)
                }
                emit(Resource.Success<ArrayList<Track>>(listSongs))
            }.onFailure { e ->
                Log.d("Related", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<Track>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getYouTubeCaption(videoId: String): Flow<Resource<Lyrics>> = flow {
        runCatching {
            getFormat(videoId).first()?.youtubeCaptionsUrl?.let { url ->
                Log.w("DURATION", "url: $url")
                YouTube.getYouTubeCaption(url).onSuccess { lyrics ->
                    Log.w("Lyrics", "lyrics: ${lyrics.toLyrics()}")
                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
                }.onFailure { e ->
                    Log.d("Lyrics", "Error: ${e.message}")
                    emit(Resource.Error<Lyrics>(e.message.toString()))
                }
            }
        }
    }
    suspend fun getLyricsData(query: String, durationInt: Int? = null): Flow<Pair<String, Resource<Lyrics>>> = flow {
        runCatching {
//            val q = query.replace(Regex("\\([^)]*?(feat.|ft.|cùng với|con)[^)]*?\\)"), "")
//                .replace("  ", " ")
            val q =
                query.replace(Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec) "), " ").replace(
                    Regex("( và | & | и | e | und |, )"), " "
                ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
            Log.d("Lyrics", "query: $q")
            var musixMatchUserToken = YouTube.musixmatchUserToken
            if (musixMatchUserToken == null) {
                YouTube.getMusixmatchUserToken().onSuccess { usertoken ->
                    YouTube.musixmatchUserToken = usertoken.message.body.user_token
                    musixMatchUserToken = usertoken.message.body.user_token
                }
                    .onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(Pair("", Resource.Error<Lyrics>("Not found")))
                    }
            }
            YouTube.searchMusixmatchTrackId(q, musixMatchUserToken!!).onSuccess { searchResult ->
                val list = arrayListOf<String>()
                for (i in searchResult.message.body.track_list) {
                    list.add(i.track.track_name + " " + i.track.artist_name)
                }
                var id = ""
                var track: SearchMusixmatchResponse.Message.Body.Track.TrackX? = null
                Log.d("DURATION", "duration: $durationInt")
                val bestMatchingIndex = bestMatchingIndex(q, list)
                if (durationInt != null && durationInt != 0) {
                    val trackLengthList = arrayListOf<Int>()
                    for (i in searchResult.message.body.track_list) {
                        trackLengthList.add(i.track.track_length)
                    }
                    val closestIndex = trackLengthList.minByOrNull { kotlin.math.abs(it - durationInt) }
                    if (closestIndex != null && kotlin.math.abs(closestIndex - durationInt) < 2) {
                        id += searchResult.message.body.track_list.find { it.track.track_length == closestIndex }?.track?.track_id.toString()
                        track = searchResult.message.body.track_list.find { it.track.track_length == closestIndex }?.track
                    }
                    if (id == "") {
                        if (list.get(bestMatchingIndex).contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name) && query.contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name)) {
                            Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name}")
                            id += searchResult.message.body.track_list.get(bestMatchingIndex).track.track_id.toString()
                            track = searchResult.message.body.track_list.get(bestMatchingIndex).track
                        }
                    }
                }
                else {
                    if (list.get(bestMatchingIndex).contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name) && query.contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name)) {
                        Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name}")
                        id += searchResult.message.body.track_list.get(bestMatchingIndex).track.track_id.toString()
                        track = searchResult.message.body.track_list.get(bestMatchingIndex).track
                    }
                }
                Log.d("DURATION", "id: $id")
                Log.w("item lyrics", searchResult.message.body.track_list.find { it.track.track_id == id.toInt() }?.track?.track_name + " " + searchResult.message.body.track_list.find { it.track.track_id == id.toInt() }?.track?.artist_name)
                if (id != "" && track != null) {
//                    YouTube.getMusixmatchLyrics(id, musixMatchUserToken!!).onSuccess {
//                        if (it != null) {
//                            emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics())))
//                        }
//                        else {
//                            Log.w("Lyrics", "Error: Lỗi getLyrics ${it.toString()}")
//                            emit(Pair(id, Resource.Error<Lyrics>("Not found")))
//                        }
//                    }.onFailure {
//                        it.printStackTrace()
//                        emit(Pair(id, Resource.Error<Lyrics>("Not found")))
//                    }
                    YouTube.getMusixmatchLyricsByQ(track, musixMatchUserToken!!).onSuccess {
                        if (it != null) {
                            emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics())))
                        }
                        else {
                            Log.w("Lyrics", "Error: Lỗi getLyrics ${it.toString()}")
                            emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                        }
                    }.onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                    }
                }
//                bestMatchingIndex(q, list).let { index ->
//                    Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(index).track.track_name}")
//                    searchResult.message.body.track_list.get(index).track.track_id.let { trackId ->
//
//                    }
//                }
                else {
                    emit(Pair("", Resource.Error<Lyrics>("Not found")))
                }
            }
                .onFailure { throwable ->
                    throwable.printStackTrace()
                    emit(Pair("", Resource.Error<Lyrics>("Not found")))
                }

        }
    }.flowOn(Dispatchers.IO)
    suspend fun getTranslateLyrics(id: String): Flow<MusixmatchTranslationLyricsResponse?> = flow {
        runCatching {
            YouTube.musixmatchUserToken?.let {
                YouTube.getMusixmatchTranslateLyrics(id,
                    it, dataStoreManager.translationLanguage.first())
                    .onSuccess { lyrics ->
                        emit(lyrics)
                    }
                    .onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getStream(videoId: String, itag: Int): Flow<String?> = flow{
        Log.i("TAG", "GETSTREAM MAINREPO")
        if (videoId.utf8Size() > 20) {
            val response: LoginResponse? = MainActivity.loginResponse;
            Log.d("TAG", "")
            val baseUrl = Config.local_Url;
            if (response != null) {
                emit("$baseUrl/play/$videoId?t=${response.audioToken}")
            } else emit("$baseUrl/play/$videoId?t=6|e0w9z2RzOMpi5Ql6XtcaE7qGIvqrvfdh9Ks9dMHX")
        } else YouTube.player(videoId).onSuccess { data ->
            val videoItag =
                VIDEO_QUALITY.itags.getOrNull(VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first()))
                    ?: 22
            val response = data.second
            if (data.third == MediaType.Song) Log.w(
                "Stream",
                "response: is SONG"
            ) else Log.w("Stream", "response: is VIDEO")
            val format =
                if (data.third == MediaType.Song) response.streamingData?.adaptiveFormats?.find { it.itag == itag } else response.streamingData?.formats?.find { it.itag == videoItag }
            runBlocking {
                insertFormat(
                    FormatEntity(
                        videoId = videoId,
                        itag = format?.itag ?: itag,
                        mimeType = format?.mimeType,
                        bitrate = format?.bitrate?.toLong(),
                        contentLength = format?.contentLength,
                        lastModified = format?.lastModified,
                        loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
                        uploader = response.videoDetails?.author?.replace(Regex(" - Topic| - Chủ đề|"), ""),
                        uploaderId = response.videoDetails?.channelId,
                        uploaderThumbnail = response.videoDetails?.authorAvatar,
                        uploaderSubCount = response.videoDetails?.authorSubCount,
                        description = response.videoDetails?.description,
                        youtubeCaptionsUrl = response.captions?.playerCaptionsTracklistRenderer?.captionTracks?.get(
                            0
                        )?.baseUrl?.replace("&fmt=srv3", ""),
                        lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
                        playbackTrackingVideostatsPlaybackUrl = response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        playbackTrackingAtrUrl = response.playbackTracking?.atrUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        playbackTrackingVideostatsWatchtimeUrl = response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        cpn = data.first,
                    )
                )
            }
            emit(format?.url?.plus("&cpn=${data.first}"))
            }.onFailure {
                it.printStackTrace()
            Log.e("Stream", "Error: ${it.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getLibraryPlaylist(): Flow<ArrayList<PlaylistsResult>?> = flow {
        YouTube.getLibraryPlaylists().onSuccess { data ->
            val input = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.gridRenderer?.items ?: null
            if (input != null) {
                Log.w("Library", "input: ${input.size}")
                val list = parseLibraryPlaylist(input)
                emit(list)
            }
            else {
                emit(null)
            }
        }
            .onFailure {e ->
                Log.e("Library", "Error: ${e.message}")
                e.printStackTrace()
                emit(null)
            }
    }
    suspend fun initPlayback(playback: String, atr: String, watchTime: String, cpn: String, playlistId: String?): Flow<Pair<Int, Float>> = flow {
        YouTube.initPlayback(playback, atr, watchTime, cpn, playlistId).onSuccess { response ->
            emit(response)
        }.onFailure {
            Log.e("InitPlayback", "Error: ${it.message}")
            emit(Pair(0, 0f))
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSkipSegments(videoId: String): Flow<List<SkipSegments>?> = flow {
        YouTube.getSkipSegments(videoId).onSuccess {
            emit(it)
        }.onFailure {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun getFullMetadata(videoId: String): Flow<YouTubeInitialPage?> = flow {
        YouTube.getFullMetadata(videoId).onSuccess {
            emit(it)
        }.onFailure {
            Log.e("getFullMetadata", "Error: ${it.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<ArrayList<SetVideoIdEntity>?> = flow {
        YouTube.playlist(youtubePlaylistId).onSuccess {
            flow {
                runCatching {
                    var id = ""
                    if (!youtubePlaylistId.startsWith("VL")) {
                        id += "VL$youtubePlaylistId"
                    } else {
                        id += youtubePlaylistId
                    }
                    Log.d("Repository", "playlist id: $id")
                    YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.contents
                        if (data != null) {
                            Log.d("Data", "data: $data")
                            Log.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        var continueParam = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                        var count = 0
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            YouTube.customQuery(browseId = "", continuation = continueParam, setLogin = true).onSuccess { values ->
                                Log.d("Continue", "continue: $continueParam")
                                val dataMore: List<MusicShelfRenderer.Content>? = values.continuationContents?.musicPlaylistShelfContinuation?.contents
                                if (dataMore != null) {
                                    listContent.addAll(dataMore)
                                }
                                continueParam = values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(0)?.nextContinuationData?.continuation
                                count++
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                continueParam = null
                                count++
                            }
                        }
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(listContent).let { playlist ->
                            playlist.forEach { item ->
                                insertSetVideoId(item)
                            }
                            emit(playlist)
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        emit(null)
                    }
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    suspend fun createYouTubePlaylist(playlist: LocalPlaylistEntity): Flow<String?> = flow {
        runCatching {
            YouTube.createPlaylist(playlist.title, playlist.tracks).onSuccess {
                emit(it.playlistId)
            }.onFailure {
                it.printStackTrace()
                emit(null)
            }
        }
    }

    suspend fun editYouTubePlaylist(title: String, youtubePlaylistId: String): Flow<Int> = flow {
        runCatching {
            YouTube.editPlaylist(youtubePlaylistId, title).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }

    suspend fun removeYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) = flow {
        runCatching {
            getSetVideoId(videoId).collect { setVideoId ->
                if (setVideoId?.setVideoId != null) {
                    YouTube.removeItemYouTubePlaylist(youtubePlaylistId, videoId, setVideoId.setVideoId).onSuccess {
                        emit(it)
                    }.onFailure {
                        emit(0)
                    }
                }
                else {
                    emit(0)
                }
            }
        }
    }

    suspend fun addYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) = flow {
        runCatching {
            YouTube.addPlaylistItem(youtubePlaylistId, videoId).onSuccess {
                if (it.playlistEditResults.isNotEmpty()) {
                    for (playlistEditResult in it.playlistEditResults) {
                        insertSetVideoId(SetVideoIdEntity(playlistEditResult.playlistEditVideoAddedResultData.videoId, playlistEditResult.playlistEditVideoAddedResultData.setVideoId))
                    }
                    emit(it.status)
                }
                else {
                    emit("FAILED")
                }
            }.onFailure {
                emit("FAILED")
            }
        }
    }

    suspend fun loginToMusixMatch(email: String, password: String): Flow<MusixmatchCredential?> = flow {
        runCatching {
            if (YouTube.musixmatchUserToken != null && YouTube.musixmatchUserToken != "") {
                YouTube.postMusixmatchCredentials(email, password, YouTube.musixmatchUserToken!!).onSuccess { response ->
                    emit(response)
                }.onFailure {
                    it.printStackTrace()
                    emit(null)
                }
            }
            else {
                YouTube.getMusixmatchUserToken().onSuccess { usertoken ->
                    YouTube.musixmatchUserToken = usertoken.message.body.user_token
                    delay(2000)
                    YouTube.postMusixmatchCredentials(email, password, YouTube.musixmatchUserToken!!).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
                }
                    .onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(null)
                    }
            }
        }
    }

    suspend fun updateWatchTime(playbackTrackingVideostatsWatchtimeUrl: String, watchTimeList: ArrayList<Float>, cpn: String, playlistId: String?): Flow<Int> = flow {
        runCatching {
            YouTube.updateWatchTime(playbackTrackingVideostatsWatchtimeUrl, watchTimeList, cpn, playlistId).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun updateWatchTimeFull(watchTime: String, cpn: String, playlistId: String?): Flow<Int> = flow {
        runCatching {
            YouTube.updateWatchTimeFull(watchTime, cpn, playlistId).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }.flowOn(Dispatchers.IO)
}