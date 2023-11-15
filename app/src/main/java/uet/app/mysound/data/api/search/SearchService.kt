package uet.app.mysound.data.api.search

import uet.app.mysound.data.model.home.chart.Chart
import uet.app.mysound.data.model.browse.album.AlbumBrowse
import uet.app.mysound.data.model.browse.artist.ArtistBrowse
import uet.app.mysound.data.model.browse.playlist.PlaylistBrowse
import uet.app.mysound.data.model.explore.mood.Mood
import uet.app.mysound.data.model.home.homeItem
import uet.app.mysound.data.model.searchResult.albums.AlbumsResult
import uet.app.mysound.data.model.searchResult.artists.ArtistsResult
import uet.app.mysound.data.model.searchResult.playlists.PlaylistsResult
import uet.app.mysound.data.model.searchResult.songs.SongsResult
import uet.app.mysound.data.model.thumbnailUrl
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    //get_thumbnails
    @GET("thumbnails")
    suspend fun getThumbnails(@Query("songId") songId: String): Response<ArrayList<thumbnailUrl>>
    //search
    @GET("search")
    suspend fun searchAll(@Query("q") query: String): Response<ArrayList<Any>>
    @GET("search")
    suspend fun searchSongs(@Query("q") query: String, @Query("f") filter: String = "songs"): Response<ArrayList<SongsResult>>
    @GET("search")
    suspend fun searchArtists(@Query("q") query: String, @Query("f") filter: String = "artists"): Response<ArrayList<ArtistsResult>>
    @GET("search")
    suspend fun searchAlbums(@Query("q") query: String, @Query("f") filter: String = "albums"): Response<ArrayList<AlbumsResult>>
    @GET("search")
    suspend fun searchPlaylists(@Query("q") query: String, @Query("f") filter: String = "playlists"): Response<ArrayList<PlaylistsResult>>

    //suggest query
    @GET("query")
    suspend fun suggestQuery(@Query("q") query: String): Response<ArrayList<String>>

    //getHome
    @GET("home")
    suspend fun getHome(): Response<ArrayList<homeItem>>

    //exploreMood
    @GET("explore/mood")
    suspend fun exploreMood(): Response<Mood>
    //Chart
    @GET("explore/charts")
    suspend fun exploreChart(@Query("cc") regionCode: String): Response<Chart>

    //browse
    //Artist
    @GET("browse/artists")
    suspend fun browseArtist(@Query("channelId") channelId: String): Response<ArtistBrowse>
    //Artist Album
    @GET("browse/artists")
    suspend fun browseArtistAlbum(@Query("channelId") channelId: String, @Query("params") params: String)
    //Album
    @GET("browse/albums")
    suspend fun browseAlbum(@Query("browseId") browseId: String): Response<AlbumBrowse>
    //Playlist
    @GET("playlists")
    suspend fun browsePlaylist(@Query("id") id: String): Response<PlaylistBrowse>


}