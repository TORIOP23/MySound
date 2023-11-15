package uet.app.mysound.data.repository

import uet.app.mysound.data.model.home.chart.Chart
import uet.app.mysound.data.api.BaseApiResponse
import uet.app.mysound.data.api.search.RemoteDataSource
import uet.app.mysound.data.model.browse.album.AlbumBrowse
import uet.app.mysound.data.model.explore.mood.Mood
import uet.app.mysound.data.model.home.homeItem
import uet.app.mysound.data.model.searchResult.albums.AlbumsResult
import uet.app.mysound.data.model.searchResult.artists.ArtistsResult
import uet.app.mysound.data.model.searchResult.playlists.PlaylistsResult
import uet.app.mysound.data.model.searchResult.songs.SongsResult
import uet.app.mysound.data.model.browse.artist.ArtistBrowse
import uet.app.mysound.data.model.browse.playlist.PlaylistBrowse
import uet.app.mysound.data.model.thumbnailUrl
import uet.app.mysound.utils.Resource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ActivityRetainedScoped
class MainRepository @Inject constructor(private val remoteDataSource: RemoteDataSource): BaseApiResponse() {
    suspend fun getThumbnails(songId: String): Flow<Resource<ArrayList<thumbnailUrl>>> = flow { emit(safeApiCall { remoteDataSource.getThumbnails(songId) }) }.flowOn(Dispatchers.IO)
    //search
    suspend fun searchAll(query: String) = remoteDataSource.searchAll(query)
    suspend fun searchSongs(query: String): Flow<Resource<ArrayList<SongsResult>>> = flow<Resource<ArrayList<SongsResult>>> { emit(safeApiCall { remoteDataSource.searchSongs(query) }) }.flowOn(Dispatchers.IO)
    suspend fun searchArtists(query: String): Flow<Resource<ArrayList<ArtistsResult>>> = flow<Resource<ArrayList<ArtistsResult>>> { emit(safeApiCall { remoteDataSource.searchArtists(query) }) }.flowOn(Dispatchers.IO)
    suspend fun searchAlbums(query: String): Flow<Resource<ArrayList<AlbumsResult>>> = flow<Resource<ArrayList<AlbumsResult>>> { emit(safeApiCall { remoteDataSource.searchAlbums(query) }) }.flowOn(Dispatchers.IO)
    suspend fun searchPlaylists(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> = flow<Resource<ArrayList<PlaylistsResult>>> { emit(safeApiCall { remoteDataSource.searchPlaylists(query) }) }.flowOn(Dispatchers.IO)

    //suggest query
    suspend fun suggestQuery(query: String): Flow<Resource<ArrayList<String>>> = flow<Resource<ArrayList<String>>> { emit(safeApiCall { remoteDataSource.suggestQuery(query) }) }.flowOn(Dispatchers.IO)

    //getHome
    suspend fun getHome() : Flow<Resource<ArrayList<homeItem>>> =  flow<Resource<ArrayList<homeItem>>>{emit(safeApiCall { remoteDataSource.getHome() })  }.flowOn(Dispatchers.IO)

    //exploreMood
    suspend fun exploreMood(): Flow<Resource<Mood>> = flow<Resource<Mood>> { emit(safeApiCall { remoteDataSource.exploreMood() }) }.flowOn(Dispatchers.IO)

    //browse
    //artist
    suspend fun browseArtist(channelId: String): Flow<Resource<ArtistBrowse>> = flow<Resource<ArtistBrowse>> { emit(safeApiCall { remoteDataSource.browseArtist(channelId) }) }.flowOn(Dispatchers.IO)
    //album
    suspend fun browseAlbum(browseId: String): Flow<Resource<AlbumBrowse>> = flow<Resource<AlbumBrowse>> { emit(safeApiCall { remoteDataSource.browseAlbum(browseId) }) }.flowOn(Dispatchers.IO)
    //playlist
    suspend fun browsePlaylist(id: String): Flow<Resource<PlaylistBrowse>> = flow<Resource<PlaylistBrowse>> { emit(safeApiCall { remoteDataSource.browsePlaylist(id) }) }.flowOn(Dispatchers.IO)
    //chart
    suspend fun exploreChart(regionCode: String): Flow<Resource<Chart>> = flow<Resource<Chart>> { emit(safeApiCall { remoteDataSource.exploreChart(regionCode) }) }.flowOn(Dispatchers.IO)
}