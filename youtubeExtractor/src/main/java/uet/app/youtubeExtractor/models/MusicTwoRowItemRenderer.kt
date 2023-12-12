package uet.app.youtubeExtractor.models

import uet.app.youtubeExtractor.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import uet.app.youtubeExtractor.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import uet.app.youtubeExtractor.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import uet.app.youtubeExtractor.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import kotlinx.serialization.Serializable

/**
 * Two row: a big thumbnail, a title, and a subtitle
 * Used in [GridRenderer] and [MusicCarouselShelfRenderer]
 * Item type: song, video, album, playlist, artist
 */
@Serializable
data class MusicTwoRowItemRenderer(
    val title: Runs,
    val subtitle: Runs?,
    val subtitleBadges: List<Badges>?,
    val menu: Menu?,
    val thumbnailRenderer: ThumbnailRenderer,
    val navigationEndpoint: NavigationEndpoint,
    val thumbnailOverlay: MusicResponsiveListItemRenderer.Overlay?,
    val aspectRatio: String? = null,
) {
    val isSong: Boolean
        get() = navigationEndpoint.endpoint is WatchEndpoint && aspectRatio != "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
    val isPlaylist: Boolean
        get() = navigationEndpoint.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_PLAYLIST && aspectRatio != "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
    val isAlbum: Boolean
        get() = navigationEndpoint.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_AUDIOBOOK
    val isArtist: Boolean
        get() = navigationEndpoint.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ARTIST
    val isVideo: Boolean
        get() = navigationEndpoint.endpoint is WatchEndpoint && aspectRatio == "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
}
